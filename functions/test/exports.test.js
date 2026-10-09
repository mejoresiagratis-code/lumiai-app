'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
// Load the actual deployed entry point, not only its domain helpers.
test('all three deployed handlers load with the pinned Firebase SDKs', () => {
  const handlers = require('../index');
  for (const name of ['deleteMyAccount', 'retryAccountDeletions', 'accountDeletionStatus']) {
    assert.equal(typeof handlers[name], 'function', name);
    assert.ok(handlers[name].__endpoint, `${name} must be a deployable Cloud Function`);
  }
});
