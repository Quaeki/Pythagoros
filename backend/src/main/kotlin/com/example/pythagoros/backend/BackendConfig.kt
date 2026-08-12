package com.example.pythagoros.backend

data class BackendConfig(
    val host: String,
    val port: Int,
    val aiApiKey: String?,
    val apiToken: String?,
    val aiProvider: AiProvider,
    val aiModel: String,
    val geminiBaseUrl: String,
    val openAiCompatibleBaseUrl: String,
    val authStoragePath: String,
) {
    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): BackendConfig {
            val apiKey = env["AITUNNEL_API_KEY"]
                ?.takeIf { it.isNotBlank() }
                ?: env["GEMINI_API_KEY"]?.takeIf { it.isNotBlank() }
            val provider = env["AI_PROVIDER"]?.lowercase()?.let { value ->
                when (value) {
                    "aitunnel", "openai-compatible", "openai_compatible" -> AiProvider.OpenAiCompatible
                    "google", "gemini" -> AiProvider.GoogleGemini
                    else -> null
                }
            } ?: if (apiKey?.startsWith("sk-aitunnel-") == true) {
                AiProvider.OpenAiCompatible
            } else {
                AiProvider.GoogleGemini
            }

            return BackendConfig(
                host = env["HOST"].orEmpty().ifBlank { "0.0.0.0" },
                port = env["PORT"]?.toIntOrNull() ?: 8080,
                aiApiKey = apiKey,
                apiToken = env["PYTHAGOROS_API_TOKEN"]?.takeIf { it.isNotBlank() },
                aiProvider = provider,
                aiModel = env["AI_MODEL"].orEmpty()
                    .ifBlank { env["GEMINI_MODEL"].orEmpty() }
                    .ifBlank { "gemini-3.6-flash" },
                geminiBaseUrl = env["GEMINI_BASE_URL"].orEmpty()
                    .ifBlank { "https://generativelanguage.googleapis.com/v1beta" },
                openAiCompatibleBaseUrl = env["OPENAI_COMPATIBLE_BASE_URL"].orEmpty()
                    .ifBlank { "https://api.aitunnel.ru/v1" },
                authStoragePath = env["AUTH_STORAGE_PATH"].orEmpty()
                    .ifBlank { "/opt/pythagoros-backend-data/auth.json" },
            )
        }
    }
}

enum class AiProvider {
    GoogleGemini,
    OpenAiCompatible,
}
