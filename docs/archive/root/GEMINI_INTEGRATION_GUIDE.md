# Gemini 2.5 Flash Integration Guide for SSBMax

**Date**: 2025-11-23
**Status**: Planning Phase
**Target Model**: `gemini-2.5-flash`

---

## 📋 Table of Contents

1. [Model Specifications](#model-specifications)
2. [Pricing & Rate Limits](#pricing--rate-limits)
3. [Android/Kotlin Integration](#androidkotlin-integration)
4. [Structured Output Strategy](#structured-output-strategy)
5. [Interview Feature Use Cases](#interview-feature-use-cases)
6. [Implementation Plan](#implementation-plan)
7. [Error Handling & Fallbacks](#error-handling--fallbacks)
8. [Security & Best Practices](#security--best-practices)

---

## 🤖 Model Specifications

### Gemini 2.5 Flash (`gemini-2.5-flash`)

**Official Documentation**: [ai.google.dev/gemini-api/docs](https://ai.google.dev/gemini-api/docs)

#### Key Capabilities

- **Model Status**: Stable (June 2025 release)
- **Knowledge Cutoff**: January 2025
- **Context Window**:
  - Input: 1,048,576 tokens (~1M tokens)
  - Output: 65,536 tokens (~65K tokens)
- **Multimodal Input**: Text, images, video, audio
- **Output**: Text only

#### Supported Features ✅

- ✅ **Structured Outputs** (JSON Schema) - **CRITICAL for SSBMax**
- ✅ **Function Calling** - Can invoke external tools
- ✅ **Thinking Capability** - Hybrid reasoning with configurable budgets
- ✅ **Context Caching** - Reduce costs for repeated context
- ✅ **Batch API** - 50% cost savings for non-real-time tasks
- ✅ **Code Execution** - Can execute code snippets
- ✅ **File Search** - Can search through uploaded files
- ✅ **Google Search Grounding** - Access real-time information
- ✅ **Google Maps Grounding** - Location-based queries
- ✅ **URL Context** - Can analyze web pages

#### Not Supported ❌

- ❌ Audio Generation
- ❌ Image Generation
- ❌ Live API (real-time streaming)

#### Optimal Use Cases

According to Google:
> "Best model in terms of **price-performance** for **large scale processing**, **low-latency**, **high volume tasks** that require **thinking**, and **agentic use cases**."

**Perfect fit for SSBMax interview feature**:
- ✅ Large-scale question generation (batch processing)
- ✅ Low-latency response analysis (real-time scoring)
- ✅ High volume tasks (multiple users)
- ✅ Thinking capability (complex OLQ assessment)

---

## 💰 Pricing & Rate Limits

**Official Pricing**: [ai.google.dev/gemini-api/docs/pricing](https://ai.google.dev/gemini-api/docs/pricing)

### Free Tier (Recommended for Development)

| Feature | Free Tier Limit |
|---------|----------------|
| **Text/Image/Video Input** | Free of charge |
| **Audio Input** | Free of charge |
| **Output Tokens** | Free of charge (including thinking tokens) |
| **Context Caching** | Not available |
| **Google Search Grounding** | Free up to 500 RPD (requests per day) |

**Critical**: Content on free tier is used to improve Google products. Switch to paid tier for production.

### Paid Tier (Production)

**Per 1 Million Tokens**:

| Feature | Cost |
|---------|------|
| **Text/Image/Video Input** | $0.30 |
| **Audio Input** | $1.00 |
| **Output** | $2.50 (includes thinking tokens) |
| **Context Caching (input)** | $0.03 text/image/video, $0.10 audio |
| **Context Caching (storage)** | $1.00 / million tokens per hour |

**Batch API Pricing** (50% savings):

| Feature | Cost |
|---------|------|
| **Text/Image/Video Input** | $0.15 |
| **Audio Input** | $0.50 |
| **Output** | $1.25 |

### Cost Analysis for SSBMax

**Scenario: 1,000 interviews per month**

Assumptions:
- 10 questions per interview (10,000 total generations)
- Average PIQ context: 500 tokens
- Average question prompt: 200 tokens
- Average question output: 150 tokens
- Average response analysis input: 300 tokens
- Average OLQ scores output: 200 tokens

**Monthly Token Usage**:
- Question generation input: (500 + 200) × 10,000 = 7M tokens
- Question generation output: 150 × 10,000 = 1.5M tokens
- Response analysis input: 300 × 10,000 = 3M tokens
- Response analysis output: 200 × 10,000 = 2M tokens

**Total**: 13.5M tokens

**Monthly Cost (Standard API)**:
- Input: 10M × $0.30/M = $3.00
- Output: 3.5M × $2.50/M = $8.75
- **Total: ~$11.75/month for 1,000 interviews**

**With Context Caching** (reusing PIQ context):
- Cached input (PIQ): 5M × $0.03/M = $0.15
- Fresh input: 5M × $0.30/M = $1.50
- Output: 3.5M × $2.50/M = $8.75
- **Total: ~$10.40/month** (11.5% savings)

**With Batch API** (for question pre-generation):
- Batch input: 10M × $0.15/M = $1.50
- Batch output: 3.5M × $1.25/M = $4.38
- **Total: ~$5.88/month** (50% savings)

---

## 📱 Android/Kotlin Integration

### Recommended Approach: Firebase AI Logic SDK

**Official Guide**: [firebase.google.com/docs/ai-logic/get-started](https://firebase.google.com/docs/ai-logic/get-started)

Google recommends Firebase AI Logic (using Gemini Developer API) over Vertex AI for new Android projects unless you have specific data location requirements.

### Setup Instructions

#### 1. Add Dependencies

**File**: `app/build.gradle.kts`

```kotlin
dependencies {
    // Firebase BOM (Bill of Materials) for version management
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))

    // Firebase AI Logic SDK
    implementation("com.google.firebase:firebase-ai")

    // Kotlin coroutines (likely already in SSBMax)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

**Note**: We use Kotlin with coroutines, so no Guava/ReactiveStreams needed (Java only).

#### 2. Get API Key

**IMPORTANT**: Do NOT add Gemini API key to codebase (CLAUDE.md security rule).

**Recommended approach for SSBMax**:

1. Store API key in `local.properties` (already gitignored):
   ```properties
   gemini.api.key=YOUR_API_KEY_HERE
   ```

2. Read in `build.gradle.kts`:
   ```kotlin
   android {
       defaultConfig {
           // Read from local.properties
           val properties = Properties()
           properties.load(project.rootProject.file("local.properties").inputStream())

           buildConfigField("String", "GEMINI_API_KEY",
               "\"${properties.getProperty("gemini.api.key", "")}\"")
       }
       buildFeatures {
           buildConfig = true
       }
   }
   ```

3. Access in code:
   ```kotlin
   val apiKey = BuildConfig.GEMINI_API_KEY
   ```

**Alternative**: Use Firebase Remote Config (as recommended by Firebase docs) for production.

#### 3. Initialize SDK

**File**: `core/data/src/main/kotlin/com/ssbmax/core/data/service/GeminiService.kt` (new file)

```kotlin
package com.ssbmax.core.data.service

import com.google.firebase.ai.Firebase
import com.google.firebase.ai.generativeModel
import com.google.firebase.ai.GenerativeBackend
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")
    }

    suspend fun generateContent(prompt: String): String {
        return try {
            val response = model.generateContent(prompt)
            response.text ?: throw IllegalStateException("Empty response from Gemini")
        } catch (e: Exception) {
            ErrorLogger.log(e, "Gemini content generation failed")
            throw e
        }
    }
}
```

#### 4. Provide via Hilt

**File**: `app/src/main/kotlin/com/ssbmax/di/AIModule.kt` (new file)

```kotlin
package com.ssbmax.di

import com.ssbmax.core.data.service.GeminiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideGeminiService(): GeminiService {
        return GeminiService()
    }
}
```

---

## 🎯 Structured Output Strategy

**Official Guide**: [firebase.google.com/docs/ai-logic/generate-structured-output](https://firebase.google.com/docs/ai-logic/generate-structured-output)

Gemini 2.5 Flash supports **JSON Schema** for structured outputs, which is **critical** for SSBMax to ensure:
- ✅ Predictable JSON responses (no parsing errors)
- ✅ Type-safe data extraction
- ✅ Consistent OLQ score formatting
- ✅ Validated interview question structure

### Defining Schemas in Kotlin

```kotlin
import com.google.firebase.ai.Schema

// Example: OLQ Score Schema
val olqScoreSchema = Schema.obj(
    mapOf(
        "olq" to Schema.string(), // OLQ enum name (e.g., "SELF_CONFIDENCE")
        "score" to Schema.integer(), // 1-10 scale
        "confidence" to Schema.integer(), // 0-100 percentage
        "reasoning" to Schema.string() // AI explanation
    )
)

// Example: Interview Question Schema
val interviewQuestionSchema = Schema.obj(
    mapOf(
        "questionText" to Schema.string(),
        "expectedOLQs" to Schema.array(Schema.string()),
        "context" to Schema.string()
    ),
    optionalProperties = listOf("context") // Context can be null
)
```

### Using Schemas with Model

```kotlin
import com.google.firebase.ai.generationConfig

val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
    modelName = "gemini-2.5-flash",
    generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = olqScoreSchema
    }
)

val response = model.generateContent(prompt)
val jsonString = response.text // Guaranteed to match schema
```

### Key Limitations

- **Supported MIME types**: `application/json` (for JSON output), `text/x.enum` (for classification)
- **Schema size counts toward input token limit** (keep schemas concise)
- **Supported JSON Schema keywords**: Limited to `enum`, `items`, `maxItems`, `nullable`, `properties`, `required`
- **Optional fields**: Use `optionalProperties` array; fields not listed are **required**

---

## 🎓 Interview Feature Use Cases

### Use Case 1: PIQ-Based Question Generation

**Goal**: Generate 4 personalized interview questions based on candidate's PIQ submission.

**Input**:
- PIQ JSON snapshot (CandidateInfo, FamilyInfo, InterestsInfo, etc.)
- Number of questions to generate (default: 4)

**Output Schema**:

```kotlin
val questionGenerationSchema = Schema.obj(
    mapOf(
        "questions" to Schema.array(
            Schema.obj(
                mapOf(
                    "questionText" to Schema.string(),
                    "expectedOLQs" to Schema.array(Schema.string()),
                    "context" to Schema.string()
                ),
                optionalProperties = listOf("context")
            ),
            maxItems = 10 // Safety limit
        )
    )
)
```

**Prompt Template**:

```kotlin
fun generateQuestionPrompt(piqJson: String, count: Int): String = """
You are an expert SSB (Services Selection Board) interviewer for the Indian Armed Forces.

Based on the candidate's Personal Information Questionnaire (PIQ) below, generate exactly $count personalized interview questions that assess Officer-Like Qualities (OLQs).

PIQ Data:
$piqJson

Available OLQs (15 total):
- Intellectual: EFFECTIVE_INTELLIGENCE, REASONING_ABILITY, ORGANIZING_ABILITY, POWER_OF_EXPRESSION
- Social: SOCIAL_ADJUSTMENT, COOPERATION, INFLUENCE_GROUP
- Dynamic: INITIATIVE, SELF_CONFIDENCE, SPEED_OF_DECISION, DETERMINATION, COURAGE
- Character & Physical: STAMINA, LIVELINESS, SENSE_OF_RESPONSIBILITY

Requirements:
1. Questions must be relevant to the candidate's background, interests, or experiences mentioned in PIQ
2. Each question should target 1-3 OLQs
3. Questions should be open-ended and require thoughtful responses
4. Use professional, respectful tone appropriate for SSB interview
5. Provide context explaining why this question is relevant to the candidate

Output Format: JSON array of questions matching the schema.
""".trimIndent()
```

**Expected Response**:

```json
{
  "questions": [
    {
      "questionText": "You mentioned NCC training in your PIQ. Describe a situation where you had to lead your unit during a challenging exercise.",
      "expectedOLQs": ["INITIATIVE", "ORGANIZING_ABILITY", "DETERMINATION"],
      "context": "Based on candidate's NCC background; assesses leadership and planning skills"
    },
    {
      "questionText": "Your father is in the Navy. How has this influenced your decision to join the armed forces?",
      "expectedOLQs": ["SENSE_OF_RESPONSIBILITY", "DETERMINATION"],
      "context": "Based on family background; explores motivation and commitment"
    }
  ]
}
```

**Fallback Strategy**:
- If Gemini fails → Use `fallback_interview_questions.json`
- If JSON parsing fails → Log error, use fallback
- If schema validation fails → Log error, use fallback

---

### Use Case 2: Response Analysis & OLQ Scoring

**Goal**: Analyze candidate's interview response and generate OLQ scores (1-10 scale, lower is better).

**Input**:
- Question text
- Candidate's response text
- Expected OLQs for the question

**Output Schema**:

```kotlin
val responseAnalysisSchema = Schema.obj(
    mapOf(
        "scores" to Schema.array(
            Schema.obj(
                mapOf(
                    "olq" to Schema.string(), // OLQ enum name
                    "score" to Schema.integer(), // 1-10 (1=exceptional, 10=poor)
                    "confidence" to Schema.integer(), // 0-100
                    "reasoning" to Schema.string() // Why this score?
                )
            )
        ),
        "overallAssessment" to Schema.string(), // Brief summary
        "strengths" to Schema.array(Schema.string()),
        "areasForImprovement" to Schema.array(Schema.string())
    )
)
```

**Prompt Template**:

```kotlin
fun generateAnalysisPrompt(
    question: String,
    response: String,
    expectedOLQs: List<String>
): String = """
You are an SSB interviewing officer analyzing a candidate's response.

Question: $question

Candidate's Response:
$response

Expected OLQs to assess: ${expectedOLQs.joinToString(", ")}

**CRITICAL: SSB Scoring Convention**
- 1-3: Exceptional performance (highly desirable officer quality)
- 4-6: Average to above average
- 7-8: Below average
- 9-10: Poor performance (major concern)

**LOWER SCORES ARE BETTER IN SSB ASSESSMENT**

Analyze the response and provide:
1. OLQ scores (1-10 scale, lower = better) with confidence (0-100%) and reasoning
2. Overall assessment of the response quality
3. Key strengths demonstrated
4. Areas for improvement

Output Format: JSON matching the schema.
""".trimIndent()
```

**Expected Response**:

```json
{
  "scores": [
    {
      "olq": "INITIATIVE",
      "score": 3,
      "confidence": 85,
      "reasoning": "Candidate demonstrated proactive approach by identifying the problem early and taking charge without being asked. Clear evidence of initiative."
    },
    {
      "olq": "ORGANIZING_ABILITY",
      "score": 4,
      "confidence": 75,
      "reasoning": "Showed basic planning skills but could have elaborated on resource allocation and timeline management."
    }
  ],
  "overallAssessment": "Strong response demonstrating leadership potential. Candidate clearly understands the importance of taking initiative and showed practical experience.",
  "strengths": [
    "Proactive mindset",
    "Clear communication",
    "Practical experience cited"
  ],
  "areasForImprovement": [
    "Could provide more detail on planning process",
    "Mention teamwork aspects"
  ]
}
```

**Fallback Strategy**:
- If Gemini fails → Use mock scores (current implementation)
- If scores out of range (not 1-10) → Clamp values + log warning
- If confidence out of range (not 0-100) → Clamp values + log warning
- If critical fields missing → Use mock scores + log error

---

### Use Case 3: Comprehensive Interview Feedback

**Goal**: Generate personalized feedback report after interview completion.

**Input**:
- All questions and responses from the interview
- Aggregated OLQ scores across all responses
- PIQ snapshot for context

**Output Schema**:

```kotlin
val feedbackSchema = Schema.obj(
    mapOf(
        "summary" to Schema.string(), // Overall performance summary
        "olqAnalysis" to Schema.array(
            Schema.obj(
                mapOf(
                    "category" to Schema.string(), // Intellectual/Social/Dynamic/Character
                    "averageScore" to Schema.number(), // 1.0-10.0
                    "strengths" to Schema.array(Schema.string()),
                    "weaknesses" to Schema.array(Schema.string()),
                    "recommendations" to Schema.array(Schema.string())
                )
            )
        ),
        "topStrengths" to Schema.array(Schema.string()),
        "keyAreasForImprovement" to Schema.array(Schema.string()),
        "preparationTips" to Schema.array(Schema.string())
    )
)
```

**Prompt Template**:

```kotlin
fun generateFeedbackPrompt(
    questions: List<String>,
    responses: List<String>,
    olqScores: Map<String, List<Float>>,
    piqJson: String
): String = """
You are an SSB interviewing officer providing comprehensive feedback to a candidate.

Interview Summary:
${questions.zip(responses).mapIndexed { i, (q, r) ->
    "Q${i+1}: $q\nA: $r\n"
}.joinToString("\n")}

OLQ Scores (1-10 scale, lower = better):
${olqScores.entries.joinToString("\n") { (olq, scores) ->
    "$olq: ${scores.joinToString(", ")} (avg: ${scores.average().format(2)})"
}}

PIQ Context:
$piqJson

Provide a comprehensive feedback report that:
1. Summarizes overall interview performance
2. Analyzes performance by OLQ category (Intellectual, Social, Dynamic, Character)
3. Highlights top 3 strengths
4. Identifies 3 key areas for improvement
5. Provides actionable preparation tips for SSB

**Remember**: Lower scores (1-3) are EXCELLENT, higher scores (7-10) indicate areas of concern.

Output Format: JSON matching the schema.
""".trimIndent()
```

**Fallback Strategy**:
- If Gemini fails → Use generic placeholder (current implementation)
- If response incomplete → Use partial data + placeholder for missing sections

---

## 🚀 Implementation Plan

### Phase 1: Question Generation (Lowest Risk)

**Priority**: HIGH
**Estimated Time**: 4-6 hours
**Risk Level**: LOW (has fallback)

#### Steps:

1. ✅ **Create GeminiService** (`core/data/src/main/kotlin/com/ssbmax/core/data/service/GeminiService.kt`)
   - Initialize Firebase AI Logic SDK
   - Add `generatePIQBasedQuestions()` method with structured output
   - Add error handling with ErrorLogger

2. ✅ **Update FirestoreInterviewRepository**
   - Inject GeminiService
   - Call `geminiService.generatePIQBasedQuestions()` in `createSession()`
   - Keep existing JSON fallback strategy
   - Add timeout handling (use `withTimeout()` coroutine)

3. ✅ **Add Unit Tests**
   - Mock GeminiService in repository tests
   - Test success path (AI generates valid questions)
   - Test failure paths (AI fails → fallback, timeout → fallback)
   - Test schema validation

4. ✅ **Add Integration Tests**
   - Test actual Gemini API call with test API key
   - Verify JSON parsing works correctly
   - Validate OLQ enum mapping

**Success Criteria**:
- ✅ Gemini generates 4 valid questions from PIQ
- ✅ Fallback activates on API failure
- ✅ All tests pass
- ✅ No production errors for 1 week

---

### Phase 2: Response Analysis (Medium Risk)

**Priority**: HIGH
**Estimated Time**: 6-8 hours
**Risk Level**: MEDIUM (critical for scoring)

#### Steps:

1. ✅ **Add Response Analysis to GeminiService**
   - Add `analyzeResponse()` method with structured output
   - Define OLQ score schema
   - Add score validation (1-10 range, confidence 0-100)

2. ✅ **Update FirestoreInterviewRepository**
   - Call `geminiService.analyzeResponse()` in `submitResponse()`
   - Keep mock scoring as fallback
   - Add caching for responses (avoid re-analyzing same response)

3. ✅ **Add Score Validation**
   - Clamp scores to 1-10 range if out of bounds
   - Clamp confidence to 0-100 range
   - Log warnings for invalid scores
   - Fall back to mock scores if validation fails critically

4. ✅ **Add Unit Tests**
   - Mock GeminiService in repository tests
   - Test score validation logic
   - Test fallback activation
   - Test caching mechanism

**Success Criteria**:
- ✅ AI generates valid OLQ scores (1-10, lower = better)
- ✅ Score validation works correctly
- ✅ Fallback activates on API failure
- ✅ All tests pass
- ✅ No score corruption in production

---

### Phase 3: Comprehensive Feedback (Complex)

**Priority**: MEDIUM
**Estimated Time**: 8-10 hours
**Risk Level**: MEDIUM (complex prompt, large output)

#### Steps:

1. ✅ **Add Feedback Generation to GeminiService**
   - Add `generateInterviewFeedback()` method
   - Define feedback schema
   - Handle large context (entire interview session)

2. ✅ **Update FirestoreInterviewRepository**
   - Call `geminiService.generateInterviewFeedback()` in `completeInterview()`
   - Replace hardcoded placeholder feedback
   - Add content filtering (detect inappropriate content)

3. ✅ **Add Content Validation**
   - Ensure feedback is constructive and professional
   - Filter out inappropriate or unhelpful content
   - Fall back to generic feedback if validation fails

4. ✅ **Add Unit Tests**
   - Mock GeminiService
   - Test feedback generation with various interview scenarios
   - Test content filtering
   - Test fallback mechanism

**Success Criteria**:
- ✅ AI generates personalized, actionable feedback
- ✅ Feedback is professional and constructive
- ✅ Content filtering works correctly
- ✅ All tests pass

---

### Phase 4: Optimization & Cost Reduction

**Priority**: LOW (only after Phase 1-3 complete)
**Estimated Time**: 4-6 hours
**Risk Level**: LOW

#### Steps:

1. ✅ **Implement Context Caching**
   - Cache PIQ context for repeated use
   - Reduce input token costs by ~50% for PIQ data
   - Set appropriate cache TTL (time-to-live)

2. ✅ **Implement Batch API for Question Pre-Generation**
   - Generate generic questions in batches (overnight jobs)
   - Store in Firestore for immediate use
   - 50% cost savings for non-personalized questions

3. ✅ **Add Thinking Budget Configuration**
   - Experiment with thinking budgets for response analysis
   - Balance quality vs. latency vs. cost
   - Monitor token usage in production

4. ✅ **Add Firebase Remote Config**
   - Allow model name switching without app update
   - Enable/disable AI features remotely
   - A/B test AI vs. mock scoring

**Success Criteria**:
- ✅ Context caching reduces costs by 10-15%
- ✅ Batch API reduces question generation costs by 50%
- ✅ Remote config allows instant feature toggling

---

## 🛡️ Error Handling & Fallbacks

### Error Types & Strategies

| Error Type | Detection | Fallback Strategy | Logging |
|------------|-----------|-------------------|---------|
| **API Failure** | Exception thrown | Use `fallback_interview_questions.json` | ErrorLogger.log() + Crashlytics |
| **Timeout** | `withTimeout()` coroutine | Use fallback questions/mock scores | ErrorLogger.log() |
| **Invalid JSON** | JSON parsing exception | Use fallback questions/mock scores | ErrorLogger.log() + send JSON to Crashlytics |
| **Schema Mismatch** | Missing required fields | Use fallback questions/mock scores | ErrorLogger.log() + send response to Crashlytics |
| **Score Out of Range** | score < 1 or > 10 | Clamp to 1-10 + log warning | ErrorLogger.log() |
| **Confidence Out of Range** | confidence < 0 or > 100 | Clamp to 0-100 + log warning | ErrorLogger.log() |
| **Empty Response** | `response.text` is null/empty | Use fallback questions/mock scores | ErrorLogger.log() |
| **Rate Limit** | 429 HTTP error | Retry with exponential backoff (3 attempts) | ErrorLogger.log() |
| **Quota Exceeded** | 403 HTTP error | Use fallback for remainder of day | ErrorLogger.log() + alert user |

### Timeout Configuration

```kotlin
// Question generation: 10 seconds timeout
val questions = withTimeout(10_000) {
    geminiService.generatePIQBasedQuestions(piqJson, count)
}

// Response analysis: 5 seconds timeout
val scores = withTimeout(5_000) {
    geminiService.analyzeResponse(question, response, expectedOLQs)
}

// Feedback generation: 15 seconds timeout (larger context)
val feedback = withTimeout(15_000) {
    geminiService.generateInterviewFeedback(session)
}
```

### Retry Logic

```kotlin
suspend fun <T> retryWithExponentialBackoff(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            ErrorLogger.log(e, "Attempt ${attempt + 1} failed, retrying...")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block() // Last attempt, let exception propagate
}
```

---

## 🔒 Security & Best Practices

### API Key Security

**DO NOT**:
- ❌ Hardcode API key in source code
- ❌ Commit API key to Git
- ❌ Store API key in `strings.xml` or `BuildConfig` (directly)
- ❌ Log API key in error messages
- ❌ Expose API key in client-side code

**DO**:
- ✅ Store API key in `local.properties` (gitignored)
- ✅ Use `BuildConfig` to inject at build time
- ✅ Use Firebase Remote Config for production (recommended)
- ✅ Rotate API keys periodically
- ✅ Monitor API usage via Google Cloud Console

### Content Safety

**Input Validation**:
- Validate PIQ data before sending to Gemini (sanitize PII if needed)
- Limit response length (prevent abuse)
- Filter inappropriate candidate responses

**Output Validation**:
- Ensure OLQ scores are in valid range (1-10)
- Ensure confidence is in valid range (0-100)
- Filter inappropriate AI-generated content
- Validate JSON schema before parsing

### Privacy & Compliance

**Free Tier**: Content is used to improve Google products
**Paid Tier**: Content is NOT used to improve products (GDPR compliant)

**Recommendation**: Use free tier for development/testing only. Switch to paid tier for production to ensure user privacy.

### Rate Limiting

**Free Tier Limits** (approximate):
- 15 requests per minute (RPM)
- 1 million tokens per minute (TPM)
- 1,500 requests per day (RPD)

**Paid Tier Limits** (higher):
- Configurable based on billing
- Monitor usage via Google Cloud Console
- Set up alerts for quota approaching limits

**SSBMax Strategy**:
- Cache questions aggressively (reduce API calls)
- Use batch API for bulk question generation
- Implement exponential backoff on rate limit errors
- Queue non-urgent requests (feedback generation)

### Testing Strategy

**Development**:
- Use free tier API key in `local.properties`
- Mock GeminiService in unit tests (avoid API calls)
- Use integration tests sparingly (consume quota)

**Production**:
- Use paid tier API key from Firebase Remote Config
- Monitor API usage via Google Cloud Console
- Set up billing alerts (prevent unexpected costs)
- A/B test AI vs. mock scoring (gradual rollout)

---

## 📚 References

### Official Documentation
- [Gemini API Documentation](https://ai.google.dev/gemini-api/docs) - Main developer documentation
- [Gemini Models Overview](https://ai.google.dev/gemini-api/docs/models) - Model specifications
- [Gemini API Pricing](https://ai.google.dev/gemini-api/docs/pricing) - Pricing and rate limits
- [Firebase AI Logic SDK](https://firebase.google.com/docs/ai-logic/get-started) - Android integration guide
- [Structured Outputs](https://firebase.google.com/docs/ai-logic/generate-structured-output) - JSON Schema usage
- [Function Calling](https://ai.google.dev/gemini-api/docs/function-calling) - Advanced capabilities
- [Vertex AI Gemini API](https://developer.android.com/ai/vertex-ai-firebase) - Alternative approach

### Google Blog Posts
- [Start Building with Gemini 2.5 Flash](https://developers.googleblog.com/en/start-building-with-gemini-25-flash/) - Announcement blog
- [Gemini API Structured Outputs](https://blog.google/technology/developers/gemini-api-structured-outputs/) - JSON Schema support
- [Leverage Gemini in Android Apps](https://android-developers.googleblog.com/2023/12/leverage-generative-ai-in-your-android-apps.html) - Android integration

### Tutorials & Guides
- [Add Gemini to Android App (Codelab)](https://developer.android.com/codelabs/gemini-summarize) - Official Android codelab
- [Getting Started with Gemini API and Android](https://developers.google.com/learn/pathways/solution-ai-gemini-getting-started-android) - Learning pathway
- [Gemini API Integration in Android](https://medium.com/@syedamariarasheed/gemini-api-in-android-834d570b4b03) - Community tutorial
- [Integrating Gemini AI into Android App (2025 Guide)](https://www.techswill.com/2025/06/28/integrating-googles-gemini-ai-into-your-android-app-2025-guide/) - Comprehensive guide

### GitHub Resources
- [Gemini API Starter Template](https://github.com/android/codelab-gemini-summary) - Official starter code
- [Gemini API Kotlin Examples](https://github.com/topics/gemini-api?l=kotlin) - Community projects

---

## ✅ Readiness Checklist

Before implementing Gemini integration:

- [ ] API key obtained from [Google AI Studio](https://ai.google.dev)
- [ ] API key stored securely in `local.properties`
- [ ] Firebase AI Logic SDK dependency added
- [ ] GeminiService created with error handling
- [ ] Structured output schemas defined for all use cases
- [ ] Fallback strategies implemented (JSON questions, mock scores)
- [ ] Timeout configuration set (10s, 5s, 15s)
- [ ] Retry logic implemented with exponential backoff
- [ ] ErrorLogger integration for all error paths
- [ ] Unit tests for GeminiService (mocked)
- [ ] Integration tests for repository layer
- [ ] Production monitoring via Crashlytics enabled
- [ ] Cost estimation reviewed and approved
- [ ] User consent for AI processing (if required by privacy policy)

---

**Next Step**: Proceed to Phase 1 (Question Generation) implementation.

**Questions?** Refer to official documentation or ask in the conversation.
