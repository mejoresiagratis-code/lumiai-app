'use strict';

// Every operation is idempotent. The durable job is created before any deletion.
// Writes are denied by Firestore rules as soon as the job exists.
function createDeletionService({ db, auth, now = () => Date.now() }) {
  const jobs = db.collection('accountDeletions');

  async function request(uid, authTime, receipt) {
    const hashedReceipt = receiptHash(receipt);
    const ref = jobs.doc(uid);
    await db.runTransaction(async tx => {
      const job = await tx.get(ref);
      if (job.exists) return; // An already-authorized request can always be resumed.
      const age = Math.floor(now() / 1000) - authTime;
      if (!Number.isFinite(authTime) || age < -60 || age > 300) {
        const error = new Error('Recent sign-in required');
        error.code = 'recent-login-required';
        throw error;
      }
      tx.create(ref, { receiptHash: hashedReceipt, state: 'pending', createdAt: new Date(now()), nextAttemptAt: new Date(now()) });
    });
    return process(uid);
  }

  async function process(uid) {
    const ref = jobs.doc(uid);
    const snapshot = await ref.get();
    if (!snapshot.exists) throw new Error('Deletion was not authorized');
    if (snapshot.get('state') === 'completed') return { status: 'completed' };
    // Rotate failing jobs to the back of the retry queue, without changing state.
    await ref.update({ nextAttemptAt: new Date(now() + 60_000) });
    await db.recursiveDelete(db.collection('users').doc(uid));
    try {
      await auth.deleteUser(uid);
    } catch (error) {
      if (error.code !== 'auth/user-not-found') throw error;
    }
    // Keep only the minimum tombstone for seven days, beyond existing ID token validity.
    await ref.update({ state: 'completed', completedAt: new Date(now()), purgeAfter: new Date(now() + 7 * 86400_000) });
    return { status: 'completed' };
  }

  async function retryPending() {
    const pending = await jobs.where('state', '==', 'pending').orderBy('nextAttemptAt').limit(50).get();
    let failed = 0;
    for (const job of pending.docs) {
      try { await process(job.id); } catch (_) { failed++; }
    }
    const expired = await jobs.where('purgeAfter', '<=', new Date(now())).limit(100).get();
    for (const job of expired.docs) {
      if (job.get('state') === 'completed') await job.ref.delete();
    }
    return { attempted: pending.size, failed, purged: expired.size };
  }
  return { request, process, retryPending };
}

function authorizedUid(request) {
  const uid = request.auth?.uid;
  if (!uid || request.auth.token.firebase?.sign_in_provider === 'anonymous') {
    const error = new Error('Sign-in required'); error.code = 'unauthenticated'; throw error;
  }
  if (request.data?.expectedUid !== uid) {
    const error = new Error('Account changed'); error.code = 'failed-precondition'; throw error;
  }
  return uid;
}
module.exports = { createDeletionService, authorizedUid };

// A random receipt permits checking completion after Auth has already been deleted.
// It grants no right to create a job, delete another account, or read profile data.
const { createHash, timingSafeEqual } = require('node:crypto');
function receiptHash(receipt) {
  if (typeof receipt !== 'string' || !/^[a-f0-9-]{72}$/.test(receipt)) {
    const error = new Error('Invalid deletion receipt'); error.code = 'failed-precondition'; throw error;
  }
  return createHash('sha256').update(receipt).digest('hex');
}
async function deletionStatus(db, uid, receipt) {
  if (typeof uid !== 'string' || uid.length < 1 || uid.length > 128 || uid.includes('/')) return 'unknown';
  const supplied = receiptHash(receipt);
  const job = await db.collection('accountDeletions').doc(uid).get();
  const stored = job.get('receiptHash');
  if (typeof stored !== 'string' || stored.length !== 64 ||
      !timingSafeEqual(Buffer.from(stored), Buffer.from(supplied))) return 'unknown';
  return job.get('state') === 'completed' ? 'completed' : 'pending';
}
module.exports.receiptHash = receiptHash;
module.exports.deletionStatus = deletionStatus;
