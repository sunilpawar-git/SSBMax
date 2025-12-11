# Firestore Security Rules for SSBMax

## How to Apply These Rules

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your SSBMax project
3. Navigate to **Firestore Database** → **Rules** tab
4. Copy the rules below and paste them (merge with existing rules)
5. Click **Publish** to apply

---

## Interview System Security Rules

Add these rules to your existing Firestore security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // ===== EXISTING RULES =====
    // Keep your existing authentication and other collection rules here

    // ===== INTERVIEW SYSTEM RULES (ADD THESE) =====

    // Interview sessions - users can create/read/update their own sessions
    match /interview_sessions/{sessionId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null && resource.data.userId == request.auth.uid;
    }

    // Interview questions - authenticated users can read/write questions
    // Questions are session-specific and stored temporarily during interview
    match /interview_questions/{questionId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }

    // Interview responses - users can write their own responses
    match /interview_responses/{responseId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow write: if request.auth != null && request.resource.data.userId == request.auth.uid;
    }

    // Interview results - users can read/write their own results
    match /interview_results/{resultId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow write: if request.auth != null && request.resource.data.userId == request.auth.uid;
    }

    // Question cache - authenticated users can read/write cached questions
    match /question_cache/{docId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }

    // Generic questions pool - read-only for users
    // Admins populate this via Firebase Console or admin script
    match /generic_questions/{questionId} {
      allow read: if request.auth != null;
      allow write: if false;  // Only admins can write via console
    }
  }
}
```

---

## Security Notes

### Authentication Required
All interview endpoints require `request.auth != null`, meaning users must be authenticated via Firebase Authentication.

### User Data Isolation
- Users can only read/write their own sessions, responses, and results
- The `userId` field is validated on create/update to match `request.auth.uid`

### Generic Questions
- The `generic_questions` collection is **read-only** for app users
- Populate this collection manually or via admin scripts in Firebase Console

### Question Caching
- The `question_cache` collection allows authenticated users to cache AI-generated questions
- This reduces API costs by storing PIQ-based questions for 30 days

---

## Testing Rules (Optional - Development Only)

For development/testing, you can temporarily use more permissive rules:

```javascript
// ⚠️ DEVELOPMENT ONLY - DO NOT USE IN PRODUCTION ⚠️
match /interview_sessions/{sessionId} {
  allow read, write: if request.auth != null;
}

match /interview_questions/{questionId} {
  allow read, write: if request.auth != null;
}

match /interview_responses/{responseId} {
  allow read, write: if request.auth != null;
}

match /interview_results/{resultId} {
  allow read, write: if request.auth != null;
}

match /question_cache/{docId} {
  allow read, write: if request.auth != null;
}

match /generic_questions/{questionId} {
  allow read, write: if request.auth != null;
}
```

**Important:** Replace with production rules (above) before going live!

---

## Verifying Rules Are Applied

After publishing, test by:
1. Starting an interview session in your app
2. Check logcat for:
   - ✅ No "PERMISSION_DENIED" errors
   - ✅ "Created interview session: {sessionId}"
   - ✅ Questions successfully stored

If you see errors, double-check:
- Rules are published (green "Published" badge in Firebase Console)
- User is authenticated (`request.auth != null` succeeds)
- Field names match your data model (`userId`, etc.)

---

## Collections Overview

| Collection | Purpose | User Access |
|------------|---------|-------------|
| `interview_sessions` | Active/completed interview sessions | Read/Write own |
| `interview_questions` | Questions for active sessions | Read/Write (temporary) |
| `interview_responses` | User responses to questions | Read/Write own |
| `interview_results` | Final OLQ assessment results | Read/Write own |
| `question_cache` | Cached AI-generated questions | Read/Write own |
| `generic_questions` | Pre-populated question pool | Read only |

---

**Last Updated:** 2025-11-22
**SSBMax Version:** Phase 6 - Interview System
