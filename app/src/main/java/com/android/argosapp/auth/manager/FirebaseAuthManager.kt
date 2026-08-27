package com.android.argosapp.auth.manager

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Centraliza as operações de autenticação e sessão do Firebase Auth. */
class FirebaseAuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    suspend fun loginWithCredentials(email: String, password: String): String {
        firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()

        return getCurrentIdToken()
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
}
