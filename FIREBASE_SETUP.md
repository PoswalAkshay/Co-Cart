# Firebase household security setup

Co-Cart household creation, joining, and leaving now use callable Cloud Functions. Firestore rules authorize household reads and grocery changes only when the signed-in user's membership document exists.

## Before deployment

1. Confirm Firebase Authentication email/password sign-in and Cloud Firestore are enabled for project `co-cart-8b546`.
2. Install the Firebase CLI and sign in with an account that can deploy to this Firebase project.
3. From the project directory, install the function dependencies:

   ```powershell
   cd functions
   npm install
   cd ..
   ```

4. Deploy the rules and functions:

   ```powershell
   firebase deploy --only firestore:rules,functions
   ```

Cloud Functions deployment may require the Firebase project to use the Blaze billing plan. Review the project’s Firebase billing settings before deployment.

## Data changes

- Membership is stored at `households/{householdId}/members/{uid}`.
- Invite lookup records are stored at `householdInvites/{code}` and are accessed by trusted Cloud Functions, not Android clients.
- `createHousehold`, `joinHousehold`, and `leaveHousehold` update household membership and the user's `householdId` in Firestore transactions.
- Invite codes remain six characters to match the app flow. The server generates codes and limits each signed-in account to ten join attempts per hour.
- Client grocery operations remain direct Firestore operations and are restricted by membership rules.

## Existing Firestore data

Households created before this change do not have member documents or invite lookup records. Their current members must be migrated into `households/{householdId}/members/{uid}` and each code must be added to `householdInvites/{code}` before the new rules can be enabled for those households. Do not deploy the rules against existing shared data until that migration is complete.

Use the Firebase Emulator Suite or a non-production Firebase project to validate the rules and callable functions before deploying them to a project with real household data.
