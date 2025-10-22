# 🔥 Firebase Authentication Error - Troubleshooting Guide

## 🚨 Possible Issues After API Key Restrictions

After restricting API keys, authentication errors can occur due to:

1. **Identity Toolkit API not enabled** ❌
2. **API key restrictions too strict** ⚠️
3. **SHA-1 fingerprint mismatch** ⚠️
4. **5-minute propagation delay** ⏱️
5. **Code syntax error** (found one!)

---

## 🐛 Code Issue Found!

### Problem: Syntax Error in FirebaseAuthService.kt

**File**: `core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirebaseAuthService.kt`

**Line 95-109**: Missing `return try {` statement!

**Current (BROKEN)**:
```kotlin
suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> {
    
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)
        
        if (account == null) {
            return Result.failure(Exception("Google Sign-In failed: Account is null"))
        }
        
        firebaseAuthWithGoogle(account)
    } catch (e: ApiException) {
        Result.failure(Exception("Google Sign-In failed: ${e.message}", e))
    } catch (e: Exception) {
        Result.failure(Exception("Authentication error: ${e.message}", e))
    }
}
```

**Should be (FIXED)**:
```kotlin
suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> {
    return try {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)
        
        if (account == null) {
            return Result.failure(Exception("Google Sign-In failed: Account is null"))
        }
        
        firebaseAuthWithGoogle(account)
    } catch (e: ApiException) {
        Result.failure(Exception("Google Sign-In failed: ${e.message}", e))
    } catch (e: Exception) {
        Result.failure(Exception("Authentication error: ${e.message}", e))
    }
}
```

**Missing**: `return try {` on line 96!

---

## ✅ Fix #1: Code Syntax Error

### Apply This Fix:

1. Open: `core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirebaseAuthService.kt`
2. Go to line 95-96
3. Change from:
   ```kotlin
   suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> {
       
           val task = GoogleSignIn.getSignedInAccountFromIntent(data)
   ```
4. To:
   ```kotlin
   suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> {
       return try {
           val task = GoogleSignIn.getSignedInAccountFromIntent(data)
   ```

---

## ✅ Fix #2: Verify Identity Toolkit API

### Check in Google Cloud Console:

1. Go to: https://console.cloud.google.com/apis/credentials?project=ssbmax-49e68
2. Click on your API key
3. Under "API restrictions" → Make sure these are checked:
   - ☑ **Identity Toolkit API** (Firebase Authentication)
   - ☑ Token Service API
   - ☑ Firebase Installations API
   - ☑ Cloud Firestore API
   - ☑ Firebase Cloud Messaging API
   - ☑ FCM Registration API
   - ☑ Cloud Storage API

---

## ✅ Fix #3: Verify SHA-1 Certificate Match

### Your Configuration:
```
Package: com.ssbmax
SHA-1: bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:d5
```

### Verify in google-services.json:
```json
"certificate_hash": "bd9b85fe9380305eea621cc35182ab959f66ec05"
```

**Note**: The hash is the same but without colons!
- Console format: `bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:05`
- JSON format: `bd9b85fe9380305eea621cc35182ab959f66ec05`

✅ **Matches!** (without colons)

---

## ✅ Fix #4: Wait for API Restriction Propagation

**Timeline**:
- API restrictions applied: ~Oct 22, 08:30
- Propagation time: **5-10 minutes**
- Should be working by: Oct 22, 08:40

If auth still fails:
- Wait 10 minutes
- Rebuild and reinstall app
- Try authentication again

---

## 🔍 Common Error Messages

### Error: "API key not valid"
**Cause**: API restrictions not propagated yet OR Identity Toolkit API not enabled  
**Fix**: Wait 10 minutes, verify Identity Toolkit API is in the allowed list

### Error: "The application signature is not valid"
**Cause**: SHA-1 fingerprint doesn't match  
**Fix**: Verify SHA-1 in Google Cloud Console matches Firebase Console

### Error: "Web Client ID not found"
**Cause**: google-services.json not processed correctly  
**Fix**: Clean and rebuild project

### Error: "Google Sign-In failed: 12500"
**Cause**: API key restrictions or SHA-1 mismatch  
**Fix**: 
1. Check Identity Toolkit API is enabled
2. Verify SHA-1 fingerprint
3. Wait for propagation

### Error: "Firebase authentication failed"
**Cause**: Identity Toolkit API not in allowed APIs  
**Fix**: Add Identity Toolkit API to API restrictions

---

## 🧪 Testing Authentication

### Step-by-Step Test:

1. **Clean and Rebuild**:
   ```bash
   cd /Users/sunil/Downloads/SSBMax
   ./gradle.sh clean
   ./gradle.sh assembleDebug
   ```

2. **Install Fresh APK**:
   ```bash
   ./gradle.sh installDebug
   ```

3. **Test Google Sign-In**:
   - Launch app
   - Click "Sign in with Google"
   - Select Google account
   - Wait for result

4. **Check Logs** (if available):
   ```bash
   adb logcat | grep -i "firebase\|auth\|google"
   ```

---

## 📋 Checklist for Auth to Work

### Code:
- [ ] Fix syntax error in FirebaseAuthService.kt (line 96)
- [ ] Rebuild project after fix

### API Keys:
- [ ] Identity Toolkit API enabled in restrictions
- [ ] Token Service API enabled
- [ ] API restrictions saved

### Configuration:
- [ ] Package name is `com.ssbmax`
- [ ] SHA-1 in Google Cloud matches Firebase
- [ ] google-services.json is the NEW rotated version
- [ ] Waited 10 minutes after applying restrictions

### Firebase Console:
- [ ] Google Sign-In enabled in Authentication section
- [ ] OAuth web client ID configured
- [ ] SHA-1 fingerprint added to app

---

## 🎯 Most Likely Cause

Based on timing and symptoms, the auth error is likely due to:

### Primary Suspect: Code Syntax Error ⭐
The missing `return try {` in `FirebaseAuthService.kt` line 96 will cause compilation or runtime errors.

### Secondary Suspect: API Propagation Delay ⏱️
API restrictions take 5-10 minutes to propagate. If you tested immediately after applying restrictions, wait a bit.

---

## 🔧 Quick Fix Steps

### 1. Fix the Code (CRITICAL):
```kotlin
// File: core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirebaseAuthService.kt
// Line 95-96

// OLD (BROKEN):
suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> {
    
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

// NEW (FIXED):
suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> {
    return try {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
```

### 2. Rebuild:
```bash
./gradle.sh clean assembleDebug
```

### 3. Install:
```bash
./gradle.sh installDebug
```

### 4. Wait 10 Minutes (if needed):
If API restrictions were just applied, wait for propagation.

### 5. Test Again:
Try Google Sign-In.

---

## 🆘 If Still Not Working

### Check These in Order:

1. **Build Logs**: Look for compilation errors related to FirebaseAuthService
2. **Firebase Console**: Verify Google Sign-In is enabled
3. **Google Cloud Console**: Verify all 7 APIs are in restrictions list
4. **SHA-1**: Double-check fingerprint matches everywhere
5. **Wait**: Full propagation can take up to 15 minutes

### Enable Firebase Authentication:

If not already enabled:
1. Firebase Console → Build → Authentication
2. Click "Get Started"
3. Sign-in method → Google → Enable
4. Save

---

## 📊 Expected Behavior After Fix

✅ Google Sign-In button works  
✅ Account picker appears  
✅ Authentication completes  
✅ User redirected to role selection or home  
✅ No API key errors  
✅ No authentication failed errors  

---

## 💡 Prevention

To avoid auth errors in future:

1. Always test auth after API key changes
2. Wait 10 minutes after applying API restrictions before testing
3. Keep Identity Toolkit API in allowed list
4. Verify SHA-1 when changing keystores
5. Test with clean install after google-services.json changes

---

**Next Step**: Fix the syntax error in FirebaseAuthService.kt and rebuild!

