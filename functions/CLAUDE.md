# functions/CLAUDE.md — Firebase Cloud Functions

**Scope:** Node.js/TypeScript Firebase Cloud Functions for backend logic. This file specializes [claude.md](../claude.md) for the functions module—where backend computation and AI evaluation happens.

**Core Principle:** Cloud Functions are state-less, event-driven, and secure. Authentication checks FIRST. Firestore rules are SSOT for data access. Gemini API calls for evaluation only.

---

## Security: Authentication & Authorization (First Line of Defense)

**Pattern: Verify User Before Processing**
```typescript
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Callable function with user authentication
exports.analyzeInterviewResponse = functions.https.onCall(
  async (data, context) => {
    // 1. Check authentication
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }
    
    const userId = context.auth.uid;
    
    // 2. Verify user exists in Firestore (optional but recommended)
    const userDoc = await admin
      .firestore()
      .collection("users")
      .doc(userId)
      .get();
    
    if (!userDoc.exists) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "User document not found"
      );
    }
    
    // 3. Check subscription/permission (Firestore rules handle this too)
    const userTier = userDoc.data()?.subscriptionTier;
    if (userTier !== "PREMIUM") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Premium subscription required"
      );
    }
    
    // 4. Now safe to process
    const result = await processData(data, userId);
    return { success: true, result };
  }
);

// Admin function (only callable from admin SDK, not from client)
exports.batchProcessData = functions.https.onCall(async (data, context) => {
  // Only Cloud Functions can call this (used in batch scripts)
  return {};
});
```

**Error Codes:**
- `"unauthenticated"` — User not logged in
- `"permission-denied"` — User lacks permission
- `"failed-precondition"` — Resource not in expected state
- `"invalid-argument"` — Bad input
- `"internal"` — Server error

**Firestore Security Rules (SSOT for access control):**
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    
    // Interviews are premium-only
    match /interviews/{interviewId} {
      allow read: if request.auth.uid != null && 
                     get(/databases/$(database)/documents/users/$(request.auth.uid)).data.subscriptionTier == "PREMIUM";
      allow write: if request.auth.uid == resource.data.userId &&
                      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.subscriptionTier == "PREMIUM";
    }
  }
}
```

---

## Gemini AI Integration (for Evaluation)

**Pattern: Structured Prompt + JSON Response**
```typescript
import { GoogleGenerativeAI } from "@google/generative-ai";

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

export const evaluateInterviewResponse = functions.https.onCall(
  async (data, context) => {
    const { questionText, userResponse } = data;
    
    try {
      const model = genAI.getGenerativeModel({ model: "gemini-2.0-flash" });
      
      // Structured prompt for consistent JSON output
      const prompt = `You are an expert SSB interview evaluator. 
      
Question: ${questionText}
User Response: ${userResponse}

Provide evaluation in this exact JSON format:
{
  "score": 1-10,
  "strengths": ["strength1", "strength2"],
  "improvements": ["area1", "area2"],
  "comments": "Brief feedback"
}

IMPORTANT: Respond ONLY with valid JSON, no markdown or extra text.`;
      
      const response = await model.generateContent(prompt);
      const text = response.response.text();
      
      // Parse JSON (with error handling)
      const evaluation = JSON.parse(text);
      
      // Validate structure
      if (!evaluation.score || !evaluation.strengths || !evaluation.improvements) {
        throw new Error("Invalid evaluation structure");
      }
      
      // Store in Firestore
      await admin
        .firestore()
        .collection("interviews")
        .doc(context.auth!.uid)
        .collection("responses")
        .add({
          questionText,
          userResponse,
          evaluation,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          model: "gemini-2.0-flash"
        });
      
      return { success: true, evaluation };
    } catch (error) {
      console.error("Gemini API error:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to evaluate response"
      );
    }
  }
);
```

**Rate Limiting (Important for Gemini API quota):**
```typescript
import * as rateLimit from "express-rate-limit";

const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 30, // 30 requests per minute per IP
  message: "Too many requests, please try again later"
});

exports.evaluateResponse = functions.https.onRequest(limiter, (req, res) => {
  // Rate-limited endpoint
});
```

---

## Firestore Transactions (Atomic Operations)

**Pattern: Multiple Reads + Writes in One Transaction**
```typescript
export const submitTestResult = functions.https.onCall(async (data, context) => {
  const { testId, answers, timeSpent } = data;
  const userId = context.auth!.uid;
  
  const db = admin.firestore();
  
  // Atomic operation: verify + update in single transaction
  const result = await db.runTransaction(async (transaction) => {
    // 1. Read user subscription status
    const userRef = db.collection("users").doc(userId);
    const userDoc = await transaction.get(userRef);
    
    if (!userDoc.data()?.subscriptionTier) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Subscription required"
      );
    }
    
    // 2. Read test metadata
    const testRef = db.collection("tests").doc(testId);
    const testDoc = await transaction.get(testRef);
    
    if (!testDoc.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        "Test not found"
      );
    }
    
    // 3. Score answers (business logic)
    const score = scoreAnswers(answers, testDoc.data()!.correctAnswers);
    
    // 4. Update user stats + create result (atomic)
    const resultRef = db.collection("results").doc();
    
    transaction.update(userRef, {
      testsCompleted: admin.firestore.FieldValue.increment(1),
      lastTestDate: admin.firestore.FieldValue.serverTimestamp()
    });
    
    transaction.set(resultRef, {
      userId,
      testId,
      score,
      timeSpent,
      answers,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });
    
    return { resultId: resultRef.id, score };
  });
  
  return result;
});
```

**Why Transactions:**
- ✅ All-or-nothing: if one fails, all rollback
- ✅ No partial updates
- ✅ Automatic conflict resolution

---

## Firestore Batch Operations (Multiple Writes, Not Transactional)

**Pattern: Bulk Insert/Update (up to 500 documents)**
```typescript
export const batchUploadQuestions = functions.https.onCall(async (data) => {
  const { questions } = data; // Array of question objects
  
  const db = admin.firestore();
  const batch = db.batch();
  
  let count = 0;
  for (const question of questions) {
    const ref = db
      .collection("questions")
      .doc(question.id);
    
    batch.set(ref, {
      ...question,
      uploadedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    count++;
    
    // Firestore batch limit: 500 writes per batch
    if (count % 500 === 0) {
      await batch.commit();
      // Start new batch
    }
  }
  
  if (count % 500 !== 0) {
    await batch.commit();
  }
  
  return { uploaded: count };
});
```

---

## Environment Variables (Secrets Management)

**Local Development (.env):**
```bash
# .env (NOT committed to git)
GEMINI_API_KEY=your_gemini_key_here
SARVAM_API_KEY=your_sarvam_key_here
```

**Firebase Console (Production):**
```bash
# Set via Firebase CLI
firebase functions:config:set gemini.api_key="your_key_here"

# In code:
const apiKey = functions.config().gemini.api_key;
```

**Or via Firestore Security Rules (read from restricted collection):**
```typescript
// collections/config/keys (read-restricted)
const apiKey = await db.collection("config").doc("keys").get();
// Only server-side JS can read (rules block client)
```

---

## Error Handling & Logging

**Pattern:**
```typescript
exports.myFunction = functions.https.onCall(async (data, context) => {
  try {
    // Validation
    if (!data.required_field) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Missing required field"
      );
    }
    
    // Processing
    const result = await process(data);
    
    return { success: true, result };
  } catch (error) {
    // Logging
    console.error("Function error:", {
      userId: context.auth?.uid,
      function: "myFunction",
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined
    });
    
    // Error response
    if (error instanceof functions.https.HttpsError) {
      throw error; // Already formatted
    }
    
    throw new functions.https.HttpsError(
      "internal",
      "Internal server error"
    );
  }
});
```

---

## Testing Cloud Functions (Emulator)

**Setup:**
```bash
firebase emulators:start --only functions,firestore
```

**Test Script (Node.js):**
```typescript
import { initializeApp } from "firebase/app";
import { connectFunctionsEmulator, httpsCallable } from "firebase/functions";

const app = initializeApp(firebaseConfig);
const functions = getFunctions(app);
connectFunctionsEmulator(functions, "localhost", 5001);

const myFunction = httpsCallable(functions, "myFunction");
const result = await myFunction({ key: "value" });
console.log(result.data);
```

---

## Best Practices

1. **Fail Fast:** Validate input + auth FIRST
2. **Use Transactions:** For multiple related writes
3. **Log Everything:** userId, function name, errors
4. **Rate Limit:** Especially for expensive operations (Gemini calls)
5. **Cache When Possible:** Store results in Firestore, don't recalculate
6. **Monitor Cold Starts:** First invocation is slow (initialize on global scope)
7. **Use Scheduled Functions:** For batch processing (not triggered per-request)

---

## References

- **Root guidance:** [claude.md](../claude.md) (security principles)
- **Firestore patterns:** [core/data/remote/CLAUDE.md](../core/data/remote/CLAUDE.md)
- **AI integration:** [core/data/ai/CLAUDE.md](../core/data/ai/CLAUDE.md)
- **Firebase Admin SDK:** https://firebase.google.com/docs/reference/admin

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
