package com.example.pythagoros.backend

import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthService(
    private val storagePath: Path,
    private val json: Json = BackendJson,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val random = SecureRandom()
    private val pendingCodes = ConcurrentHashMap<String, PendingCode>()
    private val lock = Any()
    private var state: AuthState = loadState()

    fun requestCode(phone: String): AuthRequestCodeResponse {
        val normalizedPhone = phone.normalizedPhone()
        val code = (random.nextInt(900_000) + 100_000).toString()
        val requestId = UUID.randomUUID().toString()
        val expiresAt = clock.instant().plusSeconds(CodeTtlSeconds)
        pendingCodes[requestId] = PendingCode(
            phone = normalizedPhone,
            code = code,
            expiresAtEpochSeconds = expiresAt.epochSecond,
        )

        return AuthRequestCodeResponse(
            requestId = requestId,
            expiresInSeconds = CodeTtlSeconds,
            debugCode = code,
        )
    }

    fun verifyCode(requestId: String, code: String): AuthVerifyCodeResponse {
        val pending = pendingCodes[requestId] ?: throw IllegalArgumentException("Код устарел или не найден")
        if (clock.instant().epochSecond > pending.expiresAtEpochSeconds) {
            pendingCodes.remove(requestId)
            throw IllegalArgumentException("Код устарел")
        }
        if (code.filter(Char::isDigit) != pending.code) {
            throw IllegalArgumentException("Неверный код")
        }
        pendingCodes.remove(requestId)

        synchronized(lock) {
            val existing = state.users.firstOrNull { it.phone == pending.phone }
            val isNew = existing == null
            val user = existing ?: StoredUser(
                id = UUID.randomUUID().toString(),
                phone = pending.phone,
                createdAtEpochSeconds = clock.instant().epochSecond,
            )
            val session = StoredSession(
                token = randomToken(),
                userId = user.id,
                createdAtEpochSeconds = clock.instant().epochSecond,
            )
            state = state.copy(
                users = if (isNew) state.users + user else state.users,
                sessions = state.sessions.filterNot { it.userId == user.id } + session,
            )
            saveState(state)
            return AuthVerifyCodeResponse(
                userId = user.id,
                phone = user.phone,
                sessionToken = session.token,
                isNewUser = isNew,
            )
        }
    }

    fun signInWithProvider(request: AuthProviderSignInRequest): AuthSessionResponse {
        val provider = request.provider.trim().lowercase(Locale.US)
        require(provider in SupportedProviders) { "Неподдерживаемый провайдер входа" }
        val providerUserId = request.providerUserId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: request.idToken.providerSubject()
            ?: request.accessToken.tokenFingerprint()
            ?: throw IllegalArgumentException("Провайдер не вернул идентификатор пользователя")
        val email = request.email?.trim()?.takeIf { it.isNotBlank() }
        val displayName = request.displayName?.trim()?.takeIf { it.isNotBlank() }

        synchronized(lock) {
            val existing = state.users.firstOrNull {
                it.provider == provider && it.providerUserId == providerUserId
            }
            val isNew = existing == null
            val user = existing?.copy(
                email = email ?: existing.email,
                displayName = displayName ?: existing.displayName,
            ) ?: StoredUser(
                id = UUID.randomUUID().toString(),
                phone = "",
                provider = provider,
                providerUserId = providerUserId,
                email = email,
                displayName = displayName,
                createdAtEpochSeconds = clock.instant().epochSecond,
            )
            val session = StoredSession(
                token = randomToken(),
                userId = user.id,
                createdAtEpochSeconds = clock.instant().epochSecond,
            )
            state = state.copy(
                users = state.users
                    .filterNot { it.id == user.id }
                    .plus(user),
                sessions = state.sessions.filterNot { it.userId == user.id } + session,
            )
            saveState(state)
            return AuthSessionResponse(
                userId = user.id,
                phone = user.phone.takeIf { it.isNotBlank() },
                email = user.email,
                displayName = user.displayName,
                sessionToken = session.token,
                isNewUser = isNew,
            )
        }
    }

    private fun loadState(): AuthState =
        runCatching {
            if (!Files.exists(storagePath)) return AuthState()
            json.decodeFromString<AuthState>(Files.readString(storagePath))
        }.getOrDefault(AuthState())

    private fun saveState(state: AuthState) {
        Files.createDirectories(storagePath.parent)
        Files.writeString(storagePath, json.encodeToString(state))
    }

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun String.normalizedPhone(): String {
        val digits = filter(Char::isDigit)
        require(digits.length >= 10) { "Введите корректный номер телефона" }
        return "+$digits"
    }

    private companion object {
        const val CodeTtlSeconds = 300L
        val SupportedProviders = setOf("google", "yandex")
    }
}

private data class PendingCode(
    val phone: String,
    val code: String,
    val expiresAtEpochSeconds: Long,
)

@Serializable
private data class AuthState(
    val users: List<StoredUser> = emptyList(),
    val sessions: List<StoredSession> = emptyList(),
)

@Serializable
private data class StoredUser(
    val id: String,
    val phone: String = "",
    val provider: String? = null,
    val providerUserId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val createdAtEpochSeconds: Long,
)

@Serializable
private data class StoredSession(
    val token: String,
    val userId: String,
    val createdAtEpochSeconds: Long,
)

private fun String?.providerSubject(): String? {
    if (isNullOrBlank()) return null
    val parts = split(".")
    if (parts.size < 2) return null
    return runCatching {
        val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.getOrNull(1)
    }.getOrNull()
}

private fun String?.tokenFingerprint(): String? {
    if (isNullOrBlank()) return null
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
