package com.study.argosandroidapp.mokup

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser

import com.study.argosandroidapp.data.repository.IAuthRepository

import io.mockk.mockk

/**
 * Implementação fake de [com.study.argosandroidapp.data.repository.IAuthRepository] para uso em testes unitários.
 *
 * Permite simular cenários de sucesso e falha sem depender do Firebase.
 *
 * Exemplo de uso:
 * ```
 * val repository = FakeAuthRepository()
 * repository.loginResult = Result.failure(Exception("Senha incorreta"))
 * val viewModel = AuthViewModel(repository)
 * ```
 */
class FirebaseAuthRepositoryMock : IAuthRepository {

    /** Resultado simulado para [login]. Padrão: sucesso com usuário mock. */
    var loginResult: Result<AuthResult> = Result.success(mockk(relaxed = true))

    /** Resultado simulado para [register]. Padrão: sucesso com usuário mock. */
    var registerResult: Result<AuthResult> = Result.success(mockk(relaxed = true))

    /** Resultado simulado para [googleLogin]. Padrão: sucesso com usuário mock. */
    var googleLoginResult: Result<AuthResult> = Result.success(mockk(relaxed = true))

    /** Usuário simulado retornado por [getCurrentUser]. Padrão: null (não logado). */
    var currentUser: FirebaseUser? = null

    /** @see IAuthRepository.login */
    override suspend fun login(email: String, senha: String): AuthResult {
        return loginResult.getOrThrow()
    }

    /** @see IAuthRepository.register */
    override suspend fun register(nome: String, email: String, senha: String): AuthResult {
        return registerResult.getOrThrow()
    }

    /** @see IAuthRepository.googleLogin */
    override suspend fun googleLogin(idToken: String): AuthResult {
        return googleLoginResult.getOrThrow()
    }

    /** @see IAuthRepository.logout */
    override suspend fun logout() { }

    /** @see IAuthRepository.getCurrentUser */
    override suspend fun getCurrentUser(): FirebaseUser? {
        return currentUser
    }
}