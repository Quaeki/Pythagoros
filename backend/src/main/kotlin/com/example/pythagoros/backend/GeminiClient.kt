package com.example.pythagoros.backend

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GeminiClient(
    private val config: BackendConfig,
    private val json: Json = BackendJson,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build(),
) {
    fun solve(request: PremiumSolveRequest): PremiumSolveResponse {
        val apiKey = config.aiApiKey ?: throw MissingGeminiApiKeyException()
        val payload = when (config.aiProvider) {
            AiProvider.GoogleGemini -> solveWithGoogleGemini(request, apiKey)
            AiProvider.OpenAiCompatible -> solveWithOpenAiCompatible(request, apiKey)
        }
        return PremiumSolveResponse(
            expression = request.expression,
            problemType = request.problemType,
            answer = payload.answer,
            steps = payload.steps,
            graph = payload.graph,
            model = config.aiModel,
        )
    }

    private fun solveWithGoogleGemini(
        request: PremiumSolveRequest,
        apiKey: String,
    ): GeminiSolvePayload {
        val geminiRequest = GeminiGenerateContentRequest(
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = SolvePromptFactory.systemInstruction(request.locale))),
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = SolvePromptFactory.userPrompt(request))),
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.15,
                responseMimeType = "application/json",
            ),
        )

        val endpoint = "${config.geminiBaseUrl}/models/${config.aiModel}:generateContent"
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(45))
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(geminiRequest)))
            .build()

        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw GeminiRequestException(response.statusCode(), response.body())
        }

        val text = json.decodeFromString<GeminiGenerateContentResponse>(response.body())
            .candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull { it.text != null }
            ?.text
            ?.trim()
            ?: throw GeminiRequestException(response.statusCode(), "Gemini returned no text candidate")

        return decodePayload(text)
    }

    private fun solveWithOpenAiCompatible(
        request: PremiumSolveRequest,
        apiKey: String,
    ): GeminiSolvePayload {
        val chatRequest = OpenAiChatCompletionRequest(
            model = config.aiModel,
            messages = listOf(
                OpenAiMessage(
                    role = "system",
                    content = SolvePromptFactory.systemInstruction(request.locale),
                ),
                OpenAiMessage(
                    role = "user",
                    content = SolvePromptFactory.userPrompt(request),
                ),
            ),
            temperature = 0.15,
            responseFormat = OpenAiResponseFormat(type = "json_object"),
        )

        val baseUrl = config.openAiCompatibleBaseUrl.trimEnd('/')
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(chatRequest)))
            .build()

        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw GeminiRequestException(response.statusCode(), response.body())
        }

        val text = json.decodeFromString<OpenAiChatCompletionResponse>(response.body())
            .choices
            .firstOrNull()
            ?.message
            ?.content
            ?.trim()
            ?: throw GeminiRequestException(response.statusCode(), "AI provider returned no text candidate")

        return decodePayload(text)
    }

    private fun decodePayload(text: String): GeminiSolvePayload {
        val cleaned = text
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return runCatching { json.decodeFromString<GeminiSolvePayload>(cleaned) }
            .getOrElse {
                val start = cleaned.indexOf('{')
                val end = cleaned.lastIndexOf('}')
                if (start >= 0 && end > start) {
                    json.decodeFromString(cleaned.substring(start, end + 1))
                } else {
                    throw GeminiPayloadException("Gemini response is not valid JSON")
                }
            }
    }
}

class MissingGeminiApiKeyException : IllegalStateException("AI API key is not configured")

class GeminiRequestException(
    val statusCode: Int,
    message: String,
) : RuntimeException("Gemini request failed with HTTP $statusCode: $message")

class GeminiPayloadException(message: String) : RuntimeException(message)

@Serializable
private data class GeminiGenerateContentRequest(
    @SerialName("system_instruction")
    val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig,
)

@Serializable
private data class GeminiGenerationConfig(
    val temperature: Double,
    val responseMimeType: String,
)

@Serializable
private data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
private data class GeminiPart(
    val text: String? = null,
)

@Serializable
private data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent,
)

@Serializable
private data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double,
    @SerialName("response_format")
    val responseFormat: OpenAiResponseFormat,
)

@Serializable
private data class OpenAiMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class OpenAiResponseFormat(
    val type: String,
)

@Serializable
private data class OpenAiChatCompletionResponse(
    val choices: List<OpenAiChoice> = emptyList(),
)

@Serializable
private data class OpenAiChoice(
    val message: OpenAiMessage,
)
