# 🚀 SSBMax Quick Start Guide

## ⚡ Essential Commands

### Build & Run
```bash
cd /Users/sunil/Downloads/SSBMax

# Clean build
./gradle.sh clean

# Build debug APK
./gradle.sh assembleDebug

# Install on device
./gradle.sh installDebug

# All in one
./gradle.sh clean installDebug
```

---

## 📁 Key Files

| File | Purpose |
|------|---------|
| `gradle.sh` | ✅ Use this to build (fixes JAVA_HOME) |
| `gradlew` | ❌ Don't use directly (has issues) |
| `app/google-services.json` | 🔒 Firebase config (not in Git) |
| `.firebase/service-account.json` | 🔒 Private key (not in Git) |

---

## 🔑 Security Status

✅ All API keys rotated and restricted  
✅ Git history cleaned (76 commits)  
✅ Files protected by .gitignore  
✅ Build verified and working  

---

## 📚 Documentation

- `BUILD_COMMANDS.md` - All build commands
- `BUILD_SUCCESS_AFTER_SECURITY_FIX.md` - Build verification
- `SECURITY_AND_BUILD_COMPLETE_SUMMARY.md` - Complete overview
- `API_RESTRICTION_STEP_BY_STEP.md` - API security setup

---

## 🎯 Quick Links

**Firebase Console**: https://console.firebase.google.com/  
**Google Cloud Console**: https://console.cloud.google.com/  
**GitHub**: https://github.com/sunilpawar-git/SSBMax

---

## ⚠️ Important Notes

1. **Always use `./gradle.sh`** - Never use `./gradlew` directly
2. **Never commit** `google-services.json` or `.firebase/` folder
3. **Package name**: `com.ssbmax`
4. **SHA-1**: `bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:d5`

---

## 💡 Most Used Commands

```bash
# Daily development
./gradle.sh assembleDebug

# Install on device
./gradle.sh installDebug

# After big changes
./gradle.sh clean assembleDebug

# Run tests
./gradle.sh test
```

---

**Project Status**: ✅ Ready for development!

