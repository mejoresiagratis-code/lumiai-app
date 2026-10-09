'use strict';
const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { initializeApp, deleteApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc } = require('firebase/firestore');
const { createDeletionService, authorizedUid, deletionStatus } = require('../deletion');
const { randomUUID } = require('node:crypto');
const projectId = 'demo-lumiai-deletion';
const receipt = () => randomUUID() + randomUUID();
let app, db, auth, env;
let clock;
before(async () => {
  assert.ok(process.env.FIRESTORE_EMULATOR_HOST, 'Only emulator data may be used');
  assert.ok(process.env.FIREBASE_AUTH_EMULATOR_HOST, 'Only emulator Auth may be used');
  app = initializeApp({ projectId }); db = getFirestore(app); auth = getAuth(app);
  env = await initializeTestEnvironment({ projectId, firestore: { rules: fs.readFileSync('../firestore.rules', 'utf8') } });
});
after(async () => { await env?.cleanup(); if (app) await deleteApp(app); });
beforeEach(async () => { await env.clearFirestore(); clock = Date.now(); });
const service = (overrides = {}) => createDeletionService({ db, auth, now: () => clock, ...overrides });
async function seed(uid) {
  await auth.createUser({ uid });
  await db.doc(`users/${uid}`).set({ email: 'fixture@example.test' });
  await db.doc(`users/${uid}/private/nested`).set({ value: 'fixture' });
}

test('caller cannot select another UID or use anonymous credentials', () => {
  assert.throws(() => authorizedUid({ data: {} }), /Sign-in/);
  assert.throws(() => authorizedUid({ auth: { uid: 'a', token: { firebase: { sign_in_provider: 'anonymous' } } }, data: { expectedUid: 'a' } }), /Sign-in/);
  assert.throws(() => authorizedUid({ auth: { uid: 'a', token: {} }, data: { expectedUid: 'b' } }), /Account changed/);
  assert.equal(authorizedUid({ auth: { uid: 'a', token: {} }, data: { expectedUid: 'a' } }), 'a');
});

test('recent login required before creating a job or deleting data', async () => {
  const uid = 'stale-login'; await seed(uid);
  await assert.rejects(service().request(uid, clock / 1000 - 600, receipt()), { code: 'recent-login-required' });
  assert.equal((await db.doc(`accountDeletions/${uid}`).get()).exists, false);
  assert.equal((await db.doc(`users/${uid}`).get()).exists, true);
  assert.equal((await auth.getUser(uid)).uid, uid);
});

test('deletes nested registry and Auth then confirms; repeat is idempotent', async () => {
  const uid = 'complete'; const proof = receipt(); await seed(uid);
  assert.deepEqual(await service().request(uid, clock / 1000, proof), { status: 'completed' });
  assert.equal((await db.doc(`users/${uid}/private/nested`).get()).exists, false);
  assert.equal((await db.doc(`users/${uid}`).get()).exists, false);
  await assert.rejects(auth.getUser(uid), { code: 'auth/user-not-found' });
  assert.deepEqual(await service().request(uid, 0, proof), { status: 'completed' });
  assert.equal(await deletionStatus(db, uid, proof), 'completed');
  assert.equal(await deletionStatus(db, uid, receipt()), 'unknown');
});

test('Firestore failure keeps Auth and durable pending job for retry', async () => {
  const uid = 'firestore-failure'; const proof = receipt(); await seed(uid);
  const failingDb = { collection: (...args) => db.collection(...args), runTransaction: (...args) => db.runTransaction(...args), recursiveDelete: async () => { throw new Error('offline'); } };
  await assert.rejects(service({ db: failingDb }).request(uid, clock / 1000, proof), /offline/);
  assert.equal((await auth.getUser(uid)).uid, uid);
  assert.equal(await deletionStatus(db, uid, proof), 'pending');
  await service().retryPending();
  assert.equal(await deletionStatus(db, uid, proof), 'completed');
});

test('Auth failure retries without recreating already removed registry', async () => {
  const uid = 'auth-failure'; const proof = receipt(); await seed(uid);
  await assert.rejects(service({ auth: { deleteUser: async () => { throw new Error('unavailable'); } } }).request(uid, clock / 1000, proof), /unavailable/);
  assert.equal((await db.doc(`users/${uid}`).get()).exists, false);
  assert.equal((await auth.getUser(uid)).uid, uid);
  await service().retryPending();
  assert.equal(await deletionStatus(db, uid, proof), 'completed');
});

test('pending and completed tombstones deny stale owner writes and cannot be forged', async () => {
  const uid = 'rules-owner';
  const client = env.authenticatedContext(uid).firestore();
  await assertSucceeds(setDoc(doc(client, `users/${uid}`), { fullName: 'fixture' }));
  await assertFails(setDoc(doc(client, `accountDeletions/${uid}`), { state: 'completed' }));
  await db.doc(`accountDeletions/${uid}`).set({ state: 'pending' });
  await assertFails(setDoc(doc(client, `users/${uid}`), { fullName: 'late write' }));
  await db.doc(`users/${uid}`).delete();
  await db.doc(`accountDeletions/${uid}`).update({ state: 'completed' });
  await assertFails(setDoc(doc(client, `users/${uid}`), { fullName: 'recreated' }));
  await assertSucceeds(getDoc(doc(client, `accountDeletions/${uid}`)));
  await assertFails(getDoc(doc(env.authenticatedContext('other').firestore(), `accountDeletions/${uid}`)));
  await assertFails(setDoc(doc(env.unauthenticatedContext().firestore(), `users/${uid}`), {}));
});

test('completed tombstone expires after seven days; pending job is retained', async () => {
  const uid = 'retention'; const proof = receipt(); await seed(uid);
  await service().request(uid, clock / 1000, proof);
  clock += 8 * 86400_000;
  await db.doc('accountDeletions/still-pending').set({ state: 'pending', nextAttemptAt: new Date(0) });
  const failing = service({ auth: { deleteUser: async () => { throw new Error('unavailable'); } } });
  await failing.retryPending();
  assert.equal((await db.doc(`accountDeletions/${uid}`).get()).exists, false);
  assert.equal((await db.doc('accountDeletions/still-pending').get()).get('state'), 'pending');
});

test('lost completion response resumes when Auth is already gone', async () => {
  const uid = 'lost-response'; const proof = receipt(); await seed(uid);
  await assert.rejects(service({ auth: { deleteUser: async id => { await auth.deleteUser(id); throw new Error('response lost'); } } }).request(uid, clock / 1000, proof), /response lost/);
  await service().retryPending();
  assert.equal(await deletionStatus(db, uid, proof), 'completed');
});
