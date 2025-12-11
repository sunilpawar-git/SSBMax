# Security Documentation for SSBMax

## 🔒 Overview

This document describes the security measures implemented in SSBMax to protect API keys and sensitive data.

## 🎯 Security Goals

1. **Never expose API keys in source code**
2. **Never commit sensitive files to version control**
3. **Validate security before each commit**
4. **Detect security issues in CI/CD pipeline**
5. **Use server-side API calls for production builds**

## 🛡️ Security Architecture

### API Key Management

#### Development (Debug Builds)
- API key stored in `local.properties` (git-ignored)
- Direct API calls via `GeminiAIService`
- For local testing only, never distributed

#### Production (Release Builds)
- API key stored in Firebase Functions environment
- Client calls Firebase Functions via `CloudGeminiAIService`
- API key never exposed to client apps

### File Protection

The following files are automatically excluded from version control:

```gitignore
# Android local config
local.properties

# Firebase Functions environment
functions/.env
functions/.env.local
.env

# Firebase service credentials
google-services.json
service-account.json
firebase-admin-key.json
```

## 🔧 Security Tools

### 1. Git Hooks (Local Protection)

**Installation:**
```bash
./scripts/setup-git-hooks.sh
```

**Hooks installed:**

#### `pre-commit`
Runs before each commit to validate:
- ✅ No API keys in staged files
- ✅ No sensitive files being added
- ✅ No hardcoded secrets in code
- ✅ No *Holder.kt anti-pattern files

#### `commit-msg`
Runs after commit message is written to validate:
- ✅ No API keys in commit message
- ⚠️ Warning for potential secrets in message

**Bypass hooks (NOT RECOMMENDED):**
```bash
git commit --no-verify
```

### 2. Manual Security Validation

Run comprehensive security audit anytime:

```bash
./scripts/validate-security.sh
```

This script checks:
1. API keys in tracked files
2. Sensitive files in repository
3. .gitignore configuration
4. Local configuration files
5. Hardcoded secrets in code
6. Anti-pattern files (*Holder.kt)
7. Git hooks installation
8. Firebase Functions configuration

### 3. CI/CD Pipeline (GitHub Actions)

**Workflow: `.github/workflows/security-check.yml`**

Runs on every push and pull request:
- ✅ Detect hardcoded API keys
- ✅ Verify sensitive files not tracked
- ✅ Check for hardcoded secrets
- ✅ Validate .gitignore coverage
- ✅ Scan commit messages

**Workflow: `.github/workflows/android-ci.yml`**

Builds and tests the app:
- ✅ Lint checks (includes custom security rules)
- ✅ Unit tests
- ✅ Build debug APK
- Uses test API key for CI builds

## 📋 Security Checklist

### For Developers

Before committing:
- [ ] No API keys in code
- [ ] Sensitive files in .gitignore
- [ ] Git hooks installed (`./scripts/setup-git-hooks.sh`)
- [ ] Run `./scripts/validate-security.sh`

Before pushing:
- [ ] All tests pass
- [ ] CI/CD checks pass
- [ ] No sensitive data in commit messages

Before releasing:
- [ ] Production uses CloudGeminiAIService
- [ ] API key stored in Firebase Functions
- [ ] No hardcoded secrets in APK
- [ ] Security scan passed

### For New Team Members

1. **Clone repository**
   ```bash
   git clone <repository-url>
   cd SSBMax
   ```

2. **Install Git hooks**
   ```bash
   ./scripts/setup-git-hooks.sh
   ```

3. **Create local.properties**
   ```bash
   echo "sdk.dir=/path/to/android/sdk" > local.properties
   echo "GEMINI_API_KEY=your_dev_key_here" >> local.properties
   ```

4. **Create functions/.env**
   ```bash
   cp functions/.env.example functions/.env
   # Edit functions/.env and add your API key
   ```

5. **Run security validation**
   ```bash
   ./scripts/validate-security.sh
   ```

## 🚨 What to Do If API Key is Exposed

### If key is in uncommitted files:
1. Remove the key from the file
2. Replace with placeholder
3. Commit the fix

### If key is committed to git:
1. **IMMEDIATELY rotate the API key**
   - Go to: https://makersuite.google.com/app/apikey
   - Delete the exposed key
   - Generate a new key

2. **Remove from git history**
   ```bash
   # Use git-filter-repo or BFG Repo-Cleaner
   git filter-repo --replace-text <(echo "OLD_KEY==>REDACTED")
   ```

3. **Update Firebase Functions config**
   ```bash
   firebase functions:config:set gemini.key="NEW_KEY"
   firebase deploy --only functions
   ```

4. **Update local files**
   - Update `local.properties`
   - Update `functions/.env`

5. **Force push (if needed)**
   ```bash
   git push --force --all
   ```

6. **Notify team members**
   - Ask them to delete their local copies
   - Fresh clone after cleanup

### If key is pushed to public repository:
1. **Consider the key COMPROMISED**
2. **Rotate immediately** (see steps above)
3. **Check API usage logs** for unauthorized access
4. **Set up billing alerts** to detect abuse
5. **Enable API key restrictions**:
   - Go to Google Cloud Console
   - Set allowed applications/IPs
   - Set API restrictions

## 🔐 API Key Best Practices

### For Google AI API Keys

1. **Restrict by Application**
   - Android app: Restrict by package name + SHA-1
   - Firebase Functions: Restrict by server IP (if static)

2. **Restrict by API**
   - Only enable "Generative Language API"
   - Disable unnecessary APIs

3. **Set Quotas**
   - Limit requests per day
   - Limit requests per minute
   - Set up billing alerts

4. **Monitor Usage**
   - Review API usage regularly
   - Set up alerts for unusual activity
   - Check logs for unauthorized access

## 📚 Related Documentation

- [Firebase Functions Setup](FIREBASE_FUNCTIONS_SETUP.md)
- [Architecture Guidelines](CLAUDE.md)
- [CI/CD Configuration](.github/workflows/)

## 📞 Security Contact

If you discover a security vulnerability:
1. **DO NOT** create a public GitHub issue
2. Email: [your-security-email@example.com]
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## ✅ Security Audit Log

| Date | Auditor | Status | Issues Found | Actions Taken |
|------|---------|--------|--------------|---------------|
| 2025-01-XX | AI Assistant | ✅ Pass | 0 critical, 1 info | Sanitized documentation |

---

**Last Updated:** 2025-01-XX
**Security Version:** 1.0
**Next Review:** 2025-XX-XX
