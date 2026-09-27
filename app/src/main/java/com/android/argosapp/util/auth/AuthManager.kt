package com.android.argosapp.util.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.android.argosapp.data.repository.ArgosApiRepository
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.User
import com.android.argosapp.util.session.UserSessionStorage
import com.android.argosapp.util.logging.AppLogger
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Coordena autenticação no Firebase, na API Argos e a sessão local do usuário. */
class AuthManager(
    private val argosApiRepository: ArgosApiRepository,
    private val firebaseRepository: FirebaseRepository,
    private val sessionStorage: UserSessionStorage,
) {
    init {
        AppLogger.info(
            AppLogger.AUTH_TAG,
            "auth_manager_initialized",
            "argos_repository" to argosApiRepository.javaClass.simpleName,
        )
    }

    suspend fun loginWithCredentials(email: String, password: String): String =
        loggedSuspend(AppLogger.AUTH_TAG, "auth_credentials") {
            firebaseRepository.loginWithCredentials(email, password)
        }

    fun launchGoogleSignIn(
        context: Context,
        webClientId: String,
        scope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        AppLogger.debug(AppLogger.AUTH_TAG, "auth_google_credential_request_started")
        try {
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
                    AppLogger.info(AppLogger.AUTH_TAG, "auth_google_credential_request_success")
                    onSuccess(credential.idToken)
                } catch (error: CancellationException) {
                    AppLogger.debug(AppLogger.AUTH_TAG, "auth_google_credential_request_cancelled")
                    throw error
                } catch (error: GetCredentialException) {
                    AppLogger.error(AppLogger.AUTH_TAG, "auth_google_credential_request_failure", error)
                    onError(error)
                } catch (error: Exception) {
                    AppLogger.error(AppLogger.AUTH_TAG, "auth_google_credential_request_failure", error)
                    onError(error)
                }
            }
        } catch (error: CancellationException) {
            AppLogger.debug(AppLogger.AUTH_TAG, "auth_google_credential_request_cancelled")
            throw error
        } catch (error: Exception) {
            AppLogger.error(AppLogger.AUTH_TAG, "auth_google_credential_request_failure", error)
            onError(error)
        }
    }

    suspend fun loginWithGoogle(idToken: String): String =
        loggedSuspend(AppLogger.AUTH_TAG, "auth_google_firebase") {
            firebaseRepository.loginWithGoogle(idToken)
        }

    suspend fun authenticate(firebaseIdToken: String, rememberMe: Boolean): ArgosApiAuthResponse =
        loggedSuspend(AppLogger.AUTH_TAG, "auth_argos") {
            argosApiRepository.authenticate(firebaseIdToken).also { response ->
                sessionStorage.updateSession(
                    bearerToken = response.bearerToken,
                    user = User(
                        userName = response.user.userName,
                        userEmail = response.user.userEmail,
                        farmName = response.user.farmName,
                    ),
                    rememberMe = rememberMe,
                )
                AppLogger.info(
                    AppLogger.AUTH_TAG,
                    "auth_argos_first_access_result",
                    "first_access" to response.isFirstAccess,
                )
            }
        }

    suspend fun hasValidSession(): Boolean {
        AppLogger.debug(AppLogger.SESSION_TAG, "session_check_started")
        return try {
            firebaseRepository.hasValidSession().also { isValid ->
                AppLogger.info(AppLogger.SESSION_TAG, "session_check_finished", "valid" to isValid)
            }
        } catch (error: CancellationException) {
            AppLogger.debug(AppLogger.SESSION_TAG, "session_check_cancelled")
            throw error
        } catch (error: Throwable) {
            AppLogger.error(AppLogger.SESSION_TAG, "session_check_failure", error)
            false
        }
    }

    suspend fun updatePassword(newPassword: String) =
        loggedSuspend(AppLogger.AUTH_TAG, "password_update") {
            firebaseRepository.updatePassword(newPassword)
        }

    suspend fun updateUserAccessStatus() =
        loggedSuspend(AppLogger.FIRST_ACCESS_TAG, "first_access_status_update") {
            argosApiRepository.updateUserAccessStatus(requireBearerToken())
        }

    suspend fun redefinePassword(email: String) =
        loggedSuspend(AppLogger.RESET_TAG, "password_reset_email_send") {
            firebaseRepository.redefinePassword(email)
        }

    suspend fun verifyPasswordResetCode(oobCode: String): String =
        loggedSuspend(AppLogger.RESET_TAG, "password_reset_code_verify") {
            firebaseRepository.verifyPasswordResetCode(oobCode)
        }

    suspend fun confirmPasswordReset(oobCode: String, newPassword: String) =
        loggedSuspend(AppLogger.RESET_TAG, "password_reset_confirm") {
            firebaseRepository.confirmPasswordReset(oobCode, newPassword)
        }

    fun extractPasswordResetCode(intent: Intent): String? =
        loggedSync(AppLogger.RESET_TAG, "password_reset_code_extract") {
            val data = intent.data
            data?.getQueryParameter("oobCode")
                ?: data?.getQueryParameter("link")
                    ?.let(Uri::parse)
                    ?.getQueryParameter("oobCode")
        }

    fun getBearerToken(): String? =
        loggedSync(AppLogger.SESSION_TAG, "session_bearer_read") {
            sessionStorage.getBearerToken().also {
                AppLogger.info(AppLogger.SESSION_TAG, "session_bearer_read_result", "has_bearer" to !it.isNullOrBlank())
            }
        }

    fun getUser(): User? =
        loggedSync(AppLogger.SESSION_TAG, "session_user_read") {
            sessionStorage.getUser().also {
                AppLogger.info(AppLogger.SESSION_TAG, "session_user_read_result", "has_user" to (it != null))
            }
        }

    fun isRememberMeEnabled(): Boolean =
        loggedSync(AppLogger.SESSION_TAG, "session_remember_me_read") {
            sessionStorage.isRememberMeEnabled().also {
                AppLogger.info(AppLogger.SESSION_TAG, "session_remember_me_read_result", "enabled" to it)
            }
        }

    fun clearSession() =
        loggedSync(AppLogger.SESSION_TAG, "session_clear") {
            sessionStorage.clearSession()
        }

    fun signOut() {
        loggedSync(AppLogger.SESSION_TAG, "session_sign_out") {
            firebaseRepository.signOut()
        }
    }

    private fun requireBearerToken(): String {
        return requireNotNull(sessionStorage.getBearerToken()?.takeIf { it.isNotBlank() }) {
            "A sessão do usuário não possui um bearer token válido."
        }
    }

    private suspend fun <T> loggedSuspend(
        tag: String,
        event: String,
        operation: suspend () -> T,
    ): T {
        AppLogger.debug(tag, "${event}_started")
        return try {
            operation().also { AppLogger.info(tag, "${event}_success") }
        } catch (error: CancellationException) {
            AppLogger.debug(tag, "${event}_cancelled")
            throw error
        } catch (error: Throwable) {
            AppLogger.error(tag, "${event}_failure", error)
            throw error
        }
    }

    private inline fun <T> loggedSync(
        tag: String,
        event: String,
        operation: () -> T,
    ): T {
        AppLogger.debug(tag, "${event}_started")
        return try {
            operation().also { AppLogger.info(tag, "${event}_success") }
        } catch (error: CancellationException) {
            AppLogger.debug(tag, "${event}_cancelled")
            throw error
        } catch (error: Throwable) {
            AppLogger.error(tag, "${event}_failure", error)
            throw error
        }
    }
}