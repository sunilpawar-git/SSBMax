# 🔐 API Key Restriction - Step by Step Guide
## Using Your Actual Values from Firebase Console

---

## ✅ Information You Have (From Your Screenshot):

| Field | Value |
|-------|-------|
| **Package Name** | `com.ssbmax` |
| **SHA-1 Fingerprint** | `bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:d5` |
| **App ID** | `1:836687498591:android:cb34a48a03bd0b1ff3baea` |

---

## 🎯 Step-by-Step Instructions

### Step 1: Go to Google Cloud Console Credentials

**Option A - Direct Link:**
```
https://console.cloud.google.com/apis/credentials?project=ssbmax-49e68
```

**Option B - Navigate:**
1. Go to: https://console.cloud.google.com/
2. Select project: **ssbmax-49e68**
3. Click: **APIs & Services** (hamburger menu ☰)
4. Click: **Credentials**

---

### Step 2: Find Your API Keys

On the Credentials page, scroll to the **"API Keys"** section.

You should see 1-3 keys like:
- "Android key (auto created by Firebase)"
- "Browser key (auto created by Firebase)"
- "Server key (auto created by Firebase)" or similar

---

### Step 3: Edit EACH API Key

For **EACH** key you see:

#### Click on the Key Name

(Click the name, not the key value)

This opens the edit page for that key.

---

### Step 4: Set Application Restrictions

On the edit page, find **"Application restrictions"** section:

1. Select: ⭕ **Android apps** (radio button)

2. Click: **+ ADD AN ITEM**

3. Fill in:
   ```
   Package name: com.ssbmax
   SHA-1 certificate fingerprint: bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:d5
   ```

4. Click **DONE**

---

### Step 5: Set API Restrictions

Scroll down to **"API restrictions"** section:

1. Select: ⭕ **Restrict key** (radio button)

2. Click the dropdown: **Select APIs**

3. **Check ONLY these 6 APIs:**
   ```
   ☑ Cloud Firestore API
   ☑ Firebase Cloud Messaging API
   ☑ Firebase Installations API
   ☑ FCM Registration API
   ☑ Token Service API
   ☑ Identity Toolkit API
   ```

4. **Uncheck** everything else

---

### Step 6: Save

1. Scroll to the bottom
2. Click **SAVE**
3. Wait for confirmation message

---

### Step 7: Repeat for Other Keys

If you have multiple API keys (Android key, Browser key, etc.):

- **For Android keys**: Use the settings above
- **For Web/Browser keys**: 
  - Application restrictions: HTTP referrers (add your domain)
  - API restrictions: Same 6 APIs

---

## 📋 Quick Copy-Paste Values

### Package Name:
```
com.ssbmax
```

### SHA-1 Fingerprint:
```
bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:d5
```

### APIs to Enable (6 total):
```
Cloud Firestore API
Firebase Cloud Messaging API
Firebase Installations API
FCM Registration API
Token Service API
Identity Toolkit API
```

---

## ✅ Verification

After saving each key, you should see:

**Application restrictions:**
- Badge: "Android apps"
- Details: Shows package name and SHA-1

**API restrictions:**
- Badge: "Restricted"
- Details: "6 APIs"

---

## 🧪 Test Your App

After restricting all keys:

```bash
cd /Users/sunil/Downloads/SSBMax

# Clean build
./gradlew clean

# Build
./gradlew assembleDebug

# Install and run on device/emulator
./gradlew installDebug
```

Your app should work exactly the same as before!

---

## ⚠️ Troubleshooting

### If app doesn't work after restrictions:

1. **Check package name**: Must be exactly `com.ssbmax`
2. **Check SHA-1**: Must match what's in Firebase Console
3. **Check APIs**: All 6 required APIs must be enabled
4. **Wait 5 minutes**: Sometimes restrictions take a few minutes to propagate

### Common Issues:

**"API key not valid" error:**
- SHA-1 fingerprint doesn't match
- Package name is incorrect
- Forgot to add Identity Toolkit API

**"Permission denied" error:**
- Missing required API in restrictions
- API not enabled in project

**Google Sign-In fails:**
- Identity Toolkit API not enabled
- Token Service API not enabled
- SHA-1 doesn't match

---

## 🎯 What This Accomplishes

**Before restrictions:**
- Anyone with your API key can use it from any app
- No protection if keys leak again
- Unlimited usage from anywhere

**After restrictions:**
- Keys ONLY work from `com.ssbmax` app
- SHA-1 fingerprint must match your app's signing certificate
- Only specified APIs can be called
- Even if keys leak, they're useless to attackers! 🔒

---

## ⏱️ Time Estimate

- **1 API key**: 3-5 minutes
- **2-3 API keys**: 10-15 minutes total

Worth the time for the security benefit!

---

## 📝 Checklist

- [ ] Opened Google Cloud Console → Credentials
- [ ] Found API Keys section
- [ ] For each key:
  - [ ] Clicked key name to edit
  - [ ] Set Application restriction: "Android apps"
  - [ ] Added package name: `com.ssbmax`
  - [ ] Added SHA-1: `bd:9b:85:fe:93:80:30:5e:ea:62:1c:c3:51:82:ab:95:9f:66:ec:d5`
  - [ ] Set API restriction: "Restrict key"
  - [ ] Selected all 6 required APIs
  - [ ] Clicked Save
- [ ] Verified badges show "Android apps" and "Restricted"
- [ ] Tested app - confirmed it still works

---

## 🎉 After Completion

Once done, your API keys will be fully protected! 

Combined with:
- ✅ Removed from Git history
- ✅ Rotated keys
- ✅ Protected with .gitignore
- ✅ **Restricted to your app only** ← You're doing this now!

Your security posture is **excellent**! 🛡️

---

**Ready to start? Head to:** https://console.cloud.google.com/apis/credentials?project=ssbmax-49e68

