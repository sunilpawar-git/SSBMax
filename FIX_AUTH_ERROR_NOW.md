# 🔥 Fix Firebase Authentication Error - QUICK GUIDE

## 🎯 The Issue

After applying API key restrictions, Firebase Authentication may fail due to:
1. **Identity Toolkit API not in the allowed APIs list** ⭐ Most Likely!
2. **API restrictions still propagating** (5-10 minutes)
3. **SHA-1 mismatch**
4. **Google Sign-In not enabled in Firebase Console**

---

## ✅ Solution #1: Verify Identity Toolkit API (MOST LIKELY FIX)

### Step 1: Go to Google Cloud Console Credentials

https://console.cloud.google.com/apis/credentials?project=ssbmax-49e68

### Step 2: Click on Your API Key

Find the key with "Android apps" restriction and click on it.

### Step 3: Check API Restrictions

Scroll to "API restrictions" section and verify these 7 APIs are checked:

```
☑ Cloud Firestore API
☑ Firebase Cloud Messaging API
☑ Firebase Installations API
☑ FCM Registration API
☑ Token Service API
☑ Identity Toolkit API          ← THIS IS CRITICAL FOR AUTH!
☑ Cloud Storage API
```

### Step 4: If Identity Toolkit API is Missing

1. Click on the dropdown showing "6 APIs" or "7 APIs"
2. Search for: **"Identity Toolkit API"**
3. Check the box
4. Scroll down and click **Save**
5. Wait 10 minutes for propagation

---

## ✅ Solution #2: Enable Google Sign-In in Firebase Console

### Check if Google Sign-In is Enabled:

1. Go to: https://console.firebase.google.com/project/ssbmax-49e68/authentication/providers
2. Under **Sign-in providers**, check if **Google** is enabled
3. If status shows "Disabled":
   - Click on **Google**
   - Toggle **Enable**
   - Add support email
   - Click **Save**

---

## ✅ Solution #3: Verify SHA-1 Certificate

### Your SHA-1:
```
bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:d5
```

### Check in Firebase Console:
1. Go to: https://console.firebase.google.com/project/ssbmax-49e68/settings/general
2. Scroll to **Your apps** → **SSBMax Android**
3. Under **SHA certificate fingerprints**, verify:
   ```
   bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:d5
   ```
4. If missing or different, click **Add fingerprint** and add it

### Check in Google Cloud Console:
1. Go to API key restrictions
2. Under **Application restrictions** → **Android apps**
3. Verify SHA-1 matches

---

## ✅ Solution #4: Wait for API Propagation

**If you just applied restrictions**:
- Wait: **10-15 minutes**
- Then rebuild and test

**Timeline**:
- Restrictions applied: ~08:30
- Propagation complete: ~08:45
- Current time: Check your clock

---

## 🧪 Test After Each Fix

### Rebuild and Test:
```bash
cd /Users/sunil/Downloads/SSBMax

# Clean build
./gradle.sh clean

# Build and install
./gradle.sh installDebug
```

### Launch App and Test:
1. Open SSBMax app
2. Click "Sign in with Google"
3. Select your Google account
4. Check if authentication succeeds

---

## 🔍 Specific Error Messages

### If you see: "API key not valid"
**Cause**: Identity Toolkit API not in allowed list  
**Fix**: Add Identity Toolkit API to restrictions (#Solution 1)

### If you see: "The application signature is not valid"
**Cause**: SHA-1 fingerprint mismatch  
**Fix**: Verify and add SHA-1 fingerprint (#Solution 3)

### If you see: "Error 12500" or "Sign-in failed"
**Cause**: API configuration issue  
**Fix**: 
1. Check Identity Toolkit API
2. Verify Google Sign-In enabled in Firebase
3. Wait for propagation

### If you see: "Web Client ID not found"
**Cause**: google-services.json not processed  
**Fix**: 
```bash
./gradle.sh clean
./gradle.sh processDebugGoogleServices
./gradle.sh assembleDebug
```

---

## 📋 Complete Checklist

### In Google Cloud Console:
- [ ] Opened API key restrictions
- [ ] Verified **7 APIs** are selected (not 6)
- [ ] **Identity Toolkit API** is checked ✓
- [ ] SHA-1 fingerprint is added
- [ ] Clicked Save
- [ ] Waited 10 minutes

### In Firebase Console:
- [ ] Authentication → Sign-in method
- [ ] Google provider is **Enabled**
- [ ] OAuth web client ID is configured
- [ ] SHA-1 fingerprint is added to app

### In Your Project:
- [ ] google-services.json is the NEW rotated version
- [ ] Package name is com.ssbmax
- [ ] Rebuilt project: `./gradle.sh clean assembleDebug`
- [ ] Installed fresh APK: `./gradle.sh installDebug`
- [ ] Tested Google Sign-In

---

## ⚡ Quick Fix Command

Run this to rebuild and reinstall:

```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh clean installDebug
```

Then test authentication in the app.

---

## 🎯 Most Likely Solution

**Based on the symptoms, the most likely fix is:**

1. **Go to Google Cloud Console API key**
2. **Add "Identity Toolkit API" to restrictions**
3. **Click Save**
4. **Wait 10 minutes**
5. **Rebuild and test**

This API is the backend for Firebase Authentication and MUST be in the allowed list!

---

## 📊 Expected Behavior After Fix

✅ Click "Sign in with Google"  
✅ Google account picker appears  
✅ Select account  
✅ Authentication succeeds  
✅ Redirected to app (role selection or home)  

❌ No "API key not valid" errors  
❌ No "Authentication failed" errors  
❌ No "Sign-in failed" errors  

---

## 🆘 If Still Not Working

### Double-Check These:

1. **All 7 APIs are in the allowed list**:
   - Cloud Firestore API
   - Firebase Cloud Messaging API
   - Firebase Installations API
   - FCM Registration API
   - Token Service API
   - **Identity Toolkit API** ← Critical!
   - Cloud Storage API

2. **Google Sign-In enabled** in Firebase Console

3. **SHA-1 added** in both Firebase and Google Cloud

4. **Waited full 15 minutes** after applying restrictions

5. **Fresh install** of the app (not just redeployment)

---

## 💡 Why This Happened

When we restricted the API keys earlier, we need to explicitly allow **Identity Toolkit API**, which is the backend service for Firebase Authentication.

Without it in the allowed list, authentication requests are blocked even though you have the correct keys and configuration!

---

## 🚀 Summary

**Quick Fix (90% chance this solves it)**:
1. Add **Identity Toolkit API** to Google Cloud API key restrictions
2. Save and wait 10 minutes
3. Rebuild: `./gradle.sh clean installDebug`
4. Test authentication

**This should resolve your authentication error!** ✅

---

*Created: Oct 22, 2025*  
*Issue: Firebase Auth error after API restrictions*  
*Solution: Add Identity Toolkit API to allowed APIs list*

