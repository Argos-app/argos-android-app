package com.study.argosandroidapp.data.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser

/**
 * Contrato de autenticação do aplicativo.
 *
 * Define as operações de autenticação suportadas, abstraindo a implementação
 * do provedor de identidade (Firebase Auth, mock, etc).
 */
interface IAuthRepository {

    /**
     * Autentica um usuário existente com email e senha.
     *
     * @param email Endereço de email cadastrado.
     * @param senha Senha do usuário.
     * @return [AuthResult] com os dados da sessão autenticada.
     * @throws com.google.firebase.auth.FirebaseAuthInvalidUserException Se o email não estiver cadastrado.
     * @throws com.google.firebase.auth.FirebaseAuthInvalidCredentialsException Se a senha estiver incorreta.
     */
    suspend fun login(email: String, senha: String): AuthResult

    /**
     * Registra uma nova conta com email e senha e atualiza o nome de exibição do usuário.
     *
     * @param nome Nome de exibição do usuário.
     * @param email Endereço de email para cadastro.
     * @param senha Senha desejada (mínimo 6 caracteres).
     * @return [AuthResult] com os dados da sessão autenticada.
     * @throws com.google.firebase.auth.FirebaseAuthWeakPasswordException Se a senha for fraca.
     * @throws com.google.firebase.auth.FirebaseAuthUserCollisionException Se o email já estiver cadastrado.
     */
    suspend fun register(nome: String, email: String, senha: String): AuthResult

    /**
     * Autentica um usuário via Google Sign-In.
     *
     * @param idToken Token de identificação retornado pelo Google Sign-In.
     * @return [AuthResult] com os dados da sessão autenticada.
     * @throws com.google.firebase.auth.FirebaseAuthInvalidCredentialsException Se o token for inválido.
     */
    suspend fun googleLogin(idToken: String): AuthResult

    /**
     * Encerra a sessão do usuário autenticado.
     */
    suspend fun logout()

    /**
     * Retorna o usuário atualmente autenticado, ou null se não houver sessão ativa.
     *
     * @return [FirebaseUser] com os dados do usuário, ou null.
     */
    suspend fun getCurrentUser(): FirebaseUser?

}