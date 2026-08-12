package com.example.pythagoros.presentation.viewmodel

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

    suspend fun requestCode(phone: String): AuthRequestCodeResult =
        authClient.requestCode(phone)

    suspend fun verifyCode(requestId: String, code: String): AuthVerifyCodeResult =
        authClient.verifyCode(requestId, code)

    suspend fun signInWithProvider(identity: ProviderIdentity): AuthProviderSignInResult =
        authClient.signInWithProvider(identity)

    suspend fun signInWithGoogle(): GoogleFirebaseSignInResult =
        googleAuthClient.signIn()
}
