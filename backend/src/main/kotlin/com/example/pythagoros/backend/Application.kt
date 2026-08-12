package com.example.pythagoros.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

val BackendJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

fun Application.backendModule(config: BackendConfig = BackendConfig.fromEnvironment()) {
    val geminiClient = GeminiClient(config)
    val authService = AuthService(Path.of(config.authStoragePath))
    val logger = environment.log

    install(ContentNegotiation) {
        json(BackendJson)
    }
    install(CallLogging) {
        level = Level.INFO
    }
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    install(StatusPages) {
        exception<MissingGeminiApiKeyException> { call, cause ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ApiError("ai_key_missing", cause.message ?: "AI API key is not configured"),
            )
        }
        exception<MissingApiTokenException> { call, cause ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ApiError("api_token_missing", cause.message ?: "PYTHAGOROS_API_TOKEN is not configured"),
            )
        }
        exception<InvalidApiTokenException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError("unauthorized", cause.message ?: "Invalid API token"),
            )
        }
        exception<GeminiRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadGateway,
                ApiError("gemini_request_failed", cause.message ?: "Gemini request failed"),
            )
        }
        exception<GeminiPayloadException> { call, cause ->
            call.respond(
                HttpStatusCode.BadGateway,
                ApiError("gemini_payload_invalid", cause.message ?: "Gemini returned invalid payload"),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("bad_request", cause.message ?: "Bad request"),
            )
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled backend error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("internal_error", "Internal backend error"),
            )
        }
    }

    routing {
        get("/") {
            call.respondText("Pythagoros backend")
        }

        get("/health") {
            call.respond(HealthResponse(ok = true, service = "pythagoros-backend"))
        }

        post("/v1/auth/request-code") {
            call.requireApiToken(config)
            val request = call.receive<AuthRequestCodeRequest>()
            call.respond(authService.requestCode(request.phone))
        }

        post("/v1/auth/verify-code") {
            call.requireApiToken(config)
            val request = call.receive<AuthVerifyCodeRequest>()
            call.respond(authService.verifyCode(request.requestId, request.code))
        }

        post("/v1/auth/provider") {
            call.requireApiToken(config)
            val request = call.receive<AuthProviderSignInRequest>()
            call.respond(authService.signInWithProvider(request))
        }

        post("/v1/solve/ai") {
            call.requireApiToken(config)
            val request = call.receive<PremiumSolveRequest>()
            require(request.expression.isNotBlank()) { "expression must not be blank" }
            call.respond(geminiClient.solve(request))
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.requireApiToken(config: BackendConfig) {
    val expected = config.apiToken ?: throw MissingApiTokenException()
    val provided = request.headers[HttpHeaders.Authorization]
        ?.removePrefix("Bearer")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: throw InvalidApiTokenException()

    val expectedBytes = expected.toByteArray(StandardCharsets.UTF_8)
    val providedBytes = provided.toByteArray(StandardCharsets.UTF_8)
    if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
        throw InvalidApiTokenException()
    }
}

class MissingApiTokenException : IllegalStateException("PYTHAGOROS_API_TOKEN is not configured")

class InvalidApiTokenException : IllegalArgumentException("Invalid API token")
