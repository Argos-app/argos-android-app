package com.android.argosapp.data.dto

import java.util.UUID

/**
 * Contrato da resposta de autenticação da API Argos.
 */
data class ArgosApiAuthResponse(
    val bearerToken: String,
    val userName: String,
    val email: String,
    val farmName: String,
    val isFirstAccess: Boolean,
    val userId: UUID,
    val photoUrl: String?,
    val cpf: String?,
    val address: String?,
)
