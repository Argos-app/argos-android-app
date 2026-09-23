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
import com.android.argosapp.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Centraliza as operações de autenticação e sessão do Firebase Auth.
 */
class FirebaseAuthManager(
    private val firebaseRepository: FirebaseRepository,
) {
    suspend fun loginWithCredentials(email: String, password: String): String {
        AppLogger.debug(AppLogger.AUTH_TAG, "auth_credentials_started")

        return try {
            firebaseRepository.loginWithCredentials(email, password).also {
                AppLogger.info(AppLogger.AUTH_TAG, "auth_credentials_success")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLogger.error(AppLogger.AUTH_TAG, "auth_credentials_failure", error)
            throw error
        }
    }

    fun launchGoogleSignIn(
        context: Context,
        webClientId: String,
        scope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        AppLogger.debug(AppLogger.AUTH_TAG, "auth_google_started")
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
                AppLogger.info(AppLogger.AUTH_TAG, "auth_google_success")
                onSuccess(credential.idToken)
            } catch (error: CancellationException) {
                throw error
            } catch (error: GetCredentialException) {
                AppLogger.error(AppLogger.AUTH_TAG, "auth_google_failure", error)
                onError(error)
            } catch (error: Exception) {
                AppLogger.error(AppLogger.AUTH_TAG, "auth_google_failure", error)
                onError(error)
            }
        }
    }

    suspend fun loginWithGoogle(idToken: String): String {
        AppLogger.debug(AppLogger.AUTH_TAG, "auth_google_firebase_started")

        return try {
            firebaseRepository.loginWithGoogle(idToken).also {
                AppLogger.info(AppLogger.AUTH_TAG, "auth_google_firebase_success")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLogger.error(AppLogger.AUTH_TAG, "auth_google_firebase_failure", error)
            throw error
        }
    }

    suspend fun hasValidSession(): Boolean {
        AppLogger.debug(AppLogger.SESSION_TAG, "session_check_started")

        return try {
            firebaseRepository.hasValidSession().also { isValid ->
                AppLogger.info(
                    AppLogger.SESSION_TAG,
                    "session_check_finished",
                    "valid" to isValid,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLogger.error(AppLogger.SESSION_TAG, "session_check_failure", error)
            false
        }
    }

    suspend fun updatePassword(newPassword: String) {
        AppLogger.debug(AppLogger.AUTH_TAG, "password_update_started")

        try {
            firebaseRepository.updatePassword(newPassword)
            AppLogger.info(AppLogger.AUTH_TAG, "password_update_success")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLogger.error(AppLogger.AUTH_TAG, "password_update_failure", error)
            throw error
        }
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
        AppLogger.info(AppLogger.SESSION_TAG, "session_signed_out")
    }
}
