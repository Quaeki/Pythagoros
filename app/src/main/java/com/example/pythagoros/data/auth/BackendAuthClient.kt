package com.example.pythagoros.data.auth

import com.example.pythagoros.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class BackendAuthClient(
    private val baseUrl: String = BuildConfig.PYTHAGOROS_BACKEND_URL,
    private val token: String = BuildConfig.PYTHAGOROS_BACKEND_TOKEN,
) {
    suspend fun requestCode(phone: String): AuthRequestCodeResult =
        withContext(Dispatchers.IO) {
            request("/v1/auth/request-code", JSONObject().put("phone", phone)).fold(
                onSuccess = { json ->
                    AuthRequestCodeResult.Success(
                        requestId = json.optString("requestId"),
                        expiresInSeconds = json.optLong("expiresInSeconds", 300L),
                        debugCode = json.optString("debugCode").takeIf { it.isNotBlank() },
                    )
                },
                onFailure = { AuthRequestCodeResult.Failure(it.message ?: "Не удалось отправить код") },
            )
        }

    suspend fun verifyCode(requestId: String, code: String): AuthVerifyCodeResult =
        withContext(Dispatchers.IO) {
            request(
                "/v1/auth/verify-code",
                JSONObject()
                    .put("requestId", requestId)
                    .put("code", code),
            ).fold(
                onSuccess = { json ->
                    AuthVerifyCodeResult.Success(
                        userId = json.optString("userId"),
                        phone = json.optString("phone"),
                        sessionToken = json.optString("sessionToken"),
                        isNewUser = json.optBoolean("isNewUser"),
                    )
                },
                onFailure = { AuthVerifyCodeResult.Failure(it.message ?: "Не удалось подтвердить код") },
            )
        }

    suspend fun signInWithProvider(identity: ProviderIdentity): AuthProviderSignInResult =
        withContext(Dispatchers.IO) {
            request(
                "/v1/auth/provider",
                JSONObject()
                    .put("provider", identity.provider)
                    .put("providerUserId", identity.providerUserId)
                    .put("idToken", identity.idToken)
                    .put("accessToken", identity.accessToken)
                    .put("email", identity.email)
                    .put("displayName", identity.displayName),
            ).fold(
                onSuccess = { json ->
                    AuthProviderSignInResult.Success(
                        userId = json.optString("userId"),
                        phone = json.optString("phone").takeIf { it.isNotBlank() },
                        email = json.optString("email").takeIf { it.isNotBlank() },
                        displayName = json.optString("displayName").takeIf { it.isNotBlank() },
                        sessionToken = json.optString("sessionToken"),
                        isNewUser = json.optBoolean("isNewUser"),
                    )
                },
                onFailure = { AuthProviderSignInResult.Failure(it.message ?: "Не удалось войти") },
            )
        }

    private fun request(path: String, body: JSONObject): Result<JSONObject> = runCatching {
        if (baseUrl.isBlank() || token.isBlank()) {
            error("Backend не настроен")
        }
        val connection = (URL("${baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { stream ->
            stream.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val response = connection.responseText()
        if (connection.responseCode !in 200..299) {
            error(response.toApiErrorMessage())
        }
        JSONObject(response)
    }
}

data class ProviderIdentity(
    val provider: String,
    val providerUserId: String? = null,
    val idToken: String? = null,
    val accessToken: String? = null,
    val email: String? = null,
    val displayName: String? = null,
)

sealed interface AuthRequestCodeResult {
    data class Success(
        val requestId: String,
        val expiresInSeconds: Long,
        val debugCode: String?,
    ) : AuthRequestCodeResult

    data class Failure(val message: String) : AuthRequestCodeResult
}

sealed interface AuthProviderSignInResult {
    data class Success(
        val userId: String,
        val phone: String?,
        val email: String?,
        val displayName: String?,
        val sessionToken: String,
        val isNewUser: Boolean,
    ) : AuthProviderSignInResult

    data class Failure(val message: String) : AuthProviderSignInResult
}

sealed interface AuthVerifyCodeResult {
    data class Success(
        val userId: String,
        val phone: String,
        val sessionToken: String,
        val isNewUser: Boolean,
    ) : AuthVerifyCodeResult

    data class Failure(val message: String) : AuthVerifyCodeResult
}

private fun HttpURLConnection.responseText(): String {
    val stream = if (responseCode in 200..299) inputStream else errorStream ?: inputStream
    return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
}

private fun String.toApiErrorMessage(): String =
    runCatching {
        val json = JSONObject(this)
        json.optString("message").ifBlank {
            json.optString("code").ifBlank { "Backend вернул ошибку" }
        }
    }.getOrDefault("Backend вернул ошибку")
