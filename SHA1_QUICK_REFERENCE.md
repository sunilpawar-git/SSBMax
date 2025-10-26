# 🔑 SHA-1 Quick Reference Card

## Your Debug SHA-1 Certificate

```
BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

---

## ⚡ Quick Fix (Copy & Run)

```bash
# 1. Add SHA-1 to Firebase Console (manual step - see below)
#    → https://console.firebase.google.com
#    → Project Settings → Your App → Add fingerprint
#    → Paste: BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05

# 2. Download new google-services.json from Firebase Console

# 3. Run these commands:
cd /Users/sunil/Downloads/SSBMax
./update_google_services.sh
./gradle.sh clean
./gradle.sh installDebug
adb shell pm clear com.ssbmax

# 4. Launch app and test sign-in - should work now! ✅
```

---

## 📍 Where to Add SHA-1

**Firebase Console**: https://console.firebase.google.com

```
1. Select SSBMax project
2. Click ⚙️ Settings → Project settings
3. Scroll to "Your apps" section
4. Find your Android app (com.ssbmax)
5. Scroll to "SHA certificate fingerprints"
6. Click "Add fingerprint"
7. Paste: BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
8. Press Enter or Save
```

---

## ✅ Checklist

- [ ] SHA-1 added to Firebase Console
- [ ] Google Sign-In enabled in Authentication tab
- [ ] Downloaded new google-services.json
- [ ] Replaced app/google-services.json in project
- [ ] Cleaned and rebuilt app
- [ ] Cleared app data
- [ ] Tested - Google account picker appears ✅

---

## 🎯 Expected Result

### Before Fix:
```
Click "Continue with Google"
→ resultCode=0 ❌
→ Nothing happens
```

### After Fix:
```
Click "Continue with Google"
→ Google account picker appears ✅
→ Select account
→ Navigate to home screen ✅
→ resultCode=-1 (success!)
```

---

## 📞 Need Help?

If still not working after adding SHA-1, run:

```bash
adb logcat -s LoginScreen:D AuthViewModel:D FirebaseAuthService:D
```

And share the new logs.

---

## 💾 Save This SHA-1

You'll need it for:
- Release builds
- Maps API
- Dynamic Links
- Other Firebase features

```
BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

**Keep it safe!** 🔐

