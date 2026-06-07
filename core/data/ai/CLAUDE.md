# core/data/ai/CLAUDE.md — Gemini AI Integration

**Scope:** Gemini API service, model configuration, prompt engineering, response parsing, rate limiting. Inherits [core/data/CLAUDE.md](../CLAUDE.md). This file adds AI-specific patterns.

**Key Addition:** Gemini provider function + structured prompts + JSON parsing + error handling.

---

## GeminiService Setup (Hilt Provider)

**Dependency Injection:**
```kotlin
// In core/data/src/main/kotlin/com/ssbmax/core/data/di/AIModule.kt

@Module
@InstallIn(SingletonComponent::class)
object AIModule {
  
  @Provides
  @Singleton
  fun provideGenerativeModel(): GenerativeModel {
    val apiKey = BuildConfig.GEMINI_API_KEY
    
    return GenerativeModel(
      modelName = "gemini-2.0-flash",
      apiKey = apiKey,
      generationConfig = generationConfig {
        temperature = 0.7f
        topP = 0.9f
        topK = 40
        maxOutputTokens = 2048
      },
      safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE)
      )
    )
  }
  
  @Provides
  @Singleton
  fun provideGeminiService(
    generativeModel: GenerativeModel,
    errorLogger: ErrorLogger
  ): GeminiService {
    return GeminiService(generativeModel, errorLogger)
  }
}

// ✅ API key from BuildConfig (never hardcoded)
// ✅ Temperature tuned for deterministic responses
// ✅ Safety settings prevent harmful outputs
// ✅ Singleton (one model instance)
```

**GeminiService Implementation:**
```kotlin
class GeminiService @Inject constructor(
  private val model: GenerativeModel,
  private val errorLogger: ErrorLogger
) {
  
  suspend fun evaluateInterviewResponse(
    questionText: String,
    userResponse: String
  ): Result<InterviewEvaluation> {
    return try {
      val prompt = buildEvaluationPrompt(questionText, userResponse)
      val response = model.generateContent(prompt)
      val text = response.text
      
      val evaluation = parseEvaluationResponse(text)
      Result.Success(evaluation)
    } catch (e: Exception) {
      errorLogger.log(e, "Gemini API error: evaluateInterviewResponse")
      Result.Failure(e)
    }
  }
  
  private fun buildEvaluationPrompt(question: String, response: String): String {
    return """
    You are an expert SSB interview evaluator with 20+ years experience.
    
    Question: $question
    
    Candidate's Response: $response
    
    Provide evaluation in EXACTLY this JSON format (no markdown, no extra text):
    {
      "score": 1-10,
      "strengths": ["strength1", "strength2"],
      "improvements": ["area1", "area2"],
      "comments": "Brief feedback"
    }
    """.trimIndent()
  }
  
  private fun parseEvaluationResponse(json: String): InterviewEvaluation {
    // Remove markdown if present (e.g., ```json ... ```)
    val cleanJson = json
      .replace("```json", "")
      .replace("```", "")
      .trim()
    
    val jsonObj = JSONObject(cleanJson)
    
    return InterviewEvaluation(
      score = jsonObj.getInt("score"),
      strengths = jsonObj.getJSONArray("strengths").let { arr ->
        (0 until arr.length()).map { arr.getString(it) }
      },
      improvements = jsonObj.getJSONArray("improvements").let { arr ->
        (0 until arr.length()).map { arr.getString(it) }
      },
      comments = jsonObj.getString("comments")
    )
  }
}

// ✅ Structured prompt with explicit format
// ✅ Error handling wraps in Result<T>
// ✅ JSON parsing with fallback for markdown wrappers
```

---

## Prompt Engineering (Structured Prompts)

**Pattern: Explicit format + examples**

```kotlin
// Good: Structured prompt
fun buildTATAnalysisPrompt(story: String, response: String): String {
  return """
  You are analyzing a Thematic Apperception Test (TAT) response.
  
  Story: $story
  
  Candidate's Response: $response
  
  Analyze the response and provide JSON:
  {
    "themes": ["theme1", "theme2"],
    "personality_traits": ["trait1", "trait2"],
    "concerns": ["concern1"],
    "creativity_score": 1-10,
    "coherence_score": 1-10
  }
  """.trimIndent()
}

// Bad: Vague prompt
fun badPrompt(story: String): String {
  return "Analyze this TAT response: $story"
  // Output unpredictable, might not be JSON
}

// ✅ Explicit format (JSON structure)
// ✅ Clear instructions (analyze personality, concerns, etc.)
// ✅ Scoring guidelines (1-10 scale)
// ✅ Consistent outputs
```

**Temperature Settings:**
```kotlin
// For deterministic responses (same input → same output)
temperature = 0.1f // Very conservative

// For creative responses (variation expected)
temperature = 0.7f // Moderate variation

// For brainstorming (high variation)
temperature = 1.0f // Maximum randomness

// SSBMax evaluation: use 0.7-0.8 (consistency with some natural variation)
```

---

## Response Parsing & Validation

**Pattern: Robust JSON parsing**

```kotlin
suspend fun evaluateWATResponse(
  prompt: String,
  userResponse: String
): Result<WATEvaluation> {
  return try {
    val response = model.generateContent("""
      $prompt
      
      Candidate Response: $userResponse
      
      Return ONLY valid JSON (no markdown):
      {
        "confidence_score": 1-10,
        "structure_score": 1-10,
        "clarity": "good|fair|poor",
        "key_insights": ["insight1", "insight2"]
      }
    """.trimIndent())
    
    val text = response.text.trim()
    
    // Remove markdown if present
    val json = if (text.startsWith("```")) {
      text.substringAfter("\n").substringBefore("```").trim()
    } else {
      text
    }
    
    // Validate JSON structure
    val jsonObj = JSONObject(json)
    
    if (!jsonObj.has("confidence_score") ||
        !jsonObj.has("structure_score") ||
        !jsonObj.has("clarity")) {
      return Result.Failure(Exception("Missing required fields"))
    }
    
    val evaluation = WATEvaluation(
      confidenceScore = jsonObj.getInt("confidence_score").coerceIn(1, 10),
      structureScore = jsonObj.getInt("structure_score").coerceIn(1, 10),
      clarity = jsonObj.getString("clarity"),
      insights = jsonObj.getJSONArray("key_insights").let { arr ->
        (0 until arr.length()).map { arr.getString(it) }
      }
    )
    
    Result.Success(evaluation)
  } catch (e: JSONException) {
    errorLogger.log(e, "Failed to parse Gemini JSON response")
    Result.Failure(e)
  } catch (e: Exception) {
    errorLogger.log(e, "Gemini API error in evaluateWATResponse")
    Result.Failure(e)
  }
}

// ✅ Remove markdown wrappers
// ✅ Validate structure before use
// ✅ Handle JSONException separately
// ✅ All errors wrapped in Result<T>
```

---

## Rate Limiting (API Quota Management)

**Pattern: Respect API limits**

```kotlin
class RateLimitedGeminiService @Inject constructor(
  private val gemini: GeminiService,
  private val db: FirebaseFirestore
) {
  
  private companion object {
    const val LIMIT_PER_MINUTE = 30
    const val LIMIT_PER_HOUR = 500
  }
  
  suspend fun evaluateWithRateLimit(
    userId: String,
    questionText: String,
    userResponse: String
  ): Result<InterviewEvaluation> {
    // Check rate limit
    val usageDoc = db.collection("users").document(userId)
      .collection("ai_usage").document("stats").get().await()
    
    val minuteUsage = usageDoc.getLong("minute_usage")?.toInt() ?: 0
    val hourUsage = usageDoc.getLong("hour_usage")?.toInt() ?: 0
    
    if (minuteUsage >= LIMIT_PER_MINUTE || hourUsage >= LIMIT_PER_HOUR) {
      return Result.Failure(Exception("Rate limit exceeded"))
    }
    
    // Call Gemini
    val result = gemini.evaluateInterviewResponse(questionText, userResponse)
    
    // Increment usage counter
    if (result is Result.Success) {
      db.collection("users").document(userId)
        .collection("ai_usage").document("stats").update(
          "minute_usage", FieldValue.increment(1),
          "hour_usage", FieldValue.increment(1)
        )
    }
    
    return result
  }
}

// ✅ Check limits before API call
// ✅ Count successful calls only
// ✅ Report quota exhaustion to user
```

---

## Streaming Responses (Long-Running Evaluations)

**Pattern: Stream partial results**

```kotlin
suspend fun streamEvaluationResponse(
  questionText: String,
  userResponse: String,
  onPartialResult: suspend (String) -> Unit
): Result<InterviewEvaluation> {
  return try {
    val prompt = buildEvaluationPrompt(questionText, userResponse)
    
    model.generateContentStream(prompt).collect { response ->
      // Partial result available
      onPartialResult(response.text)
    }
    
    // Final result
    val finalResponse = model.generateContent(prompt)
    val evaluation = parseEvaluationResponse(finalResponse.text)
    Result.Success(evaluation)
  } catch (e: Exception) {
    errorLogger.log(e, "Streaming evaluation failed")
    Result.Failure(e)
  }
}

// In Repository:
suspend fun streamEvaluation(
  response: InterviewResponse,
  onProgress: suspend (String) -> Unit
): Result<Evaluation> {
  return geminiService.streamEvaluationResponse(
    response.question,
    response.userResponse,
    onProgress = { partial -> onProgress(partial) }
  )
}

// In ViewModel:
fun submitTestWithStreaming() {
  viewModelScope.launch {
    _uiState.value = UiState.Evaluating(progress = "")
    
    val result = repository.streamEvaluation(
      response = testResponse,
      onProgress = { partial ->
        _uiState.value = (_uiState.value as? UiState.Evaluating)
          ?.copy(progress = partial) ?: UiState.Evaluating(partial)
      }
    )
    
    when (result) {
      is Result.Success -> _uiState.value = UiState.Complete(result.data)
      is Result.Failure -> _uiState.value = UiState.Error(result.exception.message ?: "")
    }
  }
}

// ✅ Stream partial results for better UX
// ✅ Show progress to user while waiting
// ✅ Get final result after stream completes
```

---

## Error Handling (Gemini-Specific Errors)

**Pattern: Categorize errors**

```kotlin
sealed class GeminiError : Exception() {
  data class RateLimitExceeded(override val message: String) : GeminiError()
  data class InvalidInput(override val message: String) : GeminiError()
  data class APIError(override val message: String) : GeminiError()
  data class ParseError(override val message: String) : GeminiError()
}

class GeminiService(
  private val model: GenerativeModel,
  private val errorLogger: ErrorLogger
) {
  
  suspend fun evaluateResponse(
    question: String,
    response: String
  ): Result<Evaluation> {
    return try {
      val aiResponse = model.generateContent(buildPrompt(question, response))
      val evaluation = parseResponse(aiResponse.text)
      Result.Success(evaluation)
    } catch (e: Exception) {
      val geminiError = when {
        e.message?.contains("429") == true -> 
          GeminiError.RateLimitExceeded("Too many requests")
        e.message?.contains("400") == true -> 
          GeminiError.InvalidInput("Invalid input")
        e.message?.contains("JSONException") == true -> 
          GeminiError.ParseError("Failed to parse response")
        else -> GeminiError.APIError(e.message ?: "Unknown error")
      }
      
      errorLogger.log(geminiError, "Gemini evaluation failed", mapOf(
        "question_length" to question.length,
        "response_length" to response.length
      ))
      
      Result.Failure(geminiError)
    }
  }
}

// ✅ Specific error types for better handling
// ✅ Log context (input lengths, etc.)
// ✅ Distinguishable from other errors
```

---

## Testing Gemini Integration (Mocking)

**Unit Tests:**
```kotlin
class GeminiServiceTest {
  private val mockGenerativeModel = mockk<GenerativeModel>()
  private val mockErrorLogger = mockk<ErrorLogger>(relaxed = true)
  private val service = GeminiService(mockGenerativeModel, mockErrorLogger)
  
  @Test
  fun evaluateResponseReturnsSuccessWithValidJSON() = runTest {
    val jsonResponse = """
      {
        "score": 8,
        "strengths": ["clear", "concise"],
        "improvements": ["more examples"],
        "comments": "Good"
      }
    """.trimIndent()
    
    coEvery { mockGenerativeModel.generateContent(any()) } returns 
      mockk<GenerateContentResponse>().apply {
        every { text } returns jsonResponse
      }
    
    val result = service.evaluateInterviewResponse("Q?", "A.")
    
    assertThat(result).isInstanceOf(Result.Success::class.java)
    val evaluation = (result as Result.Success).data
    assertThat(evaluation.score).isEqualTo(8)
  }
  
  @Test
  fun evaluateResponseHandlesInvalidJSON() = runTest {
    coEvery { mockGenerativeModel.generateContent(any()) } returns 
      mockk<GenerateContentResponse>().apply {
        every { text } returns "Invalid JSON"
      }
    
    val result = service.evaluateInterviewResponse("Q?", "A.")
    
    assertThat(result).isInstanceOf(Result.Failure::class.java)
  }
}
```

---

## Best Practices

1. **Use structured prompts** — explicit format for consistent outputs
2. **Parse JSON robustly** — handle markdown, missing fields
3. **Wrap all errors in Result<T>** — no exceptions in data layer
4. **Rate limit carefully** — respect API quotas
5. **Log errors with context** — help debugging
6. **Stream for UX** — show progress on long evaluations
7. **Test mocking** — mock GenerativeModel for tests

---

## References

- **Parent:** [core/data/CLAUDE.md](../CLAUDE.md) (error handling, Result<T>)
- **Gemini SDK:** https://ai.google.dev/libraries/kotlin
- **Functions module:** [functions/CLAUDE.md](../../functions/CLAUDE.md) (backend calls)

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
