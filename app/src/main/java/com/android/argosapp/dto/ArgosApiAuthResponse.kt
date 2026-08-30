package com.android.argosapp.dto

import com.android.argosapp.model.User

/**
 * Contrato da resposta de autenticação da API Argos.
 */
data class ArgosApiAuthResponse(
    val bearerToken: String,
    val user: User,
    val isFirstAccess: Boolean,
)