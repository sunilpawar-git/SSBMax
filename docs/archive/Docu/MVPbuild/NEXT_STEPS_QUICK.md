# Quick Next Steps - Firebase Console

## ✅ What You've Done So Far
- ✅ Google Sign-In enabled
- ✅ New google-services.json downloaded and replaced
- ✅ Project name set to "SSBMax"

---

## 🔐 Step 1: Add SHA-1 Certificate (5 minutes)

### Your SHA-1:
```
BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

### Instructions:
1. **Go to Firebase Console** → Your project (ssbmax-49e68)
2. Click **⚙️ (gear icon)** in top-left → **Project settings**
3. Scroll down to **"Your apps"** section
4. Find **"SSBMax Android"** (com.ssbmax)
5. Scroll to **"SHA certificate fingerprints"** section
6. Click **"Add fingerprint"** button
7. **Paste**: `BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05`
8. Click **"Save"**

✅ This enables Google Sign-In to work!

---

## 📊 Step 2: Create Firestore Database (5 minutes)

### Instructions:
1. **In Firebase Console** → Left sidebar → Click **"Firestore Database"**
2. Click **"Create database"** button
3. **Choose location**: 
   - For India: `asia-south1` (Mumbai)
   - For US: `us-central1`
4. **Security rules**: Select **"Start in production mode"**
5. Click **"Next"**
6. Click **"Enable"**
7. Wait 30-60 seconds for creation

✅ Database created!

---

## 🔒 Step 3: Set Firestore Security Rules

After database is created:

1. Click **"Rules"** tab in Firestore
2. **Replace** the existing rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can read/write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Test submissions - students can access their own, instructors can see their students'
    match /submissions/{submissionId} {
      allow read: if request.auth != null && 
                     (resource.data.userId == request.auth.uid || 
                      resource.data.instructorId == request.auth.uid);
      allow create: if request.auth != null && 
                       request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null && 
                       (resource.data.userId == request.auth.uid || 
                        resource.data.instructorId == request.auth.uid);
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    
    // Batches - readable by authenticated users, writable by instructors
    match /batches/{batchId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null && 
                               request.resource.data.instructorId == request.auth.uid;
    }
    
    // Test configs - public read, no write (admin only)
    match /test_configs/{testId} {
      allow read: if true;
      allow write: if false;
    }
    
    // User progress - users can only see their own
    match /user_progress/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

3. Click **"Publish"**

✅ Security rules active!

---

## ✅ Final Checklist

- [ ] SHA-1 certificate added to Firebase Console
- [ ] Firestore database created
- [ ] Firestore security rules published
- [ ] Clicked "Done" on the download config dialog (if still open)

---

## 🎯 When You're Done

Reply with: **"Firebase Console setup complete"**

Then I'll:
1. Rebuild the app with new config
2. Verify Firebase integration
3. Implement Firebase Authentication code
4. Implement Firestore data layer
5. Add offline support

---

**Estimated time**: 10-15 minutes total
**Current step**: Add SHA-1 certificate (Step 1)

🚀 Let's finish this!

