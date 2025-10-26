# 🎯 Firebase Console - Where to Add SHA-1

## 📋 **Quick Copy Your SHA-1**

```
BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

**← Copy this entire line**

---

## 🖼️ **Visual Navigation Guide**

### Step 1: Open Firebase Console

```
Browser → https://console.firebase.google.com
```

You'll see all your Firebase projects listed.

---

### Step 2: Select SSBMax Project

```
┌─────────────────────────────────────┐
│  Firebase Console                    │
├─────────────────────────────────────┤
│                                      │
│  📦 SSBMax                    ← Click this
│     Android • iOS                    │
│                                      │
│  📦 Other Project                    │
│     Web                              │
│                                      │
└─────────────────────────────────────┘
```

---

### Step 3: Click Settings Gear Icon

```
┌─────────────────────────────────────┐
│  ⚙️ Project Overview    👤  ⚡        │  ← Click the ⚙️ gear icon
├─────────────────────────────────────┤
│                                      │
│  🏠 Project Overview                 │
│  🔥 Firestore Database               │
│  🔐 Authentication                   │
│  📊 Analytics                        │
│                                      │
└─────────────────────────────────────┘

Click ⚙️ → Select "Project settings"
```

---

### Step 4: Scroll to "Your apps" Section

```
┌─────────────────────────────────────┐
│  Project settings                    │
├─────────────────────────────────────┤
│                                      │
│  General  |  Integrations  |  etc.  │
│                                      │
│  ─────────────────────────────────  │
│  Your apps                           │
│  ─────────────────────────────────  │
│                                      │
│  🤖 SSBMax                           │  ← Your Android app
│     Package name: com.ssbmax         │
│                                      │
└─────────────────────────────────────┘
```

---

### Step 5: Scroll Down in Your App Card

```
┌─────────────────────────────────────┐
│  🤖 SSBMax                           │
│     Package name: com.ssbmax         │
│                                      │
│  App nickname: SSBMax                │
│  Debug signing certificate SHA-1     │
│                                      │
│  📥 google-services.json             │
│     [Download google-services.json]  │
│                                      │
│  🔒 SHA certificate fingerprints     │  ← Scroll to here
│     [Add fingerprint]                │
│                                      │
└─────────────────────────────────────┘
```

---

### Step 6: Click "Add fingerprint"

```
┌─────────────────────────────────────┐
│  🔒 SHA certificate fingerprints     │
│                                      │
│  SHA-1                               │
│  ┌─────────────────────────────┐    │
│  │ [Enter SHA-1 here]          │    │  ← Click in this box
│  └─────────────────────────────┘    │
│                                      │
│  or                                  │
│                                      │
│  SHA-256                             │
│  ┌─────────────────────────────┐    │
│  │                             │    │
│  └─────────────────────────────┘    │
│                                      │
└─────────────────────────────────────┘
```

---

### Step 7: Paste Your SHA-1

```
┌─────────────────────────────────────┐
│  🔒 SHA certificate fingerprints     │
│                                      │
│  SHA-1                               │
│  ┌─────────────────────────────┐    │
│  │ BD:9B:85:FE:93:80:30:5E:... │    │  ← Paste here
│  └─────────────────────────────┘    │
│                                      │
│         [Save] or press Enter        │
│                                      │
└─────────────────────────────────────┘
```

**Paste exactly:**
```
BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

---

### Step 8: Verify It Was Added

After saving, you should see:

```
┌─────────────────────────────────────┐
│  🔒 SHA certificate fingerprints     │
│                                      │
│  ✅ BD:9B:85:FE:93:80:30:5E:EA:6... │  ← Your SHA-1 listed
│                                      │
│  [Add fingerprint]                   │  ← Can add more if needed
│                                      │
└─────────────────────────────────────┘
```

---

## 🔐 **Enable Google Sign-In**

### Go to Authentication

```
Left Sidebar:
┌─────────────────────┐
│  🏠 Project Overview │
│  🔥 Firestore       │
│  🔐 Authentication  │  ← Click this
│  📊 Analytics       │
└─────────────────────┘
```

### Click "Sign-in method" Tab

```
┌─────────────────────────────────────┐
│  Users | Sign-in method | Templates │
│  ────    ──────────────    ────────  │
│                                      │
└─────────────────────────────────────┘
                  ↑
            Click this tab
```

### Enable Google Provider

```
┌─────────────────────────────────────┐
│  Sign-in providers                   │
├─────────────────────────────────────┤
│                                      │
│  🔵 Google              [Enabled]    │  ← Should say Enabled
│                                      │
│  📧 Email/Password      Disabled     │
│                                      │
│  📱 Phone               Disabled     │
│                                      │
└─────────────────────────────────────┘
```

If it says **"Disabled"**:

1. Click on **"Google"**
2. Toggle **Enable** switch to ON
3. Set **Public-facing name**: `SSBMax`
4. Set **Support email**: (your email)
5. Click **Save**

---

## 📥 **Download google-services.json**

### Back to Project Settings

```
⚙️ Project Settings → Your apps → SSBMax
```

### Find google-services.json Section

```
┌─────────────────────────────────────┐
│  🤖 SSBMax                           │
│                                      │
│  📥 google-services.json             │
│     Your app configuration file      │
│                                      │
│     [Download google-services.json]  │  ← Click this
│                                      │
└─────────────────────────────────────┘
```

### Save to Downloads Folder

```
Save dialog appears:
→ Save to: ~/Downloads/google-services.json
→ Click [Save]
```

---

## 🔄 **Update Your Project**

After downloading, run:

```bash
cd /Users/sunil/Downloads/SSBMax

# This will copy the file from Downloads to your project
./update_google_services.sh

# Or manually:
cp ~/Downloads/google-services.json app/google-services.json

# Clean and rebuild
./gradle.sh clean
./gradle.sh installDebug

# Clear app data
adb shell pm clear com.ssbmax
```

---

## ✅ **Verification Checklist**

Before testing, verify:

- [ ] **SHA-1 added** - Check Firebase Console shows your fingerprint
- [ ] **Google Sign-In enabled** - Authentication → Sign-in method → Google = Enabled
- [ ] **google-services.json downloaded** - File exists in ~/Downloads
- [ ] **Project file updated** - `app/google-services.json` replaced
- [ ] **App rebuilt** - Ran `./gradle.sh installDebug`
- [ ] **App data cleared** - Ran `adb shell pm clear com.ssbmax`

---

## 🎯 **Now Test!**

1. **Launch your app**
2. **Click "Continue with Google"**
3. **Google account picker should appear** ✅
4. **Select your account**
5. **Navigate to home screen** ✅

---

## 📸 **What You Should See**

### Before (Current):
- Click button
- Brief flash or nothing happens
- Back to login screen
- Logcat shows `resultCode=0`

### After (Fixed):
- Click button
- **Google account picker appears** (list of Google accounts)
- Select account
- Brief loading
- **Navigate to home screen**
- Logcat shows `resultCode=-1` (success!)

---

## 🆘 **Common Issues**

### "I don't see my Android app in Firebase"

**Solution**: Add your app first
1. Project Settings
2. "Add app" button (near top)
3. Click Android icon 🤖
4. Enter package name: `com.ssbmax`
5. Download `google-services.json`
6. Follow wizard
7. Then add SHA-1 as described above

### "Google provider is disabled and I can't enable it"

**Solution**: 
1. Make sure you're a project owner/admin
2. Check if billing is enabled (required for some features)
3. Contact the project owner to enable it

### "SHA-1 field is grayed out"

**Solution**:
1. Make sure you're in **Project Settings** → **Your apps** → Your Android app
2. Scroll to **"SHA certificate fingerprints"** section
3. Click **"Add fingerprint"** button (not the text field)
4. A new editable field should appear

---

## 💡 **Pro Tip**

**Bookmark this SHA-1** - You'll need it again when:
- Setting up release builds
- Enabling other Firebase features (Dynamic Links, etc.)
- Adding to Google Cloud Console for Maps API, etc.

```
Your Debug SHA-1:
BD:9B:85:FE:93:80:30:5E:EA:62:1C:C3:51:82:AB:95:9F:66:EC:05
```

Save it in a secure note or password manager! 🔐

