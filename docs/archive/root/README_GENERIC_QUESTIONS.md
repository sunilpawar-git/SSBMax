# Generic Interview Questions - Upload Guide

This guide explains how to upload curated generic SSB interview questions to Firestore.

## Overview

The generic questions pool provides 25% of interview questions (in the 70/25/5 distribution):
- **70%** PIQ-based questions (personalized, generated from candidate's Personal Information Questionnaire)
- **25%** Generic questions (from this curated pool)
- **5%** Adaptive questions (based on previous responses)

## Files

1. **`generic_interview_questions.json`** - Contains 30 curated interview questions
2. **`upload_generic_questions.js`** - Node.js script to upload questions to Firestore
3. **`package.json`** - Dependencies (firebase-admin already included)

## Prerequisites

### 1. Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (`ssbmax-app`)
3. Click **Project Settings** → **Service Accounts** tab
4. Click **Generate New Private Key**
5. Save the downloaded file as `serviceAccountKey.json` in the project root

**⚠️ IMPORTANT: Never commit `serviceAccountKey.json` to git! It's already in `.gitignore`**

### 2. Install Dependencies

```bash
npm install
```

This installs `firebase-admin` (already in package.json).

## Upload Instructions

### Step 1: Review Questions

Open `generic_interview_questions.json` and review the 30 questions:

```json
{
  "questions": [
    {
      "id": "gen_001",
      "text": "Tell me about yourself and your family background.",
      "category": "Personal Background",
      "targetOLQs": ["POWER_OF_EXPRESSION", "SELF_CONFIDENCE", "SOCIAL_ADJUSTMENT"],
      "difficulty": 2,
      "expectedDuration": 180
    },
    // ... 29 more questions
  ],
  "metadata": {
    "totalQuestions": 30,
    "version": "1.0",
    ...
  }
}
```

**Quality Checklist:**
- ✅ All questions are SSB-appropriate
- ✅ Questions cover all 15 OLQs evenly (2 per OLQ on average)
- ✅ Difficulty ranges from 2-5 (medium to hard)
- ✅ Expected duration realistic (120-180 seconds)
- ✅ No duplicate or overlapping questions

### Step 2: Set Environment Variable

**Option A: Export (Temporary)**
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/Users/sunil/Downloads/SSBMax/serviceAccountKey.json"
```

**Option B: Add to ~/.zshrc (Permanent)**
```bash
echo 'export GOOGLE_APPLICATION_CREDENTIALS="/Users/sunil/Downloads/SSBMax/serviceAccountKey.json"' >> ~/.zshrc
source ~/.zshrc
```

### Step 3: Run Upload Script

```bash
node upload_generic_questions.js
```

**Expected Output:**

```
════════════════════════════════════════
🚀 Starting Generic Questions Upload
════════════════════════════════════════

✅ Firebase Admin SDK initialized
📂 Reading questions from: /Users/sunil/Downloads/SSBMax/generic_interview_questions.json
📋 Found 30 questions to upload
📊 Metadata: { totalQuestions: 30, version: '1.0', ... }
✅ Uploaded batch of 30 questions (Total: 30)

════════════════════════════════════════
✅ SUCCESS: Generic questions uploaded!
════════════════════════════════════════
📦 Total questions uploaded: 30
🗂️ Collection: generic_questions
🌐 Project: ssbmax-app

Next steps:
1. Verify questions in Firebase Console
2. Create Firestore index: generic_questions (difficulty ASC, usageCount ASC)
3. Test question retrieval in the app
```

### Step 4: Verify Upload in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/) → Firestore Database
2. Navigate to `generic_questions` collection
3. Verify all 30 documents exist with correct data
4. Check sample questions:
   - `gen_001`: "Tell me about yourself and your family background."
   - `gen_015`: "Tell me about a time when you went out of your way to help someone."
   - `gen_030`: "What steps have you taken to prepare yourself for SSB?"

### Step 5: Create Firestore Index (IMPORTANT)

For efficient question retrieval, create a composite index:

1. Go to Firebase Console → Firestore → **Indexes** tab
2. Click **Create Index**
3. Configure:
   - **Collection**: `generic_questions`
   - **Fields to index**:
     1. `difficulty` → Ascending
     2. `usageCount` → Ascending
4. Click **Create Index**
5. Wait for index to build (~1-2 minutes)

**Why this index?**
- Allows efficient queries like: "Get 5 unused questions of difficulty 3"
- Powers the 25% generic question distribution in interviews

## Question Structure

Each question document in Firestore has:

```typescript
{
  id: string;                  // e.g., "gen_001"
  text: string;                // The interview question
  category: string;            // e.g., "Leadership", "Teamwork"
  targetOLQs: string[];        // OLQs this question assesses
  difficulty: number;          // 2-5 (medium to hard)
  expectedDuration: number;    // Seconds (120-180)

  // Metadata (auto-added by script)
  source: "GENERIC_POOL";
  isPermanent: true;
  createdAt: Timestamp;
  updatedAt: Timestamp;
  usageCount: number;          // How many times used
  lastUsed: Timestamp | null;
  isActive: boolean;           // Can disable questions
}
```

## How Questions Are Used

### In StartInterviewViewModel.kt

```kotlin
// Fetch 5 generic questions (25% of 20 total questions)
val genericQuestions = questionCacheRepository.getGenericQuestions(
    count = 5,
    difficulty = 3,
    excludeRecentlyUsed = true
)
```

### In FirestoreInterviewRepository.kt

```kotlin
// Query generic questions
db.collection("generic_questions")
    .where("isActive", "==", true)
    .where("difficulty", "<=", 4)
    .orderBy("difficulty")
    .orderBy("usageCount")  // Prefer least-used questions
    .limit(count)
    .get()
```

## Troubleshooting

### Error: "GOOGLE_APPLICATION_CREDENTIALS not set"
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
```

### Error: "File not found: generic_interview_questions.json"
- Ensure you're running the script from project root
- Check file exists: `ls generic_interview_questions.json`

### Error: "Permission denied (Firestore)"
- Ensure service account has Firestore write permissions
- Check IAM permissions in Firebase Console

### Questions not appearing in app
1. Verify upload: Check Firebase Console
2. Check index: Ensure composite index built
3. Check repository code: `FirestoreInterviewRepository.kt`
4. Check logs: Filter for "generic_questions" in Logcat

## Updating Questions

To add/modify questions:

1. Edit `generic_interview_questions.json`
2. Update `metadata.version` (e.g., "1.0" → "1.1")
3. Run upload script again (will overwrite existing questions)
4. Test in app

## Distribution Verification

After upload, verify OLQ coverage:

```bash
# Count questions per OLQ (manual verification)
# Open generic_interview_questions.json and count targetOLQs
```

**Expected Distribution** (30 questions, targeting 15 OLQs):
- Each OLQ appears in ~4-6 questions (2 per OLQ on average)
- Some questions target multiple OLQs (multi-assessment)
- All 15 OLQs covered evenly

## Security Notes

🔒 **Service Account Key Security:**
- Never commit `serviceAccountKey.json` to version control
- Store securely (1Password, encrypted volume)
- Rotate regularly (every 90 days recommended)
- Limit permissions to minimum required (Firestore write only)

🔒 **Firestore Security Rules:**
```javascript
// Allow write only from server-side (service account)
match /generic_questions/{questionId} {
  allow read: if request.auth != null;  // Authenticated users can read
  allow write: if false;                // Only admin SDK can write
}
```

## Support

For issues:
1. Check Firebase Console → Firestore → Logs
2. Review script output for error details
3. Verify service account permissions
4. Contact team if persistent issues

---

**Last Updated**: 2025-01-26
**Version**: 1.0
**Maintained By**: SSBMax Development Team
