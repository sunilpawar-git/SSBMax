# 🔍 How to Find Your API Keys in Google Cloud Console

## Current Location (Your Screenshot)
You're currently on: **APIs & Services** → **Enabled APIs & services**

This page shows which APIs are enabled and their usage statistics.

---

## Where to Go: Credentials Page

### Method 1: Use Left Sidebar
1. Look at the left sidebar in Google Cloud Console
2. Find the section with:
   - ⚙️ **Enabled APIs & services** (where you are now)
   - 🔑 **Credentials** ← Click here!
   - 📚 Library
   - 🔐 OAuth consent screen
   - 📄 Page usage agreements

### Method 2: Direct URL
Go to: https://console.cloud.google.com/apis/credentials?project=ssbmax-49e68

---

## What You'll See on Credentials Page

The Credentials page has 3 main sections:

### 1. OAuth 2.0 Client IDs
```
These are for OAuth authentication (Google Sign-In, etc.)
Example:
- Web client (auto created by Google Service)
- Android client (com.ssbmax)
```

### 2. API Keys ← **THIS IS WHAT YOU NEED**
```
These are the keys to restrict!
Example names:
- "Android key (auto created by Firebase)"
- "Browser key (auto created by Firebase)"  
- "Server key (auto created by Firebase)"

Each key will have:
- Name
- Key (starts with AIza...)
- Created date
- Restrictions status
```

### 3. Service Accounts
```
These are for server-to-server authentication
Example:
- firebase-adminsdk@ssbmax-49e68.iam.gserviceaccount.com
```

---

## Step-by-Step from Your Current Screen

```
Current Screen: APIs & Services (Dashboard with traffic graphs)
              ↓
Click "Credentials" in left sidebar
              ↓
Credentials Page appears
              ↓
Scroll to "API Keys" section
              ↓
You'll see 1-3 API keys listed
              ↓
Click on each key name to edit restrictions
```

---

## What If You Don't See Any API Keys?

### Scenario 1: Firebase Auto-Created Them
Firebase usually auto-creates API keys when you add an Android app. They might be there but you need to scroll down on the Credentials page.

### Scenario 2: Keys Are in Different Section
Sometimes they appear under **OAuth 2.0 Client IDs** instead. Look there too.

### Scenario 3: No Keys Yet
If this is a new Firebase project, keys might not exist yet. That's okay - Firebase will create them when needed.

---

## Expected Result

On the Credentials page, under **API Keys** section, you should see something like:

```
API Keys
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

NAME                                    KEY                     CREATED         RESTRICTIONS
────────────────────────────────────────────────────────────────────────────────────────────
Android key (auto created by Firebase) AIzaSy...               Oct 17, 2025    None ⚠️
Browser key (auto created by Firebase) AIzaSy...               Oct 17, 2025    None ⚠️

                                                                        [+ CREATE CREDENTIALS]
```

**Note**: If you see "None" or "Unrestricted" in the Restrictions column, that's what we need to fix!

---

## Quick Action

**Direct link to your project's credentials:**
https://console.cloud.google.com/apis/credentials?project=ssbmax-49e68

Click that link and you should go directly to the Credentials page.

---

## What to Do Once You Find the Keys

For EACH API key you see:

1. Click on the **key name** (not the key value)
2. You'll see an edit page with:
   - Application restrictions
   - API restrictions
3. Follow the instructions in `CHECK_API_RESTRICTIONS.md`

---

## Still Can't Find Keys?

If you navigate to Credentials and still don't see any API keys:

### Check Firebase Console Instead:
1. Go to Firebase Console: https://console.firebase.google.com/
2. Select project: **ssbmax**
3. Project Settings → General → Your apps
4. Scroll down to see Web API Key listed there

The Web API Key shown in Firebase is one of the keys that should also appear in Google Cloud Console.

---

**Summary**: From where you are, just click **"Credentials"** in the left sidebar!

