'use strict';
const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');
const { onCall, onRequest, HttpsError } = require('firebase-functions/v2/https');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const logger = require('firebase-functions/logger');
const { createDeletionService, authorizedUid, deletionStatus } = require('./deletion');
initializeApp();
const deletion = createDeletionService({ db: getFirestore(), auth: getAuth() });

exports.deleteMyAccount = onCall({ region: 'europe-west1', enforceAppCheck: true, timeoutSeconds: 120, maxInstances: 5 }, async request => {
  try {
    const uid = authorizedUid(request);
    return await deletion.request(uid, request.auth.token.auth_time, request.data.receipt);
  } catch (error) {
    if (error.code === 'recent-login-required') {
      throw new HttpsError('failed-precondition', 'Sign in again', { reason: 'recent-login-required' });
    }
    if (['unauthenticated', 'failed-precondition'].includes(error.code)) {
      throw new HttpsError(error.code, error.message);
    }
    // No emails, UIDs or raw provider errors in application logs/responses.
    logger.error('Account deletion pending; retry scheduled');
    throw new HttpsError('unavailable', 'Deletion pending; retry later');
  }
});

exports.retryAccountDeletions = onSchedule({ region: 'europe-west1', schedule: 'every 5 minutes', timeoutSeconds: 540, maxInstances: 1 }, async () => {
  const result = await deletion.retryPending();
  if (result.failed) logger.error('Account deletion retries failed', result);
});


exports.accountDeletionStatus = onRequest({ region: 'europe-west1', timeoutSeconds: 30, maxInstances: 5 }, async (req, res) => {
  res.set('Cache-Control', 'no-store');
  if (req.method !== 'POST') { res.status(405).end(); return; }
  try {
    const { getAppCheck } = require('firebase-admin/app-check');
    await getAppCheck().verifyToken(req.header('X-Firebase-AppCheck') || '');
    const status = await deletionStatus(getFirestore(), req.body?.uid, req.body?.receipt);
    res.json({ status });
  } catch (_) { res.status(403).json({ status: 'unknown' }); }
});
