# ✅ Correct API Restrictions for SSBMax

## Your App's Features

Based on SSBMax implementation:
- ✅ **Google Sign-In** (Firebase Authentication with Google provider)
- ✅ **Cloud Firestore** (user profiles, test data, submissions, progress)
- ✅ **Push Notifications** (FCM for test reminders, updates)
- ✅ **Cloud Storage** (test images, study materials - if implemented)

---

## 🎯 Required APIs for SSBMax

### You Currently Have Selected (5 APIs): ✅ CORRECT!

1. ✅ **Firebase Installations API**
   - Required: YES
   - Purpose: Firebase SDK initialization and device management
   - Used by: All Firebase services

2. ✅ **Cloud Firestore API**
   - Required: YES ⭐
   - Purpose: Database for users, test results, study materials, submissions
   - Used by: Your `FirestoreUserRepository`, test data, progress tracking

3. ✅ **Token Service API**
   - Required: YES
   - Purpose: Authentication tokens and credential management
   - Used by: Google Sign-In authentication flow

4. ✅ **Firebase Cloud Messaging API**
   - Required: YES ⭐ (You have notification plans!)
   - Purpose: Push notifications
   - Used by: Test reminders, new content alerts, submission feedback

5. ✅ **FCM Registration API**
   - Required: YES
   - Purpose: Register devices for push notifications
   - Used by: FCM device token management

---

## ❓ About "Firebase Authentication API"

**Why you don't see it:**

Firebase Authentication is NOT a separate API in the Google Cloud Console API restrictions list. Here's why:

### Firebase Auth Uses These APIs Instead:
- ✅ **Identity Toolkit API** (this is Firebase Authentication's backend)
- ✅ **Token Service API** (already selected!)

### To Add Identity Toolkit API:

1. In the "Restrict key" dropdown where you have "5 APIs"
2. Click to expand and search for: **"Identity Toolkit API"**
3. Select it (this is Firebase Authentication)

---

## 🎯 FINAL Recommended API List for SSBMax

Here's what you should select:

```
✅ Selected APIs (6 total):

1. ☑ Cloud Firestore API                    (Database)
2. ☑ Firebase Cloud Messaging API           (Push notifications)
3. ☑ Firebase Installations API             (Firebase SDK)
4. ☑ FCM Registration API                   (Notification tokens)
5. ☑ Token Service API                      (Auth tokens)
6. ☑ Identity Toolkit API                   (Firebase Authentication)
                                            ↑ ADD THIS!
```

---

## 📊 Optional APIs (Add if you plan to use these features)

### If you're using Cloud Storage for files:
```
☐ Cloud Storage API
  Purpose: Store/retrieve test images, PDFs, study materials
  Add if: You upload/download files to Firebase Storage
```

### If you're using Firebase Realtime Database:
```
☐ Firebase Realtime Database API
  Purpose: Real-time data sync (alternative to Firestore)
  Add if: You're using Realtime Database (you're not - you use Firestore)
```

### If you're using Cloud Functions:
```
☐ Cloud Functions API
  Purpose: Server-side logic execution
  Add if: You have Firebase Cloud Functions deployed
```

### If you have analytics:
```
☐ Firebase Management API
  Purpose: Programmatic access to Firebase project settings
  Add if: You need to manage Firebase programmatically
```

---

## 🚫 APIs You DON'T Need

Based on your implementation, you can safely EXCLUDE:

- ❌ BigQuery APIs (unless doing data warehousing)
- ❌ Cloud SQL APIs (you use Firestore, not Cloud SQL)
- ❌ Compute Engine APIs (no VMs needed)
- ❌ Container Registry APIs (no Docker containers)
- ❌ App Engine APIs (not using App Engine)
- ❌ Cloud Run APIs (not using Cloud Run)
- ❌ Pub/Sub APIs (not using message queues)

---

## 📱 Your Current App Architecture

From your codebase:

### Authentication:
```kotlin
FirebaseAuthService.kt → Google Sign-In
  ↓
Uses: Identity Toolkit API (Firebase Auth backend)
Uses: Token Service API (for OAuth tokens)
```

### Database:
```kotlin
FirestoreUserRepository.kt → User profiles, test data
FirestoreTestRepository.kt → Test submissions, results
  ↓
Uses: Cloud Firestore API
```

### Notifications (Planned):
```kotlin
FCM integration → Push notifications
  ↓
Uses: Firebase Cloud Messaging API
Uses: FCM Registration API
```

---

## 🎯 Final Steps for You

### 1. Add Identity Toolkit API
```
Current: 5 APIs selected
Action: Click the dropdown → Search "Identity Toolkit" → Check it
Result: 6 APIs selected
```

### 2. Verify Your Selection
```
✅ Cloud Firestore API
✅ Firebase Cloud Messaging API  
✅ Firebase Installations API
✅ FCM Registration API
✅ Token Service API
✅ Identity Toolkit API  ← ADD THIS
```

### 3. Click Save

---

## 🔍 How to Find Identity Toolkit API

1. Look at your current screen with "5 APIs" dropdown
2. Click on the dropdown to expand the list of all available APIs
3. Scroll or search for: **"Identity Toolkit API"**
4. Check the box next to it
5. You should now see "6 APIs" in the dropdown
6. Click **Save** at the bottom

---

## ✅ Summary

**What you have now:** ✅ Mostly correct (5/6 APIs)  
**What to add:** Identity Toolkit API (Firebase Authentication)  
**Total APIs needed:** 6  
**Optional APIs:** Cloud Storage API (if you store files)

---

## 🎉 After Adding Identity Toolkit API

Your authentication will work with:
- ✅ Google Sign-In (via Identity Toolkit + Token Service)
- ✅ User session management
- ✅ Auth state monitoring
- ✅ Sign out and account deletion

All Firestore operations will work with:
- ✅ User profiles
- ✅ Test submissions  
- ✅ Progress tracking
- ✅ Study materials
- ✅ Real-time updates

Push notifications will work with:
- ✅ Test reminders
- ✅ New content alerts
- ✅ Submission feedback

---

**Action Required:** Add "Identity Toolkit API" to your selection, then click Save!

