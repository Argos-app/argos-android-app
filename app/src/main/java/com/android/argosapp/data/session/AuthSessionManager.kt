package com.android.argosapp.data.session

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/** Centraliza a validação e a limpeza da sessão mantida pelo Firebase Auth. */
interface AuthSessionManager {
    suspend fun hasValidSession(): Boolean

    fun clear()
}

class FirebaseAuthSessionManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
): AuthSessionManager {

    override suspend fun hasValidSession(): Boolean {
        val currentUser = firebaseAuth.currentUser ?: return false

        return runCatching {
            currentUser.getIdToken(false).await()?.token?.isNotBlank() == true
        }.getOrDefault(false)
    }

    override fun clear() {
        firebaseAuth.signOut()
    }
}
