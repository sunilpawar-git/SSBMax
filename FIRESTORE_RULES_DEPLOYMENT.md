# Firestore Security Rules Deployment Guide

## Problem
User Profile saving is failing with error: **"PERMISSION_DENIED: Missing or insufficient permissions"**

## Root Cause
The Firestore security rules don't allow access to the `users/{userId}/data/{document}` subcollection where user profiles are stored.

## Solution
Updated Firestore security rules to allow users to read/write their own data subcollection.

## Changes Made

### Added to firestore.rules (lines 64-68):
```javascript
// User data subcollection (for profile, settings, etc.)
match /data/{document} {
  // Users can read/write their own data subcollection
  allow read, write: if isOwner(userId);
}
```

This nested rule allows authenticated users to access documents at:
- `users/{userId}/data/profile` (User Profile)
- `users/{userId}/data/settings` (Future use)
- `users/{userId}/data/*` (Any other data documents)

## Data Structure
```
Firestore
└── users
    └── {userId}
        ├── (main user document - role, email, etc.)
        └── data (subcollection)
            ├── profile (UserProfile data)
            └── settings (Future)
```

## Deployment Steps

### Option 1: Firebase Console (Recommended for Quick Fix)

1. **Open Firebase Console**
   - Go to: https://console.firebase.google.com/
   - Select your SSBMax project

2. **Navigate to Firestore Rules**
   - Click "Firestore Database" in left menu
   - Click "Rules" tab

3. **Update Rules**
   - Find this section (around line 50):
   ```javascript
   match /users/{userId} {
     // Users can read their own data
     allow read: if isOwner(userId);
     
     // Users can create their own profile
     allow create: if isAuthenticated() && request.auth.uid == userId;
     
     // Users can update their own profile (except role)
     allow update: if isOwner(userId) && 
                      !request.resource.data.diff(resource.data).affectedKeys().hasAny(['role']);
     
     // No one can delete user profiles
     allow delete: if false;
   }
   ```

4. **Add this nested match BEFORE the closing brace:**
   ```javascript
   match /users/{userId} {
     // ... existing rules ...
     
     // ADD THIS SECTION:
     // User data subcollection (for profile, settings, etc.)
     match /data/{document} {
       // Users can read/write their own data subcollection
       allow read, write: if isOwner(userId);
     }
   }
   ```

5. **Publish Rules**
   - Click "Publish" button
   - Wait for confirmation (usually takes 1-2 seconds)

6. **Test in App**
   - Force close SSBMax app
   - Reopen and try to save profile
   - Should work without permission error

### Option 2: Firebase CLI Deployment

1. **Install Firebase CLI** (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. **Login to Firebase**:
   ```bash
   firebase login
   ```

3. **Deploy Rules**:
   ```bash
   cd /Users/sunil/Downloads/SSBMax
   firebase deploy --only firestore:rules
   ```

4. **Verify Deployment**:
   ```bash
   firebase firestore:rules get
   ```

### Option 3: Use the Updated File

The complete updated rules file is already created:
- `/Users/sunil/Downloads/SSBMax/firestore.rules`

You can copy this entire file content to Firebase Console or deploy via CLI.

## Verification

After deploying the rules, verify they work:

1. **Open App** → Login
2. **Navigate to Profile** → Edit Profile
3. **Fill in details**: Name, Age, Gender, Entry Type
4. **Tap Save**
5. **Should show**: Success message (no permission error)
6. **Open Drawer** → Should display profile information

## Security Implications

### What's Allowed ✅
- Users can read their own profile data
- Users can create/update their own profile data
- Each user is isolated to their own data (userId must match auth.uid)

### What's Blocked ❌
- Users cannot read other users' profiles
- Users cannot modify other users' data
- Unauthenticated users have no access
- Cannot delete profile data

### Security Check
The rule `if isOwner(userId)` ensures:
```javascript
function isOwner(userId) {
  return isAuthenticated() && request.auth.uid == userId;
}
```
- User must be authenticated
- User's auth UID must match the document's userId path parameter

## Complete Updated Rules Section

```javascript
match /users/{userId} {
  // Users can read their own data
  allow read: if isOwner(userId);
  
  // Users can create their own profile
  allow create: if isAuthenticated() && request.auth.uid == userId;
  
  // Users can update their own profile (except role)
  allow update: if isOwner(userId) && 
                   !request.resource.data.diff(resource.data).affectedKeys().hasAny(['role']);
  
  // No one can delete user profiles
  allow delete: if false;
  
  // User data subcollection (for profile, settings, etc.)
  match /data/{document} {
    // Users can read/write their own data subcollection
    allow read, write: if isOwner(userId);
  }
}
```

## Testing Checklist

- [ ] Rules deployed successfully
- [ ] App can read existing profile (if any)
- [ ] App can create new profile (first time users)
- [ ] App can update existing profile
- [ ] App shows profile in drawer header
- [ ] No permission errors in logs
- [ ] Other users cannot access your profile (security test)

## Rollback Plan

If there are any issues with the new rules:

1. Go to Firebase Console → Firestore Database → Rules
2. Click "History" tab
3. Select previous version
4. Click "Restore"

## Files Modified

1. ✅ `firestore.rules` - Created with updated rules
2. ✅ `firestore.rules.updated` - Updated with new subcollection rules

## Next Steps

1. **Deploy the rules** using one of the methods above
2. **Test profile creation/editing** in the app
3. **Verify drawer** shows profile correctly
4. Continue with Phase 4 development

---

**Status**: Rules Updated ✅  
**Deployment**: Required (use Firebase Console or CLI)  
**Testing**: After deployment  
**Priority**: HIGH (app cannot save profiles without this)

