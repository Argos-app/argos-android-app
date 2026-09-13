package com.android.argosapp.auth.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.android.argosapp.data.api.FirebaseRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Centraliza as operações de autenticação e sessão do Firebase Auth.
 */
class FirebaseAuthManager(
    private val firebaseRepository: FirebaseRepository,
) {
    suspend fun loginWithCredentials(email: String, password: String): String {
        return firebaseRepository.loginWithCredentials(email, password)
    }

    fun launchGoogleSignIn(
        context: Context,
        webClientId: String,
        scope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                onSuccess(credential.idToken)
            } catch (error: GetCredentialException) {
                onError(error)
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    suspend fun loginWithGoogle(idToken: String): String {
        return firebaseRepository.loginWithGoogle(idToken)
    }

    suspend fun hasValidSession(): Boolean {
        return firebaseRepository.hasValidSession()
    }

    suspend fun updatePassword(newPassword: String) {
        firebaseRepository.updatePassword(newPassword)
    }

    suspend fun redefinePassword(email: String) {
        firebaseRepository.redefinePassword(email)
    }

    suspend fun verifyPasswordResetCode(oobCode: String): String {
        return firebaseRepository.verifyPasswordResetCode(oobCode)
    }

    suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        firebaseRepository.confirmPasswordReset(oobCode, newPassword)
    }

    fun extractPasswordResetCode(intent: Intent): String? {
        val data = intent.data ?: return null

        return data.getQueryParameter("oobCode")
            ?: data.getQueryParameter("link")
                ?.let(Uri::parse)
                ?.getQueryParameter("oobCode")
    }

    fun signOut() {
        firebaseRepository.signOut()
    }
}
