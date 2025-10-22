# 🚨 CRITICAL: Google API Key Security Fix

## Problem
GitHub detected exposed Google API keys in `app/google-services.json` (commit cd9bbb2c). This file was accidentally committed because it was commented out in `.gitignore`.

## Why This Happened
- Line 63 in `.gitignore` had `# google-services.json` (commented)
- Should have been `google-services.json` (uncommented)
- File was tracked by Git and pushed to remote repository

## ✅ Step 1: Prevent Future Commits (DONE)
- [x] Uncommented `google-services.json` in `.gitignore`

## 🔥 Step 2: Remove From Git History (CRITICAL - DO THIS NOW)

### Option A: Using git filter-repo (Recommended)
```bash
# Install git-filter-repo if not installed
# brew install git-filter-repo  # On macOS

# Remove the file from all commits
git filter-repo --path app/google-services.json --invert-paths --force

# Force push to remote (WARNING: This rewrites history)
git push origin --force --all
```

### Option B: Using BFG Repo-Cleaner (Alternative)
```bash
# Download BFG from https://rtyley.github.io/bfg-repo-cleaner/
java -jar bfg.jar --delete-files google-services.json

# Clean up
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# Force push
git push origin --force --all
```

### Option C: Manual removal (If file was only in recent commits)
```bash
# Remove from Git tracking but keep local file
git rm --cached app/google-services.json

# Commit the removal
git commit -m "Remove google-services.json from tracking"

# Push the change
git push origin main
```

⚠️ **WARNING**: Options A and B rewrite Git history. If others are working on this repo, coordinate with them first!

## 🔐 Step 3: Rotate ALL API Keys (DO THIS IMMEDIATELY)

### Firebase Console Steps:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your SSBMax project
3. Go to **Project Settings** (gear icon)
4. Navigate to **Service Accounts** tab
5. Click **"Generate New Private Key"**
6. Go to **General** tab
7. Under **Your apps**, delete and re-create your Android app OR regenerate keys

### Google Cloud Console Steps:
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Go to **APIs & Credentials** > **Credentials**
4. Find all API keys listed in your old `google-services.json`:
   - Android Key
   - Browser Key  
   - Cloud Messaging API Key
5. Click on each key → **Regenerate Key** or **Delete & Create New**

### Restrict Your New API Keys:
For each new API key:
1. Click on the key name
2. Under **Application restrictions**:
   - For Android keys: Set to "Android apps" and add your package name + SHA-1
   - For Web keys: Set to "HTTP referrers" and add your domains
3. Under **API restrictions**:
   - Select "Restrict key"
   - Only enable APIs you actually use (Firebase, FCM, etc.)
4. Save changes

## 📥 Step 4: Download Fresh google-services.json

1. In Firebase Console → Project Settings → General
2. Scroll to **Your apps** section
3. Click on your Android app
4. Click **"Download google-services.json"**
5. Replace the local file at `app/google-services.json`
6. **DO NOT** commit this new file (it's now in .gitignore)

## 🔍 Step 5: Verify Security

### Check Firebase Usage:
```bash
# In Firebase Console → Usage and billing
# Look for any suspicious activity from Oct 17 onwards
```

### Check Google Cloud Billing:
```bash
# In Google Cloud Console → Billing
# Check for unexpected API calls or charges
```

### Enable Firebase App Check:
1. Firebase Console → Build → App Check
2. Enable for Android app
3. Use Play Integrity or SafetyNet attestation

## 📝 Step 6: Add google-services.json to Local Setup Docs

Create a note for your team about obtaining the file:

```markdown
## Setting Up google-services.json

This file is not tracked in Git for security reasons.

To obtain it:
1. Contact project admin for access to Firebase Console
2. Download from Firebase Console → Project Settings → Your apps
3. Place at `app/google-services.json`
4. Never commit this file
```

## ✅ Verification Checklist

- [ ] `.gitignore` updated (google-services.json uncommented)
- [ ] File removed from Git history
- [ ] All API keys rotated in Firebase Console
- [ ] All API keys rotated in Google Cloud Console
- [ ] New API keys properly restricted
- [ ] Fresh google-services.json downloaded
- [ ] No suspicious activity in Firebase/GCloud usage
- [ ] Firebase App Check enabled
- [ ] Team notified (if applicable)
- [ ] GitHub secret scanning alert marked as resolved

## 🚀 Quick Command Summary

```bash
# 1. Verify .gitignore is updated
cat .gitignore | grep google-services.json

# 2. Remove from tracking (if using Option C)
git rm --cached app/google-services.json
git commit -m "Remove google-services.json from tracking"
git push

# 3. Verify file is ignored
git status  # Should not show google-services.json

# 4. After rotating keys, download fresh file
# Place at app/google-services.json
# File should NOT appear in git status
```

## 📚 Prevention for Future

### Add Pre-commit Hook:
Create `.git/hooks/pre-commit`:

```bash
#!/bin/bash
if git diff --cached --name-only | grep -q "google-services.json"; then
    echo "❌ ERROR: Attempting to commit google-services.json"
    echo "This file contains sensitive API keys and should not be committed."
    exit 1
fi
```

Make it executable:
```bash
chmod +x .git/hooks/pre-commit
```

### Regular Security Audits:
- Use `git secrets` or `gitleaks` to scan for secrets
- Enable GitHub secret scanning (already enabled)
- Review committed files before pushing

## 📞 Need Help?

If you're unsure about any step, especially removing from Git history, consider:
- Making the repository private temporarily
- Contacting GitHub support
- Consulting with your team lead

## ⏰ Timeline

- **Immediate** (within 1 hour): Rotate all API keys
- **Within 24 hours**: Remove file from Git history
- **Within 48 hours**: Verify no unauthorized usage

---

**Remember**: The damage is done once secrets are public. Act quickly but carefully!

