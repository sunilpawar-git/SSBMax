// Rules-unit coverage for the `test_sessions` collection in ../firestore.rules.
//
// Why this file exists: the "stuck ACTIVE test_sessions" production incident (see
// CLAUDE.local.md / the commit that fixed PPDT+OIR) happened because the update rule was
// tightened to fix one bug and, in doing so, silently blocked every legitimate retake -- and
// nothing in CI would have caught either version of the mistake before it shipped. This suite
// pins the three legal transitions the rule's own comment documents, plus the illegal
// transitions a future "simplification" could easily reintroduce.
//
// Run via `firebase emulators:exec --only firestore "npm --prefix firestore-tests test"` --
// requires the emulator listening on 127.0.0.1:8080 (see ../firebase.json).

import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';

const HOUR_MS = 60 * 60 * 1000;

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-ssbmax-rules-test',
    firestore: {
      rules: readFileSync('../firestore.rules', 'utf8'),
      host: '127.0.0.1',
      port: 8080
    }
  });
});

after(async () => {
  await testEnv.cleanup();
});

function activeSession(overrides = {}) {
  const now = Date.now();
  return {
    userId: 'user-1',
    testId: 'tat_standard',
    testType: 'TAT',
    startTime: now,
    expiresAt: now + 2 * HOUR_MS,
    isActive: true,
    status: 'ACTIVE',
    ...overrides
  };
}

async function seed(docId, data) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('test_sessions').doc(docId).set(data);
  });
}

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

test('an ACTIVE session may transition to a terminal status (submit)', async () => {
  const docId = 'user-1_tat_standard';
  await seed(docId, activeSession());
  const db = testEnv.authenticatedContext('user-1').firestore();

  await assertSucceeds(
    db.collection('test_sessions').doc(docId).update({
      isActive: false,
      status: 'SUBMITTED'
    })
  );
});

test('a terminal session may start a fresh attempt (retake)', async () => {
  const docId = 'user-1_tat_standard';
  await seed(docId, activeSession({ isActive: false, status: 'SUBMITTED' }));
  const db = testEnv.authenticatedContext('user-1').firestore();
  const now = Date.now();

  await assertSucceeds(
    db.collection('test_sessions').doc(docId).update({
      isActive: true,
      status: 'ACTIVE',
      startTime: now,
      expiresAt: now + 2 * HOUR_MS
    })
  );
});

test('an expired ACTIVE session may be reclaimed by a fresh attempt', async () => {
  // Regression case for the actual incident: client never called abandonTestSession on some
  // exit path, so the doc is stuck ACTIVE past its own expiresAt.
  const docId = 'user-1_tat_standard';
  const now = Date.now();
  await seed(docId, activeSession({ expiresAt: now - HOUR_MS }));
  const db = testEnv.authenticatedContext('user-1').firestore();

  await assertSucceeds(
    db.collection('test_sessions').doc(docId).update({
      isActive: true,
      status: 'ACTIVE',
      startTime: now,
      expiresAt: now + 2 * HOUR_MS
    })
  );
});

test('a non-expired ACTIVE session cannot be reactivated by another attempt', async () => {
  // This is exactly the class of change that must stay denied: a live, in-progress session
  // being silently clobbered by a second "fresh attempt" write would let two tabs/devices race.
  const docId = 'user-1_tat_standard';
  await seed(docId, activeSession());
  const db = testEnv.authenticatedContext('user-1').firestore();
  const now = Date.now();

  await assertFails(
    db.collection('test_sessions').doc(docId).update({
      isActive: true,
      status: 'ACTIVE',
      startTime: now,
      expiresAt: now + 2 * HOUR_MS
    })
  );
});

test('another user cannot update someone else\'s session', async () => {
  const docId = 'user-1_tat_standard';
  await seed(docId, activeSession());
  const db = testEnv.authenticatedContext('user-2').firestore();

  await assertFails(
    db.collection('test_sessions').doc(docId).update({
      isActive: false,
      status: 'SUBMITTED'
    })
  );
});

test('userId, testId, and testType are immutable on update', async () => {
  const docId = 'user-1_tat_standard';
  await seed(docId, activeSession());
  const db = testEnv.authenticatedContext('user-1').firestore();

  await assertFails(
    db.collection('test_sessions').doc(docId).update({
      isActive: false,
      status: 'SUBMITTED',
      testType: 'WAT'
    })
  );
});

test('an unauthenticated client cannot update a session', async () => {
  const docId = 'user-1_tat_standard';
  await seed(docId, activeSession());
  const db = testEnv.unauthenticatedContext().firestore();

  await assertFails(
    db.collection('test_sessions').doc(docId).update({
      isActive: false,
      status: 'SUBMITTED'
    })
  );
});

test('a client cannot create a session with someone else\'s userId', async () => {
  const db = testEnv.authenticatedContext('user-1').firestore();
  const now = Date.now();

  await assertFails(
    db.collection('test_sessions').doc('user-1_tat_standard').set({
      userId: 'user-2',
      testId: 'tat_standard',
      testType: 'TAT',
      startTime: now,
      expiresAt: now + 2 * HOUR_MS,
      isActive: true,
      status: 'ACTIVE'
    })
  );
});

test('a client cannot delete a session', async () => {
  const docId = 'user-1_tat_standard';
  await seed(docId, activeSession());
  const db = testEnv.authenticatedContext('user-1').firestore();

  await assertFails(db.collection('test_sessions').doc(docId).delete());
});
