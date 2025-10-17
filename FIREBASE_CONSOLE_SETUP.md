# Firebase Console Setup - Final Steps

## ✅ What's Done
- ✅ Firebase project created
- ✅ Android app registered
- ✅ google-services.json downloaded and placed
- ✅ Firebase SDK configured in project
- ✅ Project builds successfully

---

## 🔐 Step 1: Add SHA-1 Certificate to Firebase

### Your SHA-1 Certificate:
```
BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

### Instructions:

1. **Go to Firebase Console**: https://console.firebase.google.com
2. **Select your project** (SSBMax or whatever you named it)
3. Click the **⚙️ gear icon** (top left) → **Project settings**
4. Scroll down to **"Your apps"** section
5. Find your Android app card (com.ssbmax)
6. Scroll to **"SHA certificate fingerprints"** section
7. Click **"Add fingerprint"** button
8. **Paste**: `BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05`
9. Click **"Save"**

✅ SHA-1 added! This enables Google Sign-In to work.

---

## 🔥 Step 2: Enable Firebase Authentication

1. **In Firebase Console** → Left sidebar → Click **"Authentication"**
2. Click **"Get started"** button
3. Go to **"Sign-in method"** tab
4. Click on **"Google"** in the providers list
5. Toggle **"Enable"** switch to ON
6. **Project support email**: Enter your email (required)
7. Click **"Save"**

✅ Google Sign-In is now enabled!

---

## 📊 Step 3: Create Cloud Firestore Database

1. **In Firebase Console** → Left sidebar → Click **"Firestore Database"**
2. Click **"Create database"** button
3. **Select location**: Choose the closest region
   - For India: `asia-south1` (Mumbai)
   - For US: `us-central1`
   - For Europe: `europe-west1`
4. **Security rules**: Select **"Start in production mode"**
   - Click **"Next"**
5. Click **"Enable"**
6. Wait 30-60 seconds for database creation

✅ Firestore database created!

---

## 🔒 Step 4: Set Up Security Rules

After Firestore is created:

1. Go to **"Rules"** tab in Firestore
2. **Replace** the existing rules with these:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns the document
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // Users collection - users can only read/write their own profile
    match /users/{userId} {
      allow read: if isOwner(userId);
      allow write: if isOwner(userId);
    }
    
    // Test submissions - users can read/write their own submissions
    // Instructors can read submissions for their batches
    match /submissions/{submissionId} {
      allow read: if isAuthenticated() && 
                     (resource.data.userId == request.auth.uid || 
                      resource.data.instructorId == request.auth.uid);
      allow create: if isAuthenticated() && 
                       request.resource.data.userId == request.auth.uid;
      allow update: if isAuthenticated() && 
                       (resource.data.userId == request.auth.uid || 
                        resource.data.instructorId == request.auth.uid);
      allow delete: if isOwner(resource.data.userId);
    }
    
    // Batches - students can read their batches, instructors can create/update
    match /batches/{batchId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && 
                       request.resource.data.instructorId == request.auth.uid;
      allow update: if isAuthenticated() && 
                       resource.data.instructorId == request.auth.uid;
      allow delete: if isAuthenticated() && 
                       resource.data.instructorId == request.auth.uid;
    }
    
    // Test configurations - public read, no write (manage via Firebase Console)
    match /test_configs/{testId} {
      allow read: if true;  // Anyone can read test configurations
      allow write: if false;  // Only admin/console can write
    }
    
    // Analytics and progress - users can only see their own
    match /user_progress/{userId} {
      allow read, write: if isOwner(userId);
    }
    
    // Achievements - users can read their own
    match /achievements/{achievementId} {
      allow read: if isAuthenticated() && 
                     resource.data.userId == request.auth.uid;
      allow write: if false;  // System/Cloud Function only
    }
  }
}
```

3. Click **"Publish"** button

✅ Security rules are now active!

**Why these rules?**
- Users can only access their own data
- Submissions are protected (only student + their instructor can see)
- Test configs are read-only (you manage them via Console)
- Prevents unauthorized access and data leaks

---

## 📦 Step 5: Enable Firebase Storage (Optional - for TAT/PPDT images)

This is optional but recommended for storing test images:

1. **In Firebase Console** → Left sidebar → Click **"Storage"**
2. Click **"Get started"** button
3. **Security rules**: Choose **"Start in production mode"**
   - Click **"Next"**
4. **Storage location**: Choose **same region as Firestore**
5. Click **"Done"**

### Set Storage Security Rules:

Go to **"Rules"** tab and paste:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Allow authenticated users to upload/download their own test images
    match /user_uploads/{userId}/{allPaths=**} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId
                   && request.resource.size < 5 * 1024 * 1024; // Max 5MB
    }
    
    // Public test images (TAT/PPDT pictures) - read only
    match /test_images/{allPaths=**} {
      allow read: if true;  // Anyone can read
      allow write: if false;  // Only admin can upload
    }
    
    // Submission attachments - only owner and instructor can access
    match /submissions/{submissionId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

Click **"Publish"**

✅ Storage is now ready!

---

## 📊 Step 6: Enable Offline Persistence (Optional but Recommended)

Firestore has built-in offline support! We'll enable it in the code later, but you can verify it's available:

1. Go to **Firestore Database** → **Settings** tab
2. Look for **"Offline persistence"** - should show as available
3. ✅ Already enabled by default for Android!

---

## ✅ Setup Complete Checklist

Check off each item:

- [ ] SHA-1 certificate added to Firebase Console
- [ ] Authentication enabled (Google Sign-In)
- [ ] Firestore database created
- [ ] Firestore security rules published
- [ ] Storage enabled (optional)
- [ ] Storage security rules published (optional)

---

## 🎯 What's Next?

After completing these steps, you're ready to:

1. **Implement Firebase Auth Service** - Handle Google Sign-In
2. **Create Firestore Repository** - Save/load test submissions
3. **Add offline support** - Work without internet
4. **Implement real-time sync** - Listen for grading updates

---

## 🔍 Verify Setup

You can test if everything works by:

### Test 1: Check google-services.json
```bash
cat /Users/sunil/Downloads/SSBMax/app/google-services.json | grep project_id
```
Should show your Firebase project ID.

### Test 2: Build the app
```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh assembleDebug
```
Should build successfully (already done ✅).

### Test 3: Run on emulator/device
```bash
./gradle.sh installDebug
```
App should install and Firebase should initialize automatically.

---

## 🐛 Common Issues

### Issue 1: "Google Sign-In failed with error 10"
**Solution**: 
- Verify SHA-1 is added in Firebase Console
- Re-download google-services.json
- Rebuild app: `./gradle.sh clean assembleDebug`

### Issue 2: "Firestore permission denied"
**Solution**: 
- Check security rules are published
- Verify user is authenticated before accessing Firestore
- Check Firebase Console → Authentication → Users (should see logged-in user)

### Issue 3: "Firebase not initialized"
**Solution**: 
- Verify google-services.json is in `app/` directory
- Check build.gradle.kts has `id("com.google.gms.google-services")`
- Rebuild app

---

## 📚 Useful Firebase Console URLs

Replace `YOUR_PROJECT_ID` with your actual project ID:

- **Project Overview**: https://console.firebase.google.com/project/YOUR_PROJECT_ID
- **Authentication**: https://console.firebase.google.com/project/YOUR_PROJECT_ID/authentication
- **Firestore**: https://console.firebase.google.com/project/YOUR_PROJECT_ID/firestore
- **Storage**: https://console.firebase.google.com/project/YOUR_PROJECT_ID/storage
- **Project Settings**: https://console.firebase.google.com/project/YOUR_PROJECT_ID/settings/general

---

## 🎉 You're Ready!

Once you complete the checklist above, Firebase is fully configured and ready to use in your app!

**Next steps**: Let me know when you've finished these console steps, and I'll provide the code implementation for Firebase Authentication and Firestore integration.

Good luck! 🚀

