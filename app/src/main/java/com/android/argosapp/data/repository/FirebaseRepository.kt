package com.android.argosapp.data.repository

import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Abstrai as operações realizadas diretamente no Firebase Authentication.
 */
class FirebaseRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun loginWithCredentials(email: String, password: String): String {
        firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()

        return getCurrentIdToken()
    }

    suspend fun loginWithGoogle(idToken: String): String {
        firebaseAuth
            .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .await()

        return getCurrentIdToken()
    }

    suspend fun hasValidSession(): Boolean {
        if (firebaseAuth.currentUser == null) return false

        return runCatching {
            getCurrentIdToken().isNotBlank()
        }.getOrDefault(false)
    }

    suspend fun updatePassword(newPassword: String) {
        val currentUser = requireNotNull(firebaseAuth.currentUser) {
            "O Firebase não possui um usuário autenticado."
        }

        currentUser.updatePassword(newPassword).await()
    }

    suspend fun redefinePassword(email: String) {
        val settings = ActionCodeSettings.newBuilder()
            .setUrl(PASSWORD_RESET_CONTINUE_URL)
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.android.argosapp", false, null)
            .build()

        firebaseAuth.sendPasswordResetEmail(email, settings).await()
    }

    suspend fun verifyPasswordResetCode(oobCode: String): String {
        return firebaseAuth.verifyPasswordResetCode(oobCode).await()
    }

    suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        firebaseAuth.confirmPasswordReset(oobCode, newPassword).await()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    private suspend fun getCurrentIdToken(): String {
        val currentUser = requireNotNull(firebaseAuth.currentUser) {
            "O Firebase não possui um usuário autenticado."
        }

        return requireNotNull(currentUser.getIdToken(false).await()?.token?.takeIf { it.isNotBlank() }) {
            "O Firebase não retornou um ID token válido."
        }
    }

    private companion object {
        const val PASSWORD_RESET_CONTINUE_URL =
            "https://argos-app-a3c63.firebaseapp.com/redefinir-senha"
    }
}
