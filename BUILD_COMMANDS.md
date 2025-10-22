# 🔧 SSBMax Build Commands

## ✅ Correct Way to Build (Use gradle.sh)

**Important**: Always use `./gradle.sh` instead of `./gradlew` to avoid JAVA_HOME issues.

---

## 🎯 Common Build Commands

### Clean Project
```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh clean
```

### Build Debug APK
```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh assembleDebug
```

### Build and Install on Device
```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh installDebug
```

### Build Release APK (Signed)
```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh assembleRelease
```

### Clean + Build
```bash
cd /Users/sunil/Downloads/SSBMax
./gradle.sh clean assembleDebug
```

---

## 🧪 Testing Commands

### Run Unit Tests
```bash
./gradle.sh test
```

### Run Android Instrumentation Tests
```bash
./gradle.sh connectedAndroidTest
```

### Run Specific Module Tests
```bash
./gradle.sh :core:domain:test
./gradle.sh :core:data:test
```

---

## 🔍 Analysis Commands

### Check for Lint Issues
```bash
./gradle.sh lint
```

### Dependency Report
```bash
./gradle.sh dependencies
```

### Build with Stack Traces (Debug Issues)
```bash
./gradle.sh assembleDebug --stacktrace
```

### Build with Info Logging
```bash
./gradle.sh assembleDebug --info
```

---

## 📱 Device/Emulator Commands

### List Connected Devices
```bash
adb devices
```

### Install Specific APK
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Uninstall App
```bash
adb uninstall com.ssbmax
```

### View Logs (Logcat)
```bash
adb logcat | grep SSBMax
```

---

## 🛠️ Why gradle.sh?

### The Problem:
```bash
# ❌ Direct gradlew has JAVA_HOME issues
./gradlew assembleDebug
# Error: JAVA_HOME is set to an invalid directory
```

### The Solution:
`gradle.sh` automatically sets the correct JAVA_HOME:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew "$@"
```

### The Result:
```bash
# ✅ gradle.sh works perfectly
./gradle.sh assembleDebug
# BUILD SUCCESSFUL in 16s
```

---

## 📦 Output Locations

### Debug APK:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK:
```
app/build/outputs/apk/release/app-release.apk
```

### Test Reports:
```
app/build/reports/tests/
core/domain/build/reports/tests/
```

### Lint Reports:
```
app/build/reports/lint-results.html
```

---

## ⚡ Quick Reference

| Task | Command |
|------|---------|
| **Clean** | `./gradle.sh clean` |
| **Build Debug** | `./gradle.sh assembleDebug` |
| **Install on Device** | `./gradle.sh installDebug` |
| **Run Tests** | `./gradle.sh test` |
| **Lint Check** | `./gradle.sh lint` |
| **Clean + Build** | `./gradle.sh clean assembleDebug` |

---

## 🔥 Common Issues

### Issue: Permission Denied
```bash
# Fix: Make gradle.sh executable
chmod +x gradle.sh
```

### Issue: Build Fails
```bash
# Solution: Clean and rebuild
./gradle.sh clean
./gradle.sh assembleDebug
```

### Issue: Outdated Dependencies
```bash
# Solution: Sync Gradle
./gradle.sh --refresh-dependencies
```

---

## 💡 Pro Tips

### 1. Build Faster with Daemon
```bash
# Daemon is enabled by default, keeps Gradle in memory
# Check daemon status:
./gradle.sh --status
```

### 2. Build Specific Variant
```bash
# Debug only
./gradle.sh assembleDebug

# Release only
./gradle.sh assembleRelease

# All variants
./gradle.sh assemble
```

### 3. Parallel Builds
```bash
# Enable parallel builds (already in gradle.properties)
./gradle.sh assembleDebug --parallel
```

### 4. Offline Mode
```bash
# Build without checking for updates
./gradle.sh assembleDebug --offline
```

---

## 🎯 Full Build + Install Flow

```bash
# 1. Navigate to project
cd /Users/sunil/Downloads/SSBMax

# 2. Clean previous build
./gradle.sh clean

# 3. Build fresh APK
./gradle.sh assembleDebug

# 4. Install on connected device
./gradle.sh installDebug

# Or all in one command:
./gradle.sh clean installDebug
```

---

## 📊 Build Performance

### Current Performance:
```
Clean build: ~16 seconds
Incremental build: ~5-8 seconds
APK size: 24 MB (debug)
```

### Optimize Build Speed:
1. ✅ Use `gradle.sh` (Gradle daemon)
2. ✅ Enable parallel builds (already configured)
3. ✅ Use incremental builds (don't clean unnecessarily)
4. ✅ Configure Gradle memory in `gradle.properties`

---

## ✅ Recommended Workflow

### Daily Development:
```bash
# Don't clean every time - use incremental builds
./gradle.sh assembleDebug
./gradle.sh installDebug
```

### After Major Changes:
```bash
# Clean build to avoid cache issues
./gradle.sh clean assembleDebug
```

### Before Testing:
```bash
# Build + Install + Run tests
./gradle.sh clean installDebug connectedAndroidTest
```

### Before Release:
```bash
# Build release APK (requires signing setup)
./gradle.sh clean assembleRelease
```

---

## 🚀 Remember

**Always use**: `./gradle.sh` ✅  
**Never use**: `./gradlew` directly ❌ (JAVA_HOME issues)

---

*Keep this file handy for quick build command reference!*

