package com.android.argosapp.data.repository

data class AuthenticatedSession(
    val idToken: String,
)

/**
 * Repository que centraliza as operações de autenticação necessárias ao login.
 *
 * Suas responsabilidades são:
 * - receber as credenciais ou o token de identidade;
 * - delegar a autenticação ao provedor configurado;
 * - devolver o resultado da autenticação para o ViewModel;
 * - propagar falhas para que o ViewModel decida como apresentá-las na UI.
 *
 */
interface IAuthRepository {

    /** Autentica uma conta existente usando email e senha. */
    suspend fun login(email: String, password: String): AuthenticatedSession

    /** Autentica usando o ID token retornado pelo fluxo de login do Google. */
    suspend fun googleLogin(idToken: String): AuthenticatedSession
}
