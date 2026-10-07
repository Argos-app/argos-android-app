package com.android.argosapp.data.repository

import com.android.argosapp.util.logging.RepositoryAppLogger
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Abstrai as operações realizadas diretamente no Firebase Authentication.
 */
class FirebaseRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val logger = RepositoryAppLogger("FirebaseRepository")

    init {
        logger.info("repository_initialized")
    }

    suspend fun loginWithCredentials(email: String, password: String): String =
        logger.runSuspending("login_with_credentials") {
            firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            getCurrentIdToken()
        }

    suspend fun loginWithGoogle(idToken: String): String =
        logger.runSuspending("login_with_google") {
            firebaseAuth
                .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .await()

            getCurrentIdToken()
        }

    suspend fun hasValidSession(): Boolean = logger.runSuspending("has_valid_session") {
        if (firebaseAuth.currentUser == null) {
            logger.info("has_valid_session_no_firebase_user")
            false
        } else {
            try {
                getCurrentIdToken().isNotBlank()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logger.error("has_valid_session_token_check_failed", error)
                false
            }
        }
    }.also {
        logger.info("has_valid_session_result", "valid" to it)
    }

    suspend fun updatePassword(newPassword: String): Unit =
        logger.runSuspending("update_password") {
            val currentUser = requireNotNull(firebaseAuth.currentUser) {
                "O Firebase não possui um usuário autenticado."
            }

            currentUser.updatePassword(newPassword).await()
        }

    suspend fun redefinePassword(email: String): Unit =
        logger.runSuspending("send_password_reset_email") {
            val settings = ActionCodeSettings.newBuilder()
                .setUrl(PASSWORD_RESET_CONTINUE_URL)
                .setHandleCodeInApp(true)
                .setAndroidPackageName("com.android.argosapp", false, null)
                .build()

            firebaseAuth.sendPasswordResetEmail(email, settings).await()
        }

    suspend fun verifyPasswordResetCode(oobCode: String): String =
        logger.runSuspending("verify_password_reset_code") {
            firebaseAuth.verifyPasswordResetCode(oobCode).await()
        }

    suspend fun confirmPasswordReset(oobCode: String, newPassword: String): Unit =
        logger.runSuspending("confirm_password_reset") {
            firebaseAuth.confirmPasswordReset(oobCode, newPassword).await()
        }

    fun signOut(): Unit = logger.runSynchronous("sign_out") { firebaseAuth.signOut() }

    private suspend fun getCurrentIdToken(): String = logger.runSuspending("get_id_token") {
        val currentUser = requireNotNull(firebaseAuth.currentUser) {
            "O Firebase não possui um usuário autenticado."
        }

        requireNotNull(currentUser.getIdToken(false).await()?.token?.takeIf { it.isNotBlank() }) {
            "O Firebase não retornou um ID token válido."
        }
    }

    private companion object {
        const val PASSWORD_RESET_CONTINUE_URL =
            "https://argos-app-a3c63.firebaseapp.com/redefinir-senha"
    }
}
