const crypto = require('node:crypto');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { HttpsError, onCall } = require('firebase-functions/v2/https');

initializeApp();
const db = getFirestore();
const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const CODE_LENGTH = 6;

function requireUser(request) {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Sign in before managing a household.');
  }
  return request.auth.uid;
}

function makeInviteCode() {
  const bytes = crypto.randomBytes(CODE_LENGTH);
  return Array.from(bytes, (byte) => CODE_ALPHABET[byte % CODE_ALPHABET.length]).join('');
}

async function consumeJoinAttempt(uid) {
  const attemptRef = db.collection('inviteJoinAttempts').doc(uid);
  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(attemptRef);
    const now = Date.now();
    const windowStartedAt = snapshot.get('windowStartedAt') || now;
    const attempts = now - windowStartedAt > 60 * 60 * 1000
      ? 0
      : (snapshot.get('attempts') || 0);
    if (attempts >= 10) {
      throw new HttpsError('resource-exhausted', 'Too many invite attempts. Try again in an hour.');
    }
    transaction.set(attemptRef, {
      windowStartedAt: attempts === 0 ? now : windowStartedAt,
      attempts: attempts + 1,
    });
  });
}

async function createUniqueInviteCode(transaction) {
  // Read and reserve a code inside the same transaction as household creation.
  for (let attempt = 0; attempt < 8; attempt += 1) {
    const code = makeInviteCode();
    const ref = db.collection('householdInvites').doc(code);
    const existing = await transaction.get(ref);
    if (!existing.exists) return { code, ref };
  }
  throw new HttpsError('resource-exhausted', 'Could not generate an invite code. Try again.');
}

exports.createHousehold = onCall(async (request) => {
  const uid = requireUser(request);
  const name = typeof request.data?.name === 'string' ? request.data.name.trim() : '';
  if (!name || name.length > 60) {
    throw new HttpsError('invalid-argument', 'Household name must be between 1 and 60 characters.');
  }

  const householdRef = db.collection('households').doc();
  const userRef = db.collection('users').doc(uid);
  const displayName = request.auth.token.name || 'Roommate';
  const createdAt = Date.now();

  const household = await db.runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    if (!userSnapshot.exists) {
      throw new HttpsError('failed-precondition', 'Your profile is missing. Sign out and sign in again.');
    }
    const currentHouseholdId = userSnapshot.get('householdId') || '';
    if (currentHouseholdId) {
      throw new HttpsError('failed-precondition', 'Leave your current household before creating another.');
    }

    const { code, ref: inviteRef } = await createUniqueInviteCode(transaction);
    const data = {
      householdId: householdRef.id,
      name,
      inviteCode: code,
      createdBy: uid,
      createdAt,
    };
    transaction.create(householdRef, data);
    transaction.create(inviteRef, { householdId: householdRef.id, createdAt });
    transaction.create(householdRef.collection('members').doc(uid), {
      uid,
      displayName,
      role: 'owner',
      joinedAt: createdAt,
    });
    transaction.update(userRef, { householdId: householdRef.id });
    return data;
  });

  return household;
});

exports.joinHousehold = onCall(async (request) => {
  const uid = requireUser(request);
  const code = typeof request.data?.inviteCode === 'string'
    ? request.data.inviteCode.trim().toUpperCase()
    : '';
  if (!/^[A-HJ-NP-Z2-9]{6}$/.test(code)) {
    throw new HttpsError('invalid-argument', 'Enter a valid 6-character invite code.');
  }

  await consumeJoinAttempt(uid);

  const inviteRef = db.collection('householdInvites').doc(code);
  const userRef = db.collection('users').doc(uid);
  const displayName = request.auth.token.name || 'Roommate';

  const household = await db.runTransaction(async (transaction) => {
    const inviteSnapshot = await transaction.get(inviteRef);
    if (!inviteSnapshot.exists) {
      throw new HttpsError('not-found', 'No household found for that invite code.');
    }
    const householdId = inviteSnapshot.get('householdId');
    const householdRef = db.collection('households').doc(householdId);
    const memberRef = householdRef.collection('members').doc(uid);
    const [householdSnapshot, memberSnapshot, userSnapshot] = await Promise.all([
      transaction.get(householdRef),
      transaction.get(memberRef),
      transaction.get(userRef),
    ]);

    if (!householdSnapshot.exists || !userSnapshot.exists) {
      throw new HttpsError('not-found', 'Household or user profile was not found.');
    }
    const currentHouseholdId = userSnapshot.get('householdId') || '';
    if (currentHouseholdId && currentHouseholdId !== householdId) {
      throw new HttpsError('failed-precondition', 'Leave your current household before joining another.');
    }
    if (!memberSnapshot.exists) {
      transaction.create(memberRef, {
        uid,
        displayName,
        role: 'member',
        joinedAt: Date.now(),
      });
    }
    transaction.update(userRef, { householdId });
    return householdSnapshot.data();
  });

  return household;
});

exports.leaveHousehold = onCall(async (request) => {
  const uid = requireUser(request);
  const userRef = db.collection('users').doc(uid);

  await db.runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    if (!userSnapshot.exists) {
      throw new HttpsError('not-found', 'User profile was not found.');
    }
    const householdId = userSnapshot.get('householdId') || '';
    if (!householdId) return;
    const householdRef = db.collection('households').doc(householdId);
    const membersRef = householdRef.collection('members');
    const memberRef = membersRef.doc(uid);
    const memberSnapshot = await transaction.get(memberRef);
    let replacementOwner = null;
    if (memberSnapshot.exists && memberSnapshot.get('role') === 'owner') {
      const memberQuery = membersRef.orderBy('joinedAt', 'asc').limit(2);
      const membersSnapshot = await transaction.get(memberQuery);
      replacementOwner = membersSnapshot.docs.find((doc) => doc.id !== uid) || null;
    }
    transaction.delete(memberRef);
    transaction.update(userRef, { householdId: '' });
    if (replacementOwner) {
      transaction.update(replacementOwner.ref, { role: 'owner' });
      transaction.update(householdRef, { createdBy: replacementOwner.id });
    }
  });

  return { success: true };
});
