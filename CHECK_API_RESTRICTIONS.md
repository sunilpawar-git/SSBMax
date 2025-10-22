# 🔐 API Key Restriction Guide - Quick 15 Minute Task

## Why Restrict API Keys?

Even with fresh keys, **unrestricted** API keys can be used by anyone from any app if they get leaked. Restrictions ensure your keys ONLY work from your official app.

## ⏱️ Time Required: 15 minutes

---

## Step-by-Step Instructions

### 1. Go to Google Cloud Console
- URL: https://console.cloud.google.com/
- Select project: **ssbmax-49e68**

### 2. Navigate to API Credentials
- Click **APIs & Services** (hamburger menu on left)
- Click **Credentials**

### 3. Find Your API Keys
You should see several keys. For EACH key:

#### For Android API Keys:
```
✅ Application restrictions:
   - Select: "Android apps"
   - Package name: com.ssbmax
   - SHA-1 fingerprint: [Get from step 4 below]

✅ API restrictions:
   - Select: "Restrict key"
   - Enable only these:
     ☑ Firebase Authentication API
     ☑ Cloud Firestore API  
     ☑ Firebase Cloud Messaging API
     ☑ Firebase Installations API
     ☑ Token Service API
     (Uncheck everything else)

Click [Save]
```

#### For Web API Keys (if any):
```
✅ Application restrictions:
   - Select: "HTTP referrers"
   - Add referrers:
     - localhost:* (for testing)
     - Your domain (if you have a web version)

✅ API restrictions:
   - Select: "Restrict key"
   - Enable only APIs you use

Click [Save]
```

### 4. Get Your SHA-1 Fingerprint

#### Option A: From Terminal
```bash
cd /Users/sunil/Downloads/SSBMax

# For debug keystore
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

#### Option B: From Android Studio
1. Open your project in Android Studio
2. Click **Gradle** tab (right side)
3. Navigate to: **app** → **Tasks** → **android** → **signingReport**
4. Double-click **signingReport**
5. Copy the **SHA1** value from the output

#### Option C: From Firebase Console
1. Go to Firebase Console → Project Settings → General
2. Scroll to "Your apps" → "SSBMax Android"
3. Click "Add fingerprint"
4. The instructions there show how to get SHA-1

### 5. Verify Restrictions Applied

After saving, you should see:
- ✅ "Android apps" badge next to the key
- ✅ "Restricted" badge for API restrictions

---

## Quick Checklist

- [ ] Logged into Google Cloud Console
- [ ] Selected ssbmax-49e68 project
- [ ] Navigated to APIs & Services → Credentials
- [ ] Got SHA-1 fingerprint from debug keystore
- [ ] For each API key:
  - [ ] Set Application restriction to "Android apps"
  - [ ] Added package name: com.ssbmax
  - [ ] Added SHA-1 fingerprint
  - [ ] Set API restrictions to only needed APIs
  - [ ] Clicked Save
- [ ] Verified restrictions show "Android apps" and "Restricted" badges
- [ ] Tested app still works (build and run)

---

## What If I Get SHA-1 Later?

If you don't have your SHA-1 handy right now:

1. **Skip this for now** - your app is secure with new keys
2. **Do it later** when you have your keystore set up
3. **For now**: At minimum, set API restrictions (Step 3) even without SHA-1

---

## Testing After Restrictions

```bash
cd /Users/sunil/Downloads/SSBMax

# Clean build
./gradlew clean

# Build and run
./gradlew assembleDebug
```

Your app should work exactly the same. If it doesn't:
- Double-check package name matches: `com.ssbmax`
- Verify SHA-1 was added correctly
- Make sure all required APIs are enabled

---

## Expected Outcome

**Before**: Keys work from any app, any device, anywhere  
**After**: Keys ONLY work from apps with package `com.ssbmax` and your SHA-1

This is like putting a lock on your API keys! 🔐

---

## Priority

**High** - Do this within 24 hours for best security practice.

Even though you've rotated keys, restrictions are an essential security layer.

---

*This task complements the key rotation you already completed.*

