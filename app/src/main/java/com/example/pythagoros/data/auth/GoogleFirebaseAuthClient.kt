package com.example.pythagoros.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.pythagoros.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class GoogleFirebaseAuthClient(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
) {
    suspend fun signIn(): GoogleFirebaseSignInResult {
        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (serverClientId.isBlank()) {
            return GoogleFirebaseSignInResult.Failure(
                "Добавь google.web.client.id в local.properties и обнови Firebase config",
            )
        }
        if (!BuildConfig.FIREBASE_CONFIGURED) {
            return GoogleFirebaseSignInResult.Failure(
                "Добавь app/google-services.json из Firebase Console",
            )
        }

        return runCatching {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential as? CustomCredential
                ?: error("Google не вернул учётные данные")
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                error("Получен неподдерживаемый тип Google credential")
            }

            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            val firebaseAuth = FirebaseAuth.getInstance()
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: error("Firebase не вернул пользователя")
            ProviderIdentity(
                provider = "google",
                providerUserId = user.uid,
                idToken = googleCredential.idToken,
                email = user.email ?: googleCredential.id,
                displayName = user.displayName,
            )
        }.fold(
            onSuccess = { GoogleFirebaseSignInResult.Success(it) },
            onFailure = { error ->
                val message = when (error) {
                    is GoogleIdTokenParsingException -> "Не удалось прочитать Google token"
                    else -> error.message ?: "Не удалось войти через Google"
                }
                GoogleFirebaseSignInResult.Failure(message)
            },
        )
    }
}

sealed interface GoogleFirebaseSignInResult {
    data class Success(val identity: ProviderIdentity) : GoogleFirebaseSignInResult
    data class Failure(val message: String) : GoogleFirebaseSignInResult
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed"),
                )
            }
        }
    }
