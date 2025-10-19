# Firebase Setup Guide for SSBMax - Complete Beginner-Friendly Instructions

**Status:** Step-by-step guide for Phase 7B  
**Date:** October 17, 2025  
**Difficulty:** Beginner-friendly with detailed screenshots references

---

## 📋 What We're Setting Up

1. **Firebase Console** - Create project and get configuration
2. **Firebase Authentication** - Already using Google Sign-In
3. **Cloud Firestore** - Database for test submissions
4. **Firebase Storage** (optional) - For images (TAT/PPDT pictures)
5. **Cloud Functions** (optional) - For AI scoring triggers

---

## 🚀 Part 1: Firebase Console Setup (10 minutes)

### Step 1.1: Create Firebase Project

1. **Go to Firebase Console**
   - Open browser: https://console.firebase.google.com
   - Sign in with your Google account

2. **Create New Project**
   - Click "Add project" (big + button)
   - Project name: `SSBMax` (or `SSBMax-Dev` for testing)
   - Click "Continue"

3. **Google Analytics** (Optional but recommended)
   - Toggle ON "Enable Google Analytics"
   - Select "Default Account for Firebase" or create new
   - Click "Create project"
   - Wait 30-60 seconds for project creation

4. **Project Created!**
   - Click "Continue" when ready
   - You'll see the Firebase Console dashboard

---

### Step 1.2: Add Android App to Firebase

1. **Add Android App**
   - On Firebase Console home, click Android icon (robot)
   - OR: Click gear icon (⚙️) → Project settings → "Add app" → Android

2. **Register Your App**
   - **Android package name**: `com.ssbmax` (MUST match your app's package)
   - **App nickname**: `SSBMax Android` (optional)
   - **Debug signing certificate SHA-1**: (We'll get this now)

3. **Get SHA-1 Certificate** (Required for Google Sign-In)
   
   **Option A: Using Android Studio Terminal**
   ```bash
   cd /Users/sunil/Downloads/SSBMax
   ./gradlew signingReport
   ```
   
   Look for output like:
   ```
   Variant: debug
   Config: debug
   Store: /Users/sunil/.android/debug.keystore
   Alias: AndroidDebugKey
   MD5: XX:XX:XX...
   SHA1: A1:B2:C3:D4:E5:F6:... (COPY THIS!)
   SHA-256: XX:XX:XX...
   ```
   
   **Option B: Using keytool command**
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

4. **Paste SHA-1**
   - Copy the SHA-1 value (format: `A1:B2:C3:...`)
   - Paste into Firebase Console "Debug signing certificate SHA-1" field
   - Click "Register app"

---

### Step 1.3: Download google-services.json

1. **Download Configuration File**
   - Firebase will show "Download google-services.json"
   - Click "Download google-services.json"
   - Save the file

2. **Move to Project**
   ```bash
   # Move to app/ directory (NOT project root!)
   mv ~/Downloads/google-services.json /Users/sunil/Downloads/SSBMax/app/
   ```

3. **Verify Placement**
   - File should be at: `SSBMax/app/google-services.json`
   - NOT at: `SSBMax/google-services.json` ❌

4. **Click "Next"** in Firebase Console

---

### Step 1.4: Add Firebase SDK (We'll do this in Part 2)

- Click "Next" for now
- Click "Continue to console"

---

## 🔧 Part 2: Update Android Project Configuration (15 minutes)

### Step 2.1: Update Project-Level build.gradle

**File:** `build.gradle.kts` (Project root)

Find the `plugins` block and add Google Services:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false  // ADD THIS LINE
}
```

**Why?** This adds the Google Services plugin needed to read `google-services.json`

---

### Step 2.2: Update gradle/libs.versions.toml

Add Firebase BOM (Bill of Materials) for version management:

```toml
[versions]
# ... existing versions ...
firebaseBom = "32.7.0"  # ADD THIS

[libraries]
# ... existing libraries ...

# Firebase - ADD THESE LINES
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
firebase-storage = { group = "com.google.firebase", name = "firebase-storage-ktx" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }

# Play Services for Auth - ADD THIS
play-services-auth = { group = "com.google.android.gms", name = "play-services-auth", version = "20.7.0" }
```

**Why?** BOM ensures all Firebase libraries use compatible versions

---

### Step 2.3: Update app/build.gradle.kts

1. **Apply Google Services Plugin** (at the TOP, after other plugins):

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")  // ADD THIS LINE
}
```

2. **Add Firebase Dependencies** (in the `dependencies` block):

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Firebase - ADD THESE LINES
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
    
    // Google Sign-In - ADD THIS
    implementation(libs.play.services.auth)
    
    // Coroutines for Firebase (if not already added)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
```

**Why?** These add Firebase capabilities to your app

---

### Step 2.4: Sync Project

1. **In Android Studio:**
   - Click "Sync Now" banner at top
   - OR: File → Sync Project with Gradle Files

2. **Wait for sync** (30-60 seconds)

3. **Check for errors:**
   - Should say "BUILD SUCCESSFUL"
   - If errors, check version numbers match

---

## 🔥 Part 3: Enable Firebase Services in Console (5 minutes)

### Step 3.1: Enable Authentication

1. **Go to Firebase Console** → Your project (SSBMax)
2. **Left sidebar** → Click "Authentication"
3. **Click "Get Started"**
4. **Sign-in method tab** → Click "Google"
5. **Toggle "Enable"** → Click "Save"
6. **Project public-facing name:** `SSBMax`
7. **Project support email:** (your email)
8. **Click "Save"**

✅ Google Sign-In is now enabled!

---

### Step 3.2: Enable Cloud Firestore

1. **Left sidebar** → Click "Firestore Database"
2. **Click "Create database"**
3. **Location:** Choose closest region
   - Asia: `asia-south1` (Mumbai)
   - US: `us-central1`
4. **Start in production mode** → Click "Next"
   - (We'll add security rules later)
5. **Click "Enable"**
6. Wait 30-60 seconds

✅ Firestore is now ready!

---

### Step 3.3: Set Up Security Rules (Important!)

**Firestore Rules Tab** → Replace with these rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can read/write their own user document
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Users can read/write their own submissions
    match /submissions/{submissionId} {
      allow read: if request.auth != null && 
                     (resource.data.userId == request.auth.uid || 
                      resource.data.instructorId == request.auth.uid);
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null && 
                       (resource.data.userId == request.auth.uid || 
                        resource.data.instructorId == request.auth.uid);
    }
    
    // Batches: Students can read, instructors can write
    match /batches/{batchId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null && 
                               request.resource.data.instructorId == request.auth.uid;
    }
    
    // Test configs: Public read, admin write only
    match /test_configs/{testId} {
      allow read: if true;
      allow write: if false; // Only via Firebase Console/Admin SDK
    }
  }
}
```

**Click "Publish"**

**Why?** Security rules prevent unauthorized data access

---

### Step 3.4: Enable Firebase Storage (Optional - for TAT/PPDT images)

1. **Left sidebar** → Click "Storage"
2. **Click "Get Started"**
3. **Start in production mode** → Click "Next"
4. **Choose location:** Same as Firestore
5. **Click "Done"**

---

## 📝 Part 4: Verify Setup (5 minutes)

### Step 4.1: Build the Project

```bash
cd /Users/sunil/Downloads/SSBMax
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug --no-daemon
```

**Expected:** `BUILD SUCCESSFUL`

### Step 4.2: Check Firebase Initialization

The app should now automatically initialize Firebase on startup.

---

## 🎯 Part 5: What's Next?

Now that Firebase is set up, we'll implement:

1. **Firebase Auth Service** - Connect Google Sign-In to Firebase
2. **Firestore Repository** - Save/load test submissions
3. **Real-time Listeners** - Listen for grading updates
4. **Offline Support** - Enable local caching

---

## 🐛 Troubleshooting Common Issues

### Issue 1: "google-services.json not found"

**Solution:**
```bash
# Verify file location
ls -la /Users/sunil/Downloads/SSBMax/app/google-services.json

# If missing, download again from Firebase Console
# Project Settings → Your apps → google-services.json
```

---

### Issue 2: "Failed to resolve: firebase-bom"

**Solution:**
- Check internet connection
- Sync project again
- Check version number matches in `libs.versions.toml`

---

### Issue 3: "SHA-1 certificate mismatch"

**Solution:**
```bash
# Get SHA-1 again
cd /Users/sunil/Downloads/SSBMax
./gradlew signingReport | grep SHA1

# Add to Firebase Console:
# Project Settings → Your apps → Add fingerprint
```

---

### Issue 4: "Google Sign-In fails with error 10"

**Solution:**
- Verify package name matches: `com.ssbmax`
- Verify SHA-1 is added to Firebase Console
- Download latest `google-services.json`
- Rebuild app

---

## 📚 Firebase Console URLs (Bookmark These!)

- **Project Console**: https://console.firebase.google.com/project/ssbmax
- **Authentication**: https://console.firebase.google.com/project/ssbmax/authentication
- **Firestore Database**: https://console.firebase.google.com/project/ssbmax/firestore
- **Storage**: https://console.firebase.google.com/project/ssbmax/storage
- **Project Settings**: https://console.firebase.google.com/project/ssbmax/settings/general

---

## ✅ Checklist Before Moving to Code Implementation

- [ ] Firebase project created
- [ ] Android app registered with correct package name
- [ ] SHA-1 certificate added
- [ ] `google-services.json` downloaded and placed in `app/` directory
- [ ] `build.gradle.kts` files updated with Firebase dependencies
- [ ] `gradle/libs.versions.toml` updated with Firebase BOM
- [ ] Project synced successfully
- [ ] Authentication enabled (Google Sign-In)
- [ ] Cloud Firestore created and security rules set
- [ ] Build successful with no errors

---

## 🎉 You're Ready!

Once you complete this setup, I'll provide the code implementation for:

1. Firebase Auth Service
2. Firestore Repository
3. Real-time submission sync
4. Offline support

**Estimated Time:** Parts 1-5 should take ~30-40 minutes total for a beginner.

Take your time, follow each step carefully, and let me know when you're ready for the code implementation! 🚀

---

## 💡 Pro Tips

1. **Use Test Mode Initially**: Start with test data, switch to production later
2. **Bookmark Console**: Keep Firebase Console open in a browser tab
3. **Check Logs**: Firebase → Analytics → DebugView for real-time app events
4. **Budget Alerts**: Set up billing alerts (Firebase is free for MVP, but good practice)
5. **Backup Rules**: Copy security rules to a file before making changes

---

**Need Help?** Common questions answered:

**Q: Do I need a credit card?**  
A: No! Firebase Spark plan (free) is sufficient for development and MVP.

**Q: Can I use this in production?**  
A: Yes, but upgrade to Blaze plan (pay-as-you-go) when you exceed free limits.

**Q: What if I make a mistake?**  
A: You can delete and recreate the project. All Firebase data is separate from your app code.

**Q: Can I test without a real device?**  
A: Yes, Android Emulator works fine. Google Sign-In works on emulator too.

---

**Ready to proceed?** Complete these steps and reply "Firebase setup complete" when done, and I'll provide the code implementation for Phase 7B! 🔥

