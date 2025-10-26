# 🎨 Visual Authentication Flow Guide

## 📱 Complete User Journey

```
┌─────────────────────────────────────────────────────────────────┐
│                        APP LAUNCH                                │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPLASH SCREEN                                 │
│                  (2 second delay)                                │
│                                                                   │
│              "SSBMax - Your Path to SSB Success"                 │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       │ Check: Is user signed in?
                       │
         ┌─────────────┴──────────────┐
         │                            │
         ▼ NO                         ▼ YES
┌──────────────────┐         ┌──────────────────┐
│  LOGIN SCREEN    │         │   HOME SCREEN    │
│  (Your current   │         │   (Dashboard)    │
│   screenshot)    │         │                  │
└─────────┬────────┘         └──────────────────┘
          │                           ▲
          │                           │
          │ Click "Continue           │
          │  with Google"             │
          │                           │
          ▼                           │
┌──────────────────┐                  │
│  GOOGLE SIGN-IN  │                  │
│    DIALOG        │                  │
│                  │                  │
│  "Choose an      │                  │
│   account"       │                  │
└─────────┬────────┘                  │
          │                           │
          │ Select account            │
          │                           │
          ▼                           │
┌──────────────────┐                  │
│   PROCESSING     │                  │
│   Firebase Auth  │                  │
│   Create Profile │                  │
└─────────┬────────┘                  │
          │                           │
          │ Success                   │
          └───────────────────────────┘
```

## 🔍 Where You're Getting Stuck

```
┌──────────────────┐
│  LOGIN SCREEN    │  ← You see this screen
│                  │
│  [Continue with  │
│      Google]     │  ← You click this button
└─────────┬────────┘
          │
          │ EXPECTED: Google dialog appears,
          │           you sign in, navigate to home
          │
          ▼
     ❌ STUCK HERE ❌
          │
          │ ACTUAL: Something fails
          │        (need logs to see what)
          │
          ✗
```

## 🎯 Current Behavior vs Expected

### First Launch (New User)
```
┌─────────┐     ┌───────┐     ┌────────┐     ┌──────┐     ┌──────┐
│ Splash  │ --> │ Login │ --> │ Google │ --> │ Role │ --> │ Home │
│ Screen  │     │ Screen│     │Sign-In │     │Select│     │Screen│
└─────────┘     └───────┘     └────────┘     └──────┘     └──────┘
                    ✅            ❌            ?            ?
               YOU ARE HERE    STUCK HERE
```

### Subsequent Launch (Already Signed In)
```
┌─────────┐     ┌──────┐
│ Splash  │ --> │ Home │
│ Screen  │     │Screen│
└─────────┘     └──────┘
    ✅            ✅
 WORKING      WORKING
```

## 📊 Authentication State Machine

```
                    ┌─────────────────┐
                    │   INITIAL       │
                    │  (Not signed in)│
                    └────────┬────────┘
                             │
                Click Google │
                Sign-In      │
                             ▼
                    ┌─────────────────┐
                    │    LOADING      │
                    │ (Processing...)  │
                    └────────┬────────┘
                             │
                  ┌──────────┴──────────┐
                  │                     │
                  ▼ SUCCESS             ▼ ERROR
         ┌─────────────────┐   ┌─────────────────┐
         │  AUTHENTICATED  │   │  ERROR STATE    │
         │                 │   │  (Show error)    │
         └────────┬────────┘   └─────────────────┘
                  │
                  ▼
         ┌─────────────────┐
         │   NAVIGATE      │
         │   TO HOME       │
         └─────────────────┘
```

## 🔄 Your App's Current Flow

### What's Working ✅
```
Launch App
    │
    ▼
Check Auth State ───► User exists? ───► YES ───► Navigate to Home ✅
    │
    ▼
  NO
    │
    ▼
Show Login Screen ✅
    │
    ▼
Display "Continue with Google" Button ✅
```

### What's NOT Working ❌
```
Click "Continue with Google"
    │
    ▼
Launch Google Sign-In Dialog ✅
    │
    ▼
User selects account ✅
    │
    ▼
❌ FAILS HERE - Need to check:
    │
    ├─► Firebase Authentication?
    │
    ├─► Firestore Profile Creation?
    │
    └─► Navigation Callback?
```

## 🎬 Code Execution Flow (With Logging)

```
USER CLICKS BUTTON
    │
    ▼
LoginScreen.kt (Line 114)
    │ "LoginScreen: Google Sign-In button clicked"
    │
    ▼
viewModel.getGoogleSignInIntent()
    │
    ▼
googleSignInLauncher.launch(signInIntent)
    │ "LoginScreen: Launching Google Sign-In intent"
    │
    ▼
[ Google Sign-In UI appears ]
    │
    │ User selects account
    │
    ▼
LoginScreen.kt (Line 41) - Callback
    │ "LoginScreen: Google Sign-In result: resultCode=..."
    │
    ▼
viewModel.handleGoogleSignInResult(data)
    │
    ▼
AuthViewModel.kt (Line 41)
    │ "AuthViewModel: handleGoogleSignInResult called"
    │
    ▼
authRepositoryImpl.handleGoogleSignInResult(data)
    │
    ▼
AuthRepositoryImpl.kt (Line 84)
    │ "AuthRepositoryImpl: Starting authentication"
    │
    ▼
firebaseAuthService.handleSignInResult(data)
    │
    ▼
FirebaseAuthService.kt (Line 97)
    │ "FirebaseAuthService: Processing Google sign-in data"
    │
    ▼
firebaseAuthWithGoogle(account)
    │
    ▼
FirebaseAuthService.kt (Line 122)
    │ "FirebaseAuthService: Authenticating with Firebase"
    │
    ├─► SUCCESS ─────────────┐
    │                        │
    │                        ▼
    │            "Firebase authentication successful"
    │                        │
    │                        ▼
    │            Load/Create User Profile
    │                        │
    │                        ├─► SUCCESS ────────────┐
    │                        │                       │
    │                        │                       ▼
    │                        │         "User profile loaded successfully"
    │                        │                       │
    │                        │                       ▼
    │                        │         Update UI State to SUCCESS
    │                        │                       │
    │                        │                       ▼
    │                        │         Navigate to Home
    │                        │
    │                        └─► ERROR ──► "Failed to load/create profile"
    │
    └─► ERROR ──────────────► "Firebase authentication error"
```

## 🚨 Possible Failure Points

```
1. ┌──────────────────────────┐
   │ SHA-1 Not Configured     │  Most Likely
   │ Error Code: 10           │  ⭐⭐⭐⭐⭐
   └──────────────────────────┘
         │
         ▼ Fails at: FirebaseAuthService.handleSignInResult()
         
2. ┌──────────────────────────┐
   │ Firestore Rules          │  Likely
   │ PERMISSION_DENIED        │  ⭐⭐⭐⭐
   └──────────────────────────┘
         │
         ▼ Fails at: loadOrCreateUserProfile()
         
3. ┌──────────────────────────┐
   │ Navigation Not Triggered │  Possible
   │ UI State Not Updated     │  ⭐⭐⭐
   └──────────────────────────┘
         │
         ▼ Fails at: LoginScreen LaunchedEffect
         
4. ┌──────────────────────────┐
   │ Google Sign-In Cancelled │  Unlikely
   │ User Pressed Back        │  ⭐⭐
   └──────────────────────────┘
         │
         ▼ Fails at: Activity Result (resultCode=0)
```

## 📈 Progress Tracking

Your current state:
```
[████████████░░░░░░░░] 60% Complete

✅ Project Setup
✅ Firebase Integration
✅ Authentication UI
✅ Splash Screen Logic
✅ Auto-Login After First Sign-In
❌ First-Time Sign-In Flow  ← YOU ARE HERE
□  Role Selection
□  Profile Completion
□  Home Screen Access
```

## 🎯 Next Steps

```
Step 1: Run App with Logging
    │
    ▼
Step 2: Click "Continue with Google"
    │
    ▼
Step 3: Check Logcat Output
    │
    ▼
Step 4: Identify Where It Fails
    │
    ├─► "ApiException: 10"
    │      └─► Fix: Add SHA-1 to Firebase
    │
    ├─► "PERMISSION_DENIED"
    │      └─► Fix: Update Firestore Rules
    │
    ├─► "No logs after button click"
    │      └─► Fix: Check button onClick
    │
    └─► Other error
           └─► Share logs for specific solution
```

---

## 💡 Remember

**The second behavior (auto-login on relaunch) is NOT a bug!**

```
┌─────────────────────────────────────┐
│  This is HOW AUTH SHOULD WORK:      │
│                                      │
│  Sign in once → Stay signed in       │
│  Close app → Still signed in         │
│  Relaunch → Skip login screen        │
│                                      │
│  Just like Gmail, Instagram, etc.    │
└─────────────────────────────────────┘
```

To test login screen again: **Sign Out** first, then close and relaunch.

