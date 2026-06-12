# SECURITY.md — SSBMax Security Standards

**Purpose:** Comprehensive security checklist and implementation guidelines for all phases.
**Version:** 1.0 | **Last Updated:** June 2026

---

## Threat Model

**Assets:** User authentication data, interview responses (PII), assessment scores, AI-generated feedback, API credentials.

**Threat Actors:** Attackers targeting user data breach, man-in-the-middle interception, privilege escalation via Firebase misconfig.

**Risks:** 
- Hardcoded API keys in repo → credential theft
- Weak Firestore rules → unauthorized data access
- User input injection → XSS/data corruption
- Unencrypted local storage → stolen data if device compromised

---

## Data Classification

| Data Type | Classification | Protection |
|---|---|---|
| User ID, email | PII | Encrypted at rest (if local); Firebase auth owns encryption |
| Interview responses | PII/SENSITIVE | Encrypted at rest; field-level Firestore rules |
| API keys (Gemini, Sarvam, ElevenLabs) | SECRET | `local.properties` (not versioned); never in code |
| Assessment scores, feedback | INTERNAL | Firestore rules: user owns their data, admin can read |
| Test questions (OIR, TAT, etc.) | INTERNAL | Public read (versioned, no PII); admin write only |

---

## Implementation Checklist (Per Phase)

### Phase 1: Documentation & Rules (NOW)
- ✅ `claude.md` updated with Security Principles
- ✅ `SECURITY.md` (this file) created
- [ ] Add security section to root `.gitignore` (ensure `local.properties`, `google-services.json` never committed)

**Action:** Verify `.gitignore` has:
```
local.properties
google-services.json
*.key
*.jks
.env*
```

### Phase 2: Lint Rules (Automated Enforcement)
**Goal:** Fail build if security violations detected.

**Detectors to implement in `lint/src/main/kotlin/`:**

1. **HardcodedApiKeyDetector.kt**
   - Pattern: Flags any string literal containing `GEMINI_API_KEY`, `SARVAM_API_KEY`, `ELEVENLABS_API_KEY`, `sk-`, `sk_`
   - Severity: ERROR (build fails)
   - Exception: None (use `local.properties` always)
   - Test: Create API key string in test code, verify lint error triggered

2. **SensitiveDataInLogsDetector.kt**
   - Pattern: Flags `Log.d()`, `println()`, `System.out.println()` containing userId, email, authToken, apiKey, password
   - Severity: ERROR (build fails)
   - Exception: Use ErrorLogger with sanitization
   - Test: Log `"User: ${user.id}"`, verify error; log `"Success"` passes

3. **UnsafeStringInputDetector.kt**
   - Pattern: User input (TextField.value, intent.getStringExtra) used directly without `.trim()`, `.isNotEmpty()`, or validation
   - Severity: WARN (build passes, but flagged)
   - Exception: Validated inputs only
   - Test: Create TextField without trim, verify warn

4. **FirebaseRulesLinterDetector.kt**
   - Pattern: Validates `firestore.rules` syntax at lint time (parse `.rules` file, detect mismatched braces, undefined functions)
   - Severity: ERROR (build fails)

### Phase 3: Pre-commit Hooks (Git-level Gates)
**File:** `scripts/git-hooks/pre-commit`

```bash
#!/bin/bash
set -e

echo "🔒 Running security pre-commit hooks..."

# 1. Check for hardcoded API keys
if grep -r "GEMINI_API_KEY\|SARVAM_API_KEY\|ELEVENLABS_API_KEY\|sk_\|sk-" \
  --include="*.kt" --include="*.java" app/ core/ lint/ 2>/dev/null; then
  echo "❌ ERROR: Hardcoded API key detected in staged files"
  exit 1
fi

# 2. Check for sensitive files (shouldn't be committed)
if git diff --cached --name-only | grep -E "local\.properties|google-services\.json|\.key|\.jks"; then
  echo "❌ ERROR: Sensitive file staged for commit (check .gitignore)"
  exit 1
fi

# 3. Validate Firestore rules syntax
if [ -f firestore.rules ]; then
  echo "Validating firestore.rules..."
  # Requires: firebase-tools installed (`npm i -g firebase-tools`)
  firebase deploy --only firestore:rules --dry-run 2>&1 | grep -q "error" && {
    echo "❌ ERROR: firestore.rules validation failed"
    exit 1
  } || true
fi

# 4. Run lint (all modules)
echo "Running lint checks..."
./gradlew lintDebug --no-daemon --quiet || exit 1

echo "✅ All security checks passed"
exit 0
```

**Install hook:**
```bash
chmod +x scripts/git-hooks/pre-commit
git config core.hooksPath scripts/git-hooks
```

### Phase 4: Firestore Security Rules (Data Access Control)
**File:** `firestore.rules`

**Principles:**
- User authentication required for all writes
- Users own their data (userId field matches auth.uid)
- Admin role can read/write all (Firebase custom claims)
- Subscriptions enforced (via SubscriptionManager)
- No public writes (admins only)

**Example rule:**
```
match /users/{uid} {
  allow read, write: if request.auth.uid == uid;
  allow read: if request.auth.token.admin == true;
}

match /interviews/{docId} {
  allow read, create: if request.auth != null && request.auth.uid == request.resource.data.userId;
  allow read, update: if request.auth.token.admin == true;
  allow delete: if false;  // Audit trail only
}
```

---

## Code Review Checklist (Security Focus)

**Before approving any PR, verify:**

- [ ] No API keys hardcoded (search for `sk_`, `GEMINI_API_KEY=`)
- [ ] All user-facing strings externalized to `strings.xml`
- [ ] User input validated & sanitized (trim, isNotEmpty, regex validation)
- [ ] Firestore/Firebase calls use repositories (not direct in UI)
- [ ] ErrorLogger used for exceptions (not printStackTrace)
- [ ] No secrets in log statements (grep PR for userId, authToken)
- [ ] New dependencies audited (check license, known vulns)
- [ ] Firestore rules updated if data model changed
- [ ] Tests include security scenarios (e.g., unauthorized access attempt)

---

## Incident Response

**If API key leaked:**
1. Immediately rotate key in Google Cloud Console
2. Check usage logs: `gcloud logging read "resource.type=api" | grep GEMINI`
3. Revoke leaked key if compromise suspected
4. Add commit hash to `.git-deny` (prevent accidental re-commit)

**If user data breach suspected:**
1. Check Firestore audit logs (enable: `gcloud logging enable firestore.googleapis.com`)
2. Verify Firestore rules didn't allow unauthorized read
3. Notify affected users (privacy policy compliance)

---

## Dependency Security

**Monthly audit:**
```bash
./gradlew dependencyCheckAnalyze
```

**Threshold:** All HIGH/CRITICAL vulns must have PR fixing in 2 weeks, or dependency removed.

---

## Encryption Key Rotation

**PII encryption (if implemented):**
- Keys stored in Firebase Remote Config (not in code)
- Rotation every 90 days
- Old keys kept for 1 week (allows decryption of old data)

---

## Reference Links

- Firebase Security Best Practices: https://firebase.google.com/docs/firestore/security/start
- OWASP Top 10 Mobile: https://owasp.org/www-project-mobile-top-10/
- Android Security Guide: https://developer.android.com/privacy-and-security
- Gemini API Security: https://ai.google.dev/docs/gemini_api_overview?hl=en#security
