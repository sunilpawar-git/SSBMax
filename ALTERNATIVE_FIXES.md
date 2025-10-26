# 🔧 Alternative Fixes for resultCode=0

## Current Status:
- ✅ SHA-1 correct
- ✅ google-services.json updated  
- ✅ Google Sign-In enabled in Firebase
- ❌ Still getting resultCode=0

## 🎯 Most Likely: Google Cloud Console API Restrictions

**First, check `CHECK_GOOGLE_CLOUD_CONSOLE.md`** - This is 80% likely to be your issue.

---

## 🔧 Alternative Fix #1: Complete Clean Rebuild

Sometimes Android caches cause issues. Do a **complete nuclear clean**:

```bash
cd /Users/sunil/Downloads/SSBMax

# Stop any running app instances
adb shell am force-stop com.ssbmax

# Uninstall completely
adb uninstall com.ssbmax

# Clean everything
./gradle.sh clean
rm -rf app/build
rm -rf build
rm -rf .gradle
rm -rf ~/.gradle/caches

# Rebuild and install
./gradle.sh installDebug

# Launch and test
adb shell monkey -p com.ssbmax 1
```

Then try signing in.

---

## 🔧 Alternative Fix #2: Check Device Google Play Services

The device/emulator must have **Google Play Services** installed and updated.

### Check Play Services Version:

```bash
adb shell dumpsys package com.google.android.gms | grep versionName
```

Should show version 20+ (ideally 23+).

### If Using Emulator:

Make sure you're using an emulator **with Google APIs**:
- NOT: "Android 13 (Google APIs Intel x86 Atom System Image)"
- YES: "Android 13 (Google Play Intel x86_64 Atom System Image)"

The emulator must have the **Play Store** icon, not just Google APIs.

---

## 🔧 Alternative Fix #3: Try Different Google Account

Some accounts have restrictions:

1. **Corporate/G Suite accounts** - Sometimes blocked by organization
2. **Accounts with 2FA** - Can cause issues in development
3. **New accounts** - Sometimes need time to propagate

**Try:**
- Use a different personal Gmail account
- Use an older, well-established Google account
- Disable 2FA temporarily on the test account

---

## 🔧 Alternative Fix #4: Enable Verbose Google Sign-In Logs

Let's see the exact error from Google Sign-In API:

```bash
# Enable verbose logging
adb shell setprop log.tag.GoogleSignIn VERBOSE
adb shell setprop log.tag.GoogleAuth VERBOSE  
adb shell setprop log.tag.GoogleApiClient VERBOSE
adb shell setprop log.tag.Auth VERBOSE

# Clear and watch logs
adb logcat -c
adb logcat | grep -E "Google|Auth|SignIn|ApiException"

# Now try signing in and share ALL the output
```

Look for lines with:
- `ApiException`
- `DEVELOPER_ERROR`
- `ERROR_CODE`
- `Status{statusCode=`

---

## 🔧 Alternative Fix #5: Verify Web Client ID is Being Used

Let's make sure the Web Client ID is correctly extracted from google-services.json.

Add temporary logging:

```bash
cd /Users/sunil/Downloads/SSBMax
```

Let me add a log statement to see what Web Client ID is being used:

<function_calls>
<invoke name="search_replace">
<parameter name="file_path">core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirebaseAuthService.kt
