# Firebase Functions Setup Guide for SSBMax

This guide explains how to configure, deploy, and use Firebase Cloud Functions for secure Gemini API integration.

## ✅ What's Been Implemented

### 1. **Firebase Functions** (Server-Side)
- `analyzeInterviewResponse`: Analyzes user's interview responses using Gemini 2.5 Flash
- `generateInterviewQuestions`: Generates personalized questions based on PIQ data
- Location: `functions/src/index.js`

### 2. **Android Integration** (Client-Side)
- `CloudGeminiAIService`: Calls Firebase Functions securely
- `GeminiAIService`: Direct API calls (development only)
- Auto-switching: Debug builds use direct API, Release builds use Cloud Functions

---

## 🔐 Step 1: Configure Gemini API Key in Firebase

The API key must be stored securely in Firebase Functions environment (not in code).

### Option A: Using Firebase CLI (Recommended)

```bash
# Navigate to project root
cd /Users/sunil/Downloads/SSBMax

# Set the Gemini API key
firebase functions:config:set gemini.key="YOUR_GEMINI_API_KEY_HERE"

# Verify it's set
firebase functions:config:get

# Expected output:
# {
#   "gemini": {
#     "key": "YOUR_GEMINI_API_KEY_HERE"
#   }
# }
```

### Option B: Using Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to: **Functions** → **Configuration**
4. Add environment variable:
   - Key: `gemini.key`
   - Value: `YOUR_GEMINI_API_KEY_HERE`

---

## 🚀 Step 2: Deploy Firebase Functions

```bash
# Make sure you're in the project root
cd /Users/sunil/Downloads/SSBMax

# Deploy functions to Firebase
firebase deploy --only functions

# Expected output:
# ✔  functions: Finished running predeploy script.
# i  functions: ensuring required API cloudfunctions.googleapis.com is enabled...
# ✔  functions: required API cloudfunctions.googleapis.com is enabled
# i  functions: preparing functions directory for uploading...
# i  functions: packaged functions (X.XX KB) for uploading
# ✔  functions: functions folder uploaded successfully
# i  functions: updating Node.js 18 function analyzeInterviewResponse...
# i  functions: updating Node.js 18 function generateInterviewQuestions...
# ✔  functions[analyzeInterviewResponse]: Successful update operation.
# ✔  functions[generateInterviewQuestions]: Successful update operation.
#
# ✔  Deploy complete!
```

### Troubleshooting Deploy Issues

**Error: "Billing account not configured"**
```bash
# Firebase Functions requires Blaze plan (pay-as-you-go)
# Go to: https://console.firebase.google.com/project/_/usage/details
# Click "Modify plan" → Upgrade to Blaze Plan
# Don't worry: You get generous free tier, and costs are low (~$10-15/month for 1000 interviews)
```

**Error: "APIs not enabled"**
```bash
# Enable required APIs
firebase functions:config:set runtime="nodejs18"
```

---

## 🧪 Step 3: Test Firebase Functions Locally (Optional)

Before deploying to production, test functions locally using Firebase Emulators.

```bash
cd /Users/sunil/Downloads/SSBMax

# Start emulators (includes Functions, Firestore, Auth)
firebase emulators:start

# Expected output:
# ┌─────────────────────────────────────────────────────────────┐
# │ ✔  All emulators ready! It's now safe to connect your app. │
# │ i  View Emulator UI at http://0.0.0.0:4000                  │
# └─────────────────────────────────────────────────────────────┘
#
# ┌───────────┬────────────────┬─────────────────────────────────┐
# │ Emulator  │ Host:Port      │ View in Emulator UI             │
# ├───────────┼────────────────┼─────────────────────────────────┤
# │ Auth      │ 0.0.0.0:9099   │ http://0.0.0.0:4000/auth        │
# │ Functions │ 0.0.0.0:5001   │ http://0.0.0.0:4000/functions   │
# │ Firestore │ 0.0.0.0:8080   │ http://0.0.0.0:4000/firestore   │
# └───────────┴────────────────┴─────────────────────────────────┘
```

**Testing a Function:**

1. Open Emulator UI: http://localhost:4000
2. Go to **Functions** tab
3. Find `analyzeInterviewResponse`
4. Click **Test** and provide mock data:
   ```json
   {
     "responseId": "test-response-123",
     "sessionId": "test-session-123"
   }
   ```

---

## 📱 Step 4: Build & Test Android App

### Development Build (Uses Direct API)

```bash
cd /Users/sunil/Downloads/SSBMax

# Build debug APK
./gradle.sh :app:assembleDebug

# Result: app/build/outputs/apk/debug/app-debug.apk
# Uses: GeminiAIService (direct API calls)
# API Key: From local.properties
```

### Production Build (Uses Firebase Functions)

```bash
# Build release APK
./gradle.sh :app:assembleRelease

# Result: app/build/outputs/apk/release/app-release.apk
# Uses: CloudGeminiAIService (Firebase Functions)
# API Key: Stored securely in Firebase (NOT in APK)
```

### Verify Which Implementation is Used

Check the logs when analyzing a response:

**Development (Debug Build):**
```
D/GeminiAIService: Analyzing response with Gemini 2.5 Flash
D/GeminiAIService: API call completed in 2.3s
```

**Production (Release Build):**
```
D/CloudGeminiAI: Calling cloud function: analyzeInterviewResponse
D/CloudGeminiAI: Cloud function completed in 3.1s
```

---

## 🔍 Step 5: Monitor & Debug

### View Function Logs

```bash
# Stream live logs
firebase functions:log

# View recent logs
firebase functions:log --limit 50

# View logs for specific function
firebase functions:log --only analyzeInterviewResponse
```

### Firebase Console Logs

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to: **Functions** → **Logs**
4. Filter by function name or time range

### Common Log Messages

**Success:**
```
Analyzing response test-response-123 for user abc123xyz
Successfully analyzed response test-response-123
```

**Auth Error:**
```
Error: User must be authenticated to analyze responses
```

**Permission Error:**
```
Error: You do not have permission to access this session
```

**Gemini API Error:**
```
Error analyzing response: Failed to generate content from Gemini
```

---

## 📊 Step 6: Cost Monitoring

### Expected Costs (1000 interviews/month)

| Service | Usage | Cost |
|---------|-------|------|
| **Gemini API** | 1000 interviews @ 10K tokens each | ~$11.75/month |
| **Firebase Functions** | 2000 invocations (analyze + questions) | **FREE** (under 2M limit) |
| **Firestore** | Read/write operations | **FREE** (under quota) |
| **Total** | 1000 interviews | **~$11.75/month** |

### Set Up Billing Alerts

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to: **Billing** → **Budgets & alerts**
3. Click **Create Budget**
4. Set alert at: $20/month
5. Add email notification

---

## 🔧 Troubleshooting

### Function Deployment Fails

**Issue:** `firebase deploy` hangs or fails

**Solution:**
```bash
# Check Firebase CLI version
firebase --version  # Should be >= 13.0.0

# Update if needed
npm install -g firebase-tools

# Re-login
firebase login --reauth

# Try again
firebase deploy --only functions --debug
```

### Android App Can't Call Function

**Issue:** `FirebaseFunctionsException: NOT_FOUND`

**Solution:**
1. Verify function is deployed: `firebase functions:list`
2. Check function name matches: `analyzeInterviewResponse`
3. Ensure user is authenticated (Firebase Auth)
4. Check Firestore security rules allow access

### API Key Not Working in Functions

**Issue:** `Error: Gemini API key not configured`

**Solution:**
```bash
# Check if key is set
firebase functions:config:get

# If empty, set it again
firebase functions:config:set gemini.key="YOUR_KEY_HERE"

# Re-deploy
firebase deploy --only functions
```

### High Latency (>10 seconds)

**Issue:** Function calls are slow

**Solutions:**
1. **Cold Start**: First call after deployment takes longer (~5-10s)
   - Subsequent calls are faster (~2-3s)
2. **Region**: Deploy closer to users
   - Edit `functions/src/index.js`: `functions.region('asia-south1')`
3. **Timeout**: Increase if needed (currently 30s for analysis)

---

## 🎯 How It Works: Complete Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. User completes interview question                            │
│    - Stores response in Firestore                               │
│    - Collection: interview_responses/{responseId}                │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Android App calls CloudGeminiAIService.analyzeResponse()     │
│    - Passes: responseId, sessionId                              │
│    - Firebase Auth token automatically included                  │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Firebase Cloud Function: analyzeInterviewResponse            │
│    ✓ Verifies user is authenticated (Firebase Auth)             │
│    ✓ Checks user owns the session (Firestore query)             │
│    ✓ Fetches response from Firestore                            │
│    ✓ Calls Gemini 2.5 Flash API (API key secure on server)      │
│    ✓ Parses JSON response                                       │
│    ✓ Stores OLQ scores back to Firestore                        │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Returns analysis to Android App                              │
│    - OLQ scores: Map<OLQ, OLQScoreWithReasoning>                │
│    - Overall confidence: Int (0-100)                             │
│    - Key insights: List<String>                                  │
│    - Suggested follow-up: String?                                │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Android App displays results to user                         │
│    - Shows OLQ scores with reasoning                             │
│    - Displays insights and recommendations                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📝 Next Steps

### Immediate (Before Beta Launch):

1. ✅ Deploy functions: `firebase deploy --only functions`
2. ✅ Set API key: `firebase functions:config:set gemini.key="YOUR_KEY"`
3. ✅ Test with debug build (uses direct API)
4. ✅ Test with release build (uses Cloud Functions)
5. ✅ Monitor logs for errors
6. ✅ Set up billing alerts

### Future Enhancements:

1. **Batch Processing**: Pre-generate questions overnight to reduce latency
2. **Caching**: Cache question sets to avoid regeneration
3. **Rate Limiting**: Implement per-user rate limits
4. **A/B Testing**: Test different prompts for better results
5. **Analytics**: Track which questions users struggle with

---

## 📞 Support

- **Firebase Functions Docs**: https://firebase.google.com/docs/functions
- **Gemini API Docs**: https://ai.google.dev/gemini-api/docs
- **Firebase Console**: https://console.firebase.google.com/

---

## ✅ Checklist: Is Everything Working?

- [ ] Firebase Functions deployed successfully
- [ ] API key configured in Firebase environment
- [ ] Debug build uses GeminiAIService (check logs)
- [ ] Release build uses CloudGeminiAIService (check logs)
- [ ] User can complete interview and get OLQ scores
- [ ] Logs show no authentication errors
- [ ] Costs are within expected range ($10-15/month)
- [ ] Billing alerts configured

**All checked?** 🎉 You're production-ready!
