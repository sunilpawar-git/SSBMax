# Firebase Debug Variant Setup

## Current Status
The debug build variant requires Firebase configuration for `com.ssbmax.debug` package name.

## Steps to Enable Debug Variant with Separate Package

### 1. Firebase Console Configuration
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select SSBMax project
3. Navigate to Project Settings → General
4. Under "Your apps" section, click "Add app" → Android
5. Register app with package name: `com.ssbmax.debug`
6. Download the updated `google-services.json`

### 2. Update google-services.json
- The new `google-services.json` will include both:
  - `com.ssbmax` (release)
  - `com.ssbmax.debug` (debug)
- Replace `app/google-services.json` with the new file

### 3. Enable Debug Application ID Suffix
In `app/build.gradle.kts`, uncomment:
```kotlin
debug {
    isDebuggable = true
    applicationIdSuffix = ".debug"  // UNCOMMENT THIS
    versionNameSuffix = "-debug"
    ...
}
```

### 4. Test Installation
After changes:
```bash
./gradle.sh assembleDebug assembleRelease
adb install app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

Both apps should install side-by-side on the same device.

## Benefits
- Test debug and release builds simultaneously
- Separate Firebase projects/data for debugging
- No accidental production data modification
- Easier QA testing

## Current Workaround
Debug variant currently uses `com.ssbmax` package name until Firebase is updated.
