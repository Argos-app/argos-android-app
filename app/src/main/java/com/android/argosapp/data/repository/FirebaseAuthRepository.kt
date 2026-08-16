package com.android.argosapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Repository que centraliza a comunicação de autenticação com o Firebase Auth.
 *
 * Esta classe transforma as operações do contrato [IAuthRepository] em chamadas
 * assíncronas do Firebase e aguarda seus resultados sem bloquear a thread da UI.
 * Exceções do Firebase são mantidas para que o ViewModel possa convertê-las em
 * estado e mensagens adequadas para a tela.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) : IAuthRepository {

    override suspend fun login(email: String, password: String): AuthenticatedSession {
        val result = firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()
        return toAuthenticatedSession(result)
    }

    override suspend fun googleLogin(idToken: String): AuthenticatedSession {
        val result = firebaseAuth
            .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .await()
        return toAuthenticatedSession(result)
    }

    private suspend fun toAuthenticatedSession(result: AuthResult): AuthenticatedSession {
        val idToken = requireNotNull(result.user?.getIdToken(false)?.await()?.token) {
            "O Firebase não retornou um ID token válido."
        }
        return AuthenticatedSession(idToken = idToken)
    }
}
