package com.example.pythagoros.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pythagoros.data.auth.AuthProviderSignInResult
import com.example.pythagoros.data.auth.AuthRequestCodeResult
import com.example.pythagoros.data.auth.AuthVerifyCodeResult
import com.example.pythagoros.data.auth.BackendAuthClient
import com.example.pythagoros.data.auth.GoogleFirebaseAuthClient
import com.example.pythagoros.data.auth.GoogleFirebaseSignInResult
import com.example.pythagoros.data.auth.ProviderIdentity
import com.example.pythagoros.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val prefs: AppPreferences,
    private val authClient: BackendAuthClient,
    private val googleAuthClient: GoogleFirebaseAuthClient,
) : ViewModel() {

    var phone by mutableStateOf("")
        private set

    var requestId by mutableStateOf("")
        private set

    var debugCode by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun beginExternalSignIn() {
        isLoading = true
        errorMessage = null
    }

    fun finishExternalSignInFailure(message: String) {
        isLoading = false
        errorMessage = message
    }

    fun cancelExternalSignIn() {
        isLoading = false
        errorMessage = null
    }

    suspend fun requestCode(phone: String): AuthRequestCodeResult {
        this.phone = phone
        errorMessage = null
        isLoading = true
        return authClient.requestCode(phone).also { result ->
            when (result) {
                is AuthRequestCodeResult.Success -> {
                    requestId = result.requestId
                    debugCode = result.debugCode
                    isLoading = false
                }

                is AuthRequestCodeResult.Failure -> {
                    isLoading = false
                    errorMessage = result.message
                }
            }
        }
    }

    suspend fun verifyCode(code: String): AuthVerifyCodeResult {
        errorMessage = null
        isLoading = true
        return authClient.verifyCode(requestId, code).also { result ->
            when (result) {
                is AuthVerifyCodeResult.Success -> {
                    prefs.userId = result.userId
                    prefs.userPhone = result.phone
                    prefs.sessionToken = result.sessionToken
                    debugCode = null
                    isLoading = false
                }

                is AuthVerifyCodeResult.Failure -> {
                    isLoading = false
                    errorMessage = result.message
                }
            }
        }
    }

    suspend fun signInWithProvider(identity: ProviderIdentity): AuthProviderSignInResult {
        errorMessage = null
        isLoading = true
        return authClient.signInWithProvider(identity).also { result ->
            when (result) {
                is AuthProviderSignInResult.Success -> {
                    prefs.userId = result.userId
                    prefs.userPhone = result.phone.orEmpty()
                    prefs.userEmail = result.email.orEmpty()
                    prefs.userDisplayName = result.displayName.orEmpty()
                    prefs.sessionToken = result.sessionToken
                    isLoading = false
                }

                is AuthProviderSignInResult.Failure -> {
                    isLoading = false
                    errorMessage = result.message
                }
            }
        }
    }

    suspend fun signInWithGoogle(): GoogleFirebaseSignInResult =
        googleAuthClient.signIn()
}
