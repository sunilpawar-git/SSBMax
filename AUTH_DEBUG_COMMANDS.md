# 🚀 Quick Debug Commands for Authentication Issue

## Build and Install Updated App

```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh installDebug
```

## View Logs in Terminal (Alternative to Android Studio)

### Option 1: Filter Authentication Logs
```bash
adb logcat -s LoginScreen:D AuthViewModel:D AuthRepositoryImpl:D FirebaseAuthService:D SplashViewModel:D
```

### Option 2: View All App Logs
```bash
adb logcat | grep -E "LoginScreen|AuthViewModel|AuthRepositoryImpl|FirebaseAuthService|SplashViewModel"
```

### Option 3: Clear and Watch Fresh Logs
```bash
adb logcat -c  # Clear old logs
adb logcat -s LoginScreen:D AuthViewModel:D AuthRepositoryImpl:D FirebaseAuthService:D SplashViewModel:D
```

## Get SHA-1 Certificate

```bash
cd /Users/sunil/Downloads/SSBMax
./gradlew signingReport | grep SHA1
```

Sample output:
```
SHA1: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78
```

Copy this and add it to Firebase Console.

## Clear App Data (For Testing)

```bash
adb shell pm clear com.example.ssbmax
```

This will:
- Sign you out
- Clear all cached data
- Reset app to first-launch state
- Next launch will show login screen

## Uninstall and Reinstall

```bash
# Uninstall
adb uninstall com.example.ssbmax

# Build and install
./gradle.sh installDebug
```

## Check if App is Running

```bash
adb shell am force-stop com.example.ssbmax  # Stop app
adb shell am start -n com.example.ssbmax/.MainActivity  # Start app
```

## Firebase Emulator (If Using Local Testing)

```bash
# Start emulators
firebase emulators:start

# In another terminal, connect app to emulators
adb shell settings put global firebase_database_emulator_host 10.0.2.2:9000
```

## Verify Firebase Configuration

```bash
# Check if google-services.json exists
ls -la app/google-services.json

# View the file (check client_id)
cat app/google-services.json | grep "client_id"
```

## Complete Debug Session Commands

```bash
# 1. Navigate to project
cd /Users/sunil/Downloads/SSBMax

# 2. Clean build
./gradle.sh clean

# 3. Build and install
./gradle.sh installDebug

# 4. Clear app data
adb shell pm clear com.example.ssbmax

# 5. Clear logcat
adb logcat -c

# 6. Start watching logs
adb logcat -s LoginScreen:D AuthViewModel:D AuthRepositoryImpl:D FirebaseAuthService:D SplashViewModel:D

# 7. In another terminal or manually: Launch app and try signing in
```

## Logcat Error Keywords to Look For

```bash
# Search for specific errors
adb logcat | grep -i "exception"
adb logcat | grep -i "error"
adb logcat | grep -i "permission"
adb logcat | grep -i "denied"
adb logcat | grep -i "failed"
```

## Most Useful Single Command

```bash
cd /Users/sunil/Downloads/SSBMax && \
./gradle.sh installDebug && \
adb logcat -c && \
echo "✅ App installed. Now launch the app and try signing in." && \
echo "📋 Watching authentication logs..." && \
adb logcat -s LoginScreen:D AuthViewModel:D AuthRepositoryImpl:D FirebaseAuthService:D SplashViewModel:D
```

This will:
1. Build and install the app
2. Clear old logs
3. Start watching authentication logs
4. Show all relevant debug information

---

## 📱 Manual Testing Steps

After running the install command:

1. **Open the app** on your device/emulator
2. **Click "Continue with Google"**
3. **Select your Google account**
4. **Watch the terminal** - logs will appear in real-time
5. **Look for errors** - any line with "Error" or "Exception"

---

## 🔍 What to Look For in Logs

### ✅ Success Pattern
```
LoginScreen: Google Sign-In button clicked
LoginScreen: Launching Google Sign-In intent
LoginScreen: Google Sign-In result: resultCode=-1
AuthViewModel: handleGoogleSignInResult called
FirebaseAuthService: Google account obtained: user@gmail.com
FirebaseAuthService: Firebase authentication successful
AuthViewModel: Sign-in SUCCESS
LoginScreen: Navigating to home screen
```

### ❌ SHA-1 Error Pattern
```
LoginScreen: Google Sign-In button clicked
LoginScreen: Launching Google Sign-In intent
LoginScreen: Google Sign-In result: resultCode=-1
FirebaseAuthService: ApiException during sign-in: 10
AuthViewModel: Sign-in FAILED: Google Sign-In failed
```
**Fix**: Add SHA-1 certificate to Firebase

### ❌ Permission Error Pattern
```
FirebaseAuthService: Firebase authentication successful
AuthRepositoryImpl: Failed to load user: PERMISSION_DENIED
AuthViewModel: Sign-in FAILED: Failed to load/create user profile
```
**Fix**: Update Firestore security rules

### ❌ Cancelled Pattern
```
LoginScreen: Google Sign-In button clicked
LoginScreen: Launching Google Sign-In intent
LoginScreen: Google Sign-In result: resultCode=0
LoginScreen: User cancelled or error occurred
```
**Reason**: User pressed back button or Google Sign-In not configured

---

## 💡 Pro Tips

1. **Keep two terminals open**:
   - Terminal 1: Build and install
   - Terminal 2: Watch logs

2. **Use Android Studio Logcat if available**:
   - Better filtering and search
   - Color-coded by severity
   - Can save logs to file

3. **Test incrementally**:
   - Test 1: Clear data, first sign-in
   - Test 2: Close and relaunch (should skip login)
   - Test 3: Sign out, close, relaunch (should show login)

4. **Save logs to file**:
   ```bash
   adb logcat -s LoginScreen:D AuthViewModel:D > auth_debug.log
   ```
   Then share `auth_debug.log` for analysis

