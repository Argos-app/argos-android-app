package com.android.argosapp.dto

/**
 * Contrato da resposta de autenticação da API Argos.
 */
data class ArgosApiAuthResponse(
    val bearerToken: String,
    val userName: String,
    val email: String,
    val farmName: String,
    val isFirstAccess: Boolean,
)