# 🔒 Required APIs for SSBMax (Production)

## API Key Restrictions Configuration

When setting up API restrictions for production deployment, ensure these APIs are enabled and selected:

### ✅ **Core Firebase APIs**
1. **Identity Toolkit API** - Firebase Authentication
2. **Token Service API** - Authentication tokens
3. **Firebase Installations API** - App installations tracking
4. **Cloud Firestore API** - Database operations
5. **Cloud Storage API** - File storage (if using Firebase Storage)

### ✅ **Google Sign-In APIs**
6. **Google Identity Services API** - Google Sign-In authentication
   - Alternative names: "Google Sign-In API", "Google OAuth2 API"
   - Required for `resultCode=-1` (successful sign-in)

### ✅ **Push Notifications APIs** (if using FCM)
7. **Firebase Cloud Messaging API** - Push notifications
8. **FCM Registration API** - Device registration for notifications

---

## 🔧 How to Apply Restrictions

### For Android API Key:

1. **Google Cloud Console** → **APIs & Services** → **Credentials**
2. Click on **"Android key (auto created by Firebase)"**
3. Scroll to **"API restrictions"**
4. Select **"Restrict key"**
5. Choose **all APIs listed above**
6. Click **"Save"**
7. **Wait 5-10 minutes** before testing

---

## 🧪 Testing Checklist After Applying Restrictions

Test these features to ensure everything works:

- [ ] **Sign in with Google** - Should show account picker and sign in successfully
- [ ] **Auto-login** - Close and reopen app, should skip login screen
- [ ] **Sign out** - Should return to login screen
- [ ] **Firestore operations** - Read/write data works
- [ ] **Storage operations** - Upload/download files works (if applicable)
- [ ] **Push notifications** - Receive notifications (if applicable)

---

## ⚠️ Important Notes

### SHA-1 Certificates:
- **Debug SHA-1**: `BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05`
- **Release SHA-1**: Generate and add before Play Store release
  ```bash
  ./gradle.sh signingReport
  ```

### Multiple Environments:
- **Development**: Use "Don't restrict key" for faster iteration
- **Staging**: Use restricted keys to test production configuration
- **Production**: Always use restricted keys for security

### If Sign-In Breaks After Restriction:
1. Check if "Google Identity Services API" is enabled in API Library
2. Check if it's selected in the API key restrictions
3. Wait 10 minutes for propagation
4. Clear app data and test again

---

## 🔄 Quick Reference Commands

```bash
# Get current SHA-1
./gradle.sh signingReport | grep SHA1

# Clear app data
adb shell pm clear com.ssbmax

# Rebuild and test
./gradle.sh clean
./gradle.sh installDebug

# Watch auth logs
adb logcat -s FirebaseAuthService:D LoginScreen:D AuthViewModel:D
```

---

## 📅 Last Updated
- Date: October 26, 2025
- Working Configuration: "Don't restrict key" (Development)
- TODO: Add restrictions before Play Store release

