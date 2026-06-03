package com.study.argosandroidapp.data.repository

import com.google.firebase.auth.*
import com.google.firebase.auth.FirebaseAuth

import kotlinx.coroutines.tasks.await

/**
 * Implementação de [IAuthRepository] utilizando Firebase Authentication.
 *
 * @property firebaseAuth Instância do [FirebaseAuth].
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : IAuthRepository {

    /**
     * @see IAuthRepository.login
     * @throws FirebaseAuthInvalidUserException Se o email não estiver cadastrado.
     * @throws FirebaseAuthInvalidCredentialsException Se a senha estiver incorreta.
     */
    override suspend fun login(email: String, senha: String): AuthResult {
        return firebaseAuth
            .signInWithEmailAndPassword(email, senha)
            .await()
    }

    /**
     * @see IAuthRepository.register
     * @throws FirebaseAuthWeakPasswordException Se a senha for fraca.
     * @throws FirebaseAuthUserCollisionException Se o email já estiver cadastrado.
     */
    override suspend fun register(nome: String, email: String, senha: String): AuthResult {
        val result = firebaseAuth
            .createUserWithEmailAndPassword(email, senha)
            .await()

        result.user!!.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(nome)
                .build()
        ).await()

        return result
    }

    /**
     * @see IAuthRepository.googleLogin
     * @throws FirebaseAuthInvalidCredentialsException Se o token do Google for inválido.
     */
    override suspend fun googleLogin(idToken: String): AuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return firebaseAuth
            .signInWithCredential(credential)
            .await()
    }

    /** @see IAuthRepository.logout */
    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    /** @see IAuthRepository.getCurrentUser */
    override suspend fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}