# 🔧 Fix Google Sign-In - Complete Guide

## ✅ **Step 1: Add SHA-1 to Firebase (DO THIS FIRST)**

### Your SHA-1 Certificate:
```
BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

### Where to Add It:

1. **Open Firebase Console**
   - URL: https://console.firebase.google.com
   - Sign in with your Google account

2. **Select Your Project**
   - Click on **"SSBMax"** (or whatever you named your Firebase project)

3. **Go to Project Settings**
   - Click the ⚙️ **gear icon** at the top-left
   - Select **"Project settings"**

4. **Find Your Android App**
   - Scroll down to the **"Your apps"** section
   - Look for your Android app (package name: `com.ssbmax` or `com.example.ssbmax`)
   
   **If you don't see an Android app:**
   - Click **"Add app"** → Select **Android** icon
   - Enter package name: `com.ssbmax`
   - Download `google-services.json` and save to `app/` folder
   - Follow rest of wizard, then continue below

5. **Add SHA-1 Fingerprint**
   - In your app card, scroll to **"SHA certificate fingerprints"**
   - Click **"Add fingerprint"** button
   - **Copy and paste this SHA-1**:
     ```
     BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
     ```
   - Press **Enter** or click **Save**

---

## ✅ **Step 2: Enable Google Sign-In Method**

While you're in Firebase Console:

1. Click **"Authentication"** in the left sidebar
2. Click **"Sign-in method"** tab
3. Find **"Google"** in the providers list
4. Click on **"Google"**
5. Toggle the **"Enable"** switch to ON
6. Select a **"Public-facing name"**: `SSBMax`
7. Select **"Project support email"**: (your email)
8. Click **"Save"**

---

## ✅ **Step 3: Download New google-services.json**

1. Go back to **Project Settings** (⚙️ icon)
2. Scroll to **"Your apps"** → Your Android app
3. Look for **"google-services.json"** section
4. Click **"Download google-services.json"** button
5. Save to your **Downloads** folder

---

## ✅ **Step 4: Update Project and Test**

Run these commands:

```bash
cd /Users/sunil/Downloads/SSBMax

# Update google-services.json
./update_google_services.sh

# Clean and rebuild
./gradle.sh clean
./gradle.sh installDebug

# Clear app data (fresh start)
adb shell pm clear com.ssbmax

# Watch logs while testing
adb logcat -c
adb logcat -s LoginScreen:D AuthViewModel:D AuthRepositoryImpl:D FirebaseAuthService:D
```

Then:
1. **Launch your app**
2. **Click "Continue with Google"**
3. **You should now see** the Google account picker! ✅

---

## 📊 **Expected Results After Fix**

### Before (Current - Failing):
```
LoginScreen: Google Sign-In button clicked
LoginScreen: Launching Google Sign-In intent
LoginScreen: Google Sign-In result: resultCode=0  ❌
LoginScreen: User cancelled or error occurred
```

### After (Fixed - Working):
```
LoginScreen: Google Sign-In button clicked
LoginScreen: Launching Google Sign-In intent
[Account picker appears - select your account]
LoginScreen: Google Sign-In result: resultCode=-1  ✅
AuthViewModel: handleGoogleSignInResult called
FirebaseAuthService: Google account obtained: your.email@gmail.com
FirebaseAuthService: Firebase authentication successful
AuthViewModel: Sign-in SUCCESS
LoginScreen: Navigating to home screen
```

---

## 🎯 **Visual Checklist**

- [ ] SHA-1 added to Firebase Console
  ```
  BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
  ```

- [ ] Google Sign-In provider enabled in Firebase Authentication

- [ ] Downloaded new `google-services.json`

- [ ] Replaced `app/google-services.json` in project

- [ ] Ran `./gradle.sh clean`

- [ ] Ran `./gradle.sh installDebug`

- [ ] Cleared app data: `adb shell pm clear com.ssbmax`

- [ ] Tested sign-in - Should work now! ✅

---

## 🚨 **If It Still Doesn't Work**

### Check 1: Verify Package Name Matches

```bash
# Check package name in your app
grep "applicationId" app/build.gradle.kts
```

Make sure the package name in Firebase Console matches this.

### Check 2: Verify SHA-1 Was Added

In Firebase Console:
- Project Settings → Your App
- Look under "SHA certificate fingerprints"
- You should see: `BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05`

### Check 3: Check Logcat for New Errors

```bash
adb logcat | grep -i "google\|auth\|firebase"
```

Look for any error messages that might give more clues.

### Check 4: Verify google-services.json

```bash
# Check if file was updated
ls -la app/google-services.json

# View contents (should have your package name and client IDs)
cat app/google-services.json | grep "package_name\|client_id"
```

---

## 💡 **Why This Fixes the Issue**

Your logs showed:
```
LoginScreen: Google Sign-In result: resultCode=0
```

**`resultCode=0`** = `RESULT_CANCELED` = Google Sign-In failed before showing account picker

**Root Cause**: Firebase didn't recognize your app's signature (SHA-1)

**Solution**: Adding SHA-1 tells Firebase "this is a legitimate instance of the SSBMax app"

**Result**: Google Sign-In will now work! The account picker will appear, and after selection, you'll navigate to the home screen.

---

## 🎉 **After Successful Sign-In**

Once you sign in successfully:

1. **First time**: You'll see role selection or go to home screen
2. **Close and relaunch**: App will auto-login (this is correct behavior!)
3. **To test login again**: 
   - Open app drawer/profile
   - Click "Sign Out"
   - Close app
   - Relaunch → Login screen appears again

---

## 📞 **Still Having Issues?**

If after adding SHA-1 you still get `resultCode=0`:

1. Run the test again with logs:
   ```bash
   adb logcat -c
   adb logcat -s LoginScreen:D AuthViewModel:D AuthRepositoryImpl:D FirebaseAuthService:D
   ```

2. Try signing in

3. Copy the new logs and share them - we'll diagnose the next issue

But **99% of the time, adding SHA-1 fixes this exact error!** 🎯

