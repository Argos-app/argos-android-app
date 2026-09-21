package com.android.argosapp.auth.manager

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.android.argosapp.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Centraliza as operações de autenticação e sessão do Firebase Auth.
 */
class FirebaseAuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    suspend fun loginWithCredentials(email: String, password: String): String {
        AppLogger.debug(AppLogger.AUTH_TAG, "auth_credentials_started")

        return try {
            firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            getCurrentIdToken().also {
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
            firebaseAuth
                .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .await()

            getCurrentIdToken().also {
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
        if (firebaseAuth.currentUser == null) {
            AppLogger.debug(AppLogger.SESSION_TAG, "session_check_empty")
            return false
        }

        AppLogger.debug(AppLogger.SESSION_TAG, "session_check_started")

        return try {
            getCurrentIdToken().isNotBlank().also { isValid ->
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
        val currentUser = requireNotNull(firebaseAuth.currentUser) {
            "O Firebase não possui um usuário autenticado."
        }

        AppLogger.debug(AppLogger.AUTH_TAG, "password_update_started")

        try {
            currentUser.updatePassword(newPassword).await()
            AppLogger.info(AppLogger.AUTH_TAG, "password_update_success")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLogger.error(AppLogger.AUTH_TAG, "password_update_failure", error)
            throw error
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        AppLogger.info(AppLogger.SESSION_TAG, "session_signed_out")
    }

    private suspend fun getCurrentIdToken(): String {
        val currentUser = requireNotNull(firebaseAuth.currentUser) {
            "O Firebase não possui um usuário autenticado."
        }

        return requireNotNull(currentUser.getIdToken(false).await()?.token?.takeIf { it.isNotBlank() }) {
            "O Firebase não retornou um ID token válido."
        }
    }
}
