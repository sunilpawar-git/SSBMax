# 🔧 Check Google Cloud Console API Restrictions

## ✅ Confirmed Working:
- SHA-1 certificate: `bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:05` ✅
- google-services.json updated ✅
- Package name: `com.ssbmax` ✅
- Google Sign-In provider: **Enabled** ✅

## 🚨 Next Most Likely Issue: API Key Restrictions

Google Cloud Console might have API restrictions that are blocking your Android app.

---

## 🔍 Step 1: Open Google Cloud Console

1. **Go to**: https://console.cloud.google.com
2. **Select project**: Look for `ssbmax-49e68` in the project dropdown at the top
   - If you don't see it, make sure you're signed in with the same Google account as Firebase

---

## 🔍 Step 2: Navigate to Credentials

1. Click **"APIs & Services"** in the left menu (or use the hamburger menu ☰)
2. Click **"Credentials"**
3. You'll see a list of API keys and OAuth clients

---

## 🔍 Step 3: Find Your Android OAuth Client

Look for your **Android client**:
```
Name: "Web client (auto created by Google Service)"
OR
Client ID: YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com
```

**Click on it** to open the details.

---

## 🔍 Step 4: Check Application Restrictions

In the OAuth client details page:

### Application restrictions section:

Should show:
```
Application type: Android
Package name: com.ssbmax
SHA-1 certificate fingerprint: BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

**If SHA-1 is missing or different:**
- Click "Edit"
- Add the correct SHA-1
- Save

---

## 🔍 Step 5: Check API Restrictions

Scroll down to **"API restrictions"** section.

### Expected setting (for development):
```
⚪ Don't restrict key
```

### If it shows:
```
⚫ Restrict key to selected APIs
   - Google Maps API
   - [other APIs but NOT Google Sign-In]
```

**This is the problem!** The API key is restricted and doesn't include Google Sign-In API.

---

## 🔧 Fix: Remove API Restrictions (For Testing)

1. **Click "Edit OAuth client"** (pencil icon or Edit button)
2. Scroll to **"API restrictions"**
3. Select **"Don't restrict key"** (the radio button)
4. **Click "Save"** at the bottom
5. **Wait 5-10 minutes** for changes to propagate

---

## 🔍 Alternative: Add Google Sign-In API to Allowed APIs

If you want to keep restrictions (more secure):

1. Keep **"Restrict key to selected APIs"** selected
2. In the dropdown below, **add**:
   - Google Sign-In API
   - Google Identity Toolkit API
   - Google Cloud APIs
3. **Click "Save"**
4. **Wait 5-10 minutes**

---

## 🧪 Test After Changes

After removing restrictions or adding APIs:

```bash
cd /Users/sunil/Downloads/SSBMax

# Wait 5-10 minutes for changes to propagate, then:

# Clear app data
adb shell pm clear com.ssbmax

# Rebuild app
./gradle.sh installDebug

# Watch logs
adb logcat -c
adb logcat -s LoginScreen:D AuthViewModel:D FirebaseAuthService:D

# Launch app and try signing in
```

---

## 📊 Expected Behavior After Fix

### Before (Current - resultCode=0):
```
LoginScreen: Google Sign-In button clicked
LoginScreen: Launching Google Sign-In intent
[Nothing happens or brief flash]
LoginScreen: Google Sign-In result: resultCode=0
```

### After (Fixed - resultCode=-1):
```
LoginScreen: Google Sign-In button clicked
LoginScreen: Launching Google Sign-In intent
[Google account picker appears]
[Select account]
LoginScreen: Google Sign-In result: resultCode=-1
AuthViewModel: handleGoogleSignInResult called
FirebaseAuthService: Google account obtained: mail.sunilpawar@gmail.com
AuthViewModel: Sign-in SUCCESS
LoginScreen: Navigating to home screen
```

---

## 🔍 Step 6: Also Check API Key (Not OAuth Client)

There's also an **API Key** (different from OAuth client) that might need checking:

1. In **Credentials** page
2. Find **"API key"** section (not OAuth 2.0 Client IDs)
3. Look for key with name like `AIzaSy***REDACTED***`
4. **Click on it**
5. Check **"API restrictions"**:
   - Should be **"Don't restrict key"** (for testing)
   - OR include Google Sign-In API in allowed list

---

## 📸 Visual Guide

### What You're Looking For:

```
Google Cloud Console → APIs & Services → Credentials

┌────────────────────────────────────────┐
│ OAuth 2.0 Client IDs                    │
├────────────────────────────────────────┤
│                                         │
│ 🤖 Android client                       │  ← Click this
│    (auto created by Google Service)    │
│    836687498591-7t5...                  │
│                                         │
└────────────────────────────────────────┘

Then check:
┌────────────────────────────────────────┐
│ API restrictions                        │
├────────────────────────────────────────┤
│ ⚪ Don't restrict key          ← Should be this
│ ⚫ Restrict key to selected APIs        │
└────────────────────────────────────────┘
```

---

## 🚨 Common Gotcha

**Propagation Time**: After making changes in Google Cloud Console, it can take **5-10 minutes** for the changes to take effect globally. Don't test immediately!

---

## 🆘 If You Can't Access Google Cloud Console

If you can't find the project in Google Cloud Console:

1. Make sure you're signed in with the **same Google account** as Firebase
2. The project should be: `ssbmax-49e68`
3. Try this direct link (replace with your project):
   ```
   https://console.cloud.google.com/apis/credentials?project=ssbmax-49e68
   ```

---

## 📞 Let Me Know

After checking Google Cloud Console:

1. **Are there API restrictions** on your Android OAuth client or API key?
2. **Did you remove them** or add Google Sign-In API?
3. **After waiting 5-10 minutes**, does sign-in work?

If still not working, we'll check for other issues like:
- Account type (personal vs G Suite)
- Network/proxy issues
- Device-specific Google Play Services problems

