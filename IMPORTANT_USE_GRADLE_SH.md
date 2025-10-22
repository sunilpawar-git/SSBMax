# ⚠️ IMPORTANT: Always Use gradle.sh

## 🎯 Quick Rule

**✅ CORRECT**: `./gradle.sh assembleDebug`  
**❌ WRONG**: `./gradlew assembleDebug`

---

## 🤔 Why?

### The Problem with ./gradlew:
```bash
$ ./gradlew assembleDebug
ERROR: JAVA_HOME is set to an invalid directory: /Applications/Android

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation.
```

### The Solution with ./gradle.sh:
```bash
$ ./gradle.sh assembleDebug
BUILD SUCCESSFUL in 23s
163 actionable tasks: 143 executed, 20 up-to-date
```

---

## 📝 What gradle.sh Does

```bash
#!/bin/zsh
# Automatically sets correct JAVA_HOME
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Then runs gradlew with all your arguments
./gradlew "$@"
```

---

## ✅ Verified Commands (All Use gradle.sh)

```bash
# Clean
./gradle.sh clean

# Build
./gradle.sh assembleDebug

# Install
./gradle.sh installDebug

# Test
./gradle.sh test

# Clean + Build
./gradle.sh clean assembleDebug
```

---

## 🎉 Build Verification

**Latest Build**: October 22, 2025  
**Status**: ✅ BUILD SUCCESSFUL in 23s  
**APK**: ✅ Generated (24 MB)  
**Command Used**: `./gradle.sh assembleDebug`

---

## 📚 Updated Documentation

All documentation files now use `./gradle.sh`:
- ✅ BUILD_COMMANDS.md
- ✅ BUILD_SUCCESS_AFTER_SECURITY_FIX.md
- ✅ SECURITY_AND_BUILD_COMPLETE_SUMMARY.md
- ✅ QUICK_START.md

---

## 🔄 If You See ./gradlew in Docs

Replace it with `./gradle.sh`:

**Old**:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

**New**:
```bash
./gradle.sh assembleDebug
```

**Much simpler!** ✨

---

## 💡 Remember

Whenever you need to build, always start with:
```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh [command]
```

---

**This ensures builds always work!** ✅

