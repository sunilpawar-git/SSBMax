# 🚨 QUICK FIX: Profile Permission Error

## The Error
```
PERMISSION_DENIED: Missing or insufficient permissions.
```

## Quick Solution (2 minutes)

### Step 1: Open Firebase Console
1. Go to https://console.firebase.google.com/
2. Click on your **SSBMax** project

### Step 2: Go to Firestore Rules
1. Click **"Firestore Database"** in the left sidebar
2. Click the **"Rules"** tab at the top

### Step 3: Find This Code (around line 50-63)
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

### Step 4: Add These Lines BEFORE the closing `}`
Add this code just before the `}` that closes the `match /users/{userId}` block:

```javascript
  // User data subcollection (for profile, settings, etc.)
  match /data/{document} {
    // Users can read/write their own data subcollection
    allow read, write: if isOwner(userId);
  }
```

### Step 5: The Complete Section Should Look Like This:
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

### Step 6: Publish
1. Click the **"Publish"** button (top right)
2. Wait for "Rules successfully published" message (~2 seconds)

### Step 7: Test
1. **Force close** the SSBMax app completely
2. **Reopen** the app
3. **Edit Profile** → Fill in details → **Save**
4. ✅ Should save without permission error!

## Alternative: Copy-Paste Entire File

If you prefer, you can replace ALL the rules:

1. Open this file: `/Users/sunil/Downloads/SSBMax/firestore.rules`
2. Copy its entire contents
3. In Firebase Console → Firestore Rules → Paste over everything
4. Click "Publish"

## Why This Works

- Your app stores profiles at: `users/{userId}/data/profile`
- Old rules only allowed access to: `users/{userId}` (main document)
- New nested rule allows access to: `users/{userId}/data/*` (subcollection)
- Security is maintained: each user can only access their own data

## Verification

After publishing, the profile save button should work and you should see your profile info in the navigation drawer!

---

**Time Required**: 2 minutes  
**Risk**: None (only adds permissions for user's own data)  
**Impact**: Fixes profile creation/editing completely

