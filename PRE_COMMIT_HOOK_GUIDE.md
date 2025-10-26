# 🔒 Pre-Commit Security Hook - Installation Guide

**Status**: ✅ **INSTALLED AND ACTIVE**  
**Location**: `.git/hooks/pre-commit`  
**Purpose**: Automatically prevent committing sensitive data

---

## ✅ What's Installed

A comprehensive security hook that automatically scans your commits for:

### 🔍 Security Checks Performed:

1. **Google/Firebase API Keys**
   - Pattern: `AIza[A-Za-z0-9_-]{35}`
   - Example: `AIzaSy***REDACTED***`

2. **OAuth 2.0 Client IDs**
   - Pattern: `[0-9]+-[a-z0-9]+.apps.googleusercontent.com`
   - Example: `836687498591-7t5p....apps.googleusercontent.com`

3. **Private Keys**
   - Pattern: `-----BEGIN PRIVATE KEY-----`
   - Blocks: RSA, EC, and other private key formats

4. **Generic API Keys**
   - Pattern: `api_key = "..."`
   - Blocks: Common API key variable patterns

5. **AWS Access Keys**
   - Pattern: `AKIA[0-9A-Z]{16}`
   - Blocks: AWS access key IDs

6. **AWS Secret Keys**
   - Pattern: `aws_secret_*_key`
   - Blocks: AWS secret key references

7. **Hardcoded Passwords**
   - Pattern: `password = "..."`
   - Blocks: Plain text password assignments

8. **Secret Tokens**
   - Pattern: `secret = "..."`
   - Blocks: Secret token assignments

9. **Bearer Tokens**
   - Pattern: `Bearer [token]`
   - Blocks: Authorization bearer tokens

10. **JWT Tokens**
    - Pattern: `eyJ...eyJ...`
    - Blocks: JSON Web Tokens

11. **google-services.json**
    - Blocks: Entire file (except test variants)
    - Critical: Contains Firebase configuration

12. **service-account.json**
    - Blocks: Any service account files
    - Critical: Contains admin private keys

---

## 🎯 How It Works

### Normal Commit (Clean):
```bash
$ git add myfile.kt
$ git commit -m "Add feature"
🔍 Running security checks...
✅ Security checks passed - No sensitive data detected
[main abc1234] Add feature
```

### Blocked Commit (Security Issue):
```bash
$ git add config.txt
$ git commit -m "Add config"
🔍 Running security checks...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ SECURITY ISSUE DETECTED - COMMIT BLOCKED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📁 File: config.txt
🔍 Issue: Google API Key detected
⚠️  Detected:
  apiKey = "AIzaSy***REDACTED***"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚫 COMMIT BLOCKED

Found 1 security issue(s) in your commit.

What to do:
  1. Remove sensitive data from the files
  2. Use environment variables or secure vaults instead
  3. Check .gitignore includes these files
  4. Run: git diff --cached to review your changes

To bypass this check (NOT RECOMMENDED):
  git commit --no-verify

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 📋 Usage Examples

### ✅ GOOD: Using Environment Variables
```kotlin
// Good - No secrets in code
class FirebaseConfig {
    val apiKey = BuildConfig.FIREBASE_API_KEY
    // Key stored in local.properties or environment
}
```

### ❌ BAD: Hardcoding Secrets (Blocked by Hook)
```kotlin
// Bad - Hook will block this commit
class FirebaseConfig {
    val apiKey = "AIzaSy***ACTUAL_KEY_HERE***"
}
```

---

## 🛠️ Managing the Hook

### Check if Hook is Active:
```bash
ls -la .git/hooks/pre-commit
```

### Test the Hook:
```bash
# Create test file with fake API key
echo 'test = "AIzaSy_TEST_KEY_32_CHARS_LONG_HERE"' > test.txt
git add test.txt
git commit -m "test"
# Should be blocked!
```

### Temporarily Bypass (Not Recommended):
```bash
git commit --no-verify -m "message"
```

### Disable the Hook:
```bash
chmod -x .git/hooks/pre-commit
```

### Re-enable the Hook:
```bash
chmod +x .git/hooks/pre-commit
```

### View Hook Contents:
```bash
cat .git/hooks/pre-commit
```

---

## 🔧 Customization

To add more security checks, edit `.git/hooks/pre-commit`:

```bash
nano .git/hooks/pre-commit
```

Example - Add check for Stripe keys:
```bash
# Check for Stripe keys
check_pattern "sk_live_[0-9A-Za-z]{24,}" "Stripe Secret Key detected" "$FILES"
```

---

## 📊 What Gets Checked

### ✅ Files Scanned:
- All source code (.kt, .java, .xml, etc.)
- Configuration files (.json, .yml, .properties)
- Documentation (.md, .txt)
- Scripts (.sh, .gradle)

### ⏭️ Files Skipped:
- Build artifacts (`app/build/*`, `build/*`)
- Binary files (`.apk`, `.aab`, `.jar`)
- Gradle cache (`.gradle/*`)
- Compiled classes (`.class`)
- Test variants of `google-services.json`

---

## 🚨 Common Issues

### Issue: Hook not running
**Solution**: Ensure it's executable
```bash
chmod +x .git/hooks/pre-commit
```

### Issue: False positive
**Solution**: Review and either:
1. Fix the pattern in the hook
2. Use `--no-verify` if truly safe (rare)

### Issue: Need to commit redacted examples
**Solution**: Use placeholders like:
```
apiKey = "AIzaSy***REDACTED***"
```

---

## 🎯 Best Practices

### DO:
✅ Keep actual secrets in:
   - Environment variables
   - `local.properties` (in .gitignore)
   - Secure vaults (AWS Secrets Manager, etc.)
   - CI/CD secret storage

✅ Use placeholders in documentation:
   ```
   API_KEY=your_key_here
   ```

✅ Keep `.gitignore` updated

### DON'T:
❌ Use `--no-verify` routinely
❌ Commit actual API keys
❌ Store secrets in version control
❌ Disable the hook permanently

---

## 📈 Hook Coverage

| Security Risk | Protected | Pattern |
|--------------|-----------|---------|
| Firebase API Keys | ✅ | `AIza...` |
| OAuth Client IDs | ✅ | `apps.googleusercontent.com` |
| Private Keys | ✅ | `BEGIN PRIVATE KEY` |
| AWS Keys | ✅ | `AKIA...` |
| Passwords | ✅ | `password = "..."` |
| Secrets/Tokens | ✅ | `secret = "..."` |
| Bearer Tokens | ✅ | `Bearer ...` |
| JWT Tokens | ✅ | `eyJ...` |
| google-services.json | ✅ | Filename match |
| service-account.json | ✅ | Filename match |

---

## 🔄 Sharing with Team

This hook is **local only** (not tracked by git). To share with your team:

### Option 1: Manual Installation
Share this file with team members:
```bash
cp .git/hooks/pre-commit ~/pre-commit-template
# Share pre-commit-template file
```

### Option 2: Project Script
Create `install-hooks.sh` in your repo:
```bash
#!/bin/bash
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
echo "✅ Pre-commit hook installed!"
```

Team members run:
```bash
./install-hooks.sh
```

---

## 📚 Additional Resources

- **Git Hooks Documentation**: https://git-scm.com/docs/githooks
- **Security Best Practices**: See `SECURITY_AUDIT_REPORT.md`
- **API Key Management**: See `URGENT_SECURITY_ACTION_REQUIRED.md`

---

## ✅ Installation Confirmation

**Hook Status**: 🟢 **ACTIVE**  
**Installed**: October 26, 2025  
**Checks**: 12 security patterns  
**Protection Level**: High

Your commits are now automatically protected against accidental secret leaks!

---

## 🆘 Support

If you encounter issues:

1. **Check hook is executable**: `ls -la .git/hooks/pre-commit`
2. **Test with dummy data**: See "Test the Hook" section above
3. **Review commit contents**: `git diff --cached`
4. **Check patterns**: `cat .git/hooks/pre-commit`

**Remember**: This hook is your first line of defense against accidental secret commits!

