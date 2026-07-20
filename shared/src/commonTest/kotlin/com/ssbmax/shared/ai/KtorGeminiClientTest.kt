package com.ssbmax.shared.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the Ktor-based Gemini REST call end-to-end against a MockEngine
 * (request shape + response parsing) without a live network call or a real
 * API key -- this is the deterministic substitute for a contract test here,
 * since GitLive/Gemini calls can't be unit-tested against the live service.
 * Confirms the "Gemini -> Ktor" migration item actually round-trips data
 * correctly, not just that it compiles against the Ktor API.
 */
class KtorGeminiClientTest {

    private fun clientWith(engine: MockEngine) = KtorGeminiClient(
        httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
        apiKey = "test-key"
    )

    @Test
    fun `sends the prompt and returns the parsed candidate text`() = runTest {
        val engine = MockEngine { request ->
            assertTrue(request.url.toString().contains("key=test-key"))
            respond(
                content = """{"candidates":[{"content":{"parts":[{"text":"Hello from Gemini"}]}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = clientWith(engine).generateContent("Reply with OK")

        assertEquals("Hello from Gemini", result.getOrThrow())
    }

    @Test
    fun `no candidates in the response is a failure not an empty success`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"candidates":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = clientWith(engine).generateContent("prompt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `network error surfaces as Result failure not an unhandled exception`() = runTest {
        val engine = MockEngine {
            respond(content = "Internal error", status = HttpStatusCode.InternalServerError)
        }

        val result = clientWith(engine).generateContent("prompt")
        assertTrue(result.isFailure)
    }
}
