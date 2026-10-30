package com.android.argosapp.model

import java.util.UUID

/**
 * Contrato que encapsula o acesso aos dados do usuário.
 */
data class User (
    val userName: String,
    val email: String,
    val farmName: String,
    val userId: UUID,
    val photoUrl: String?,
    val cpf: String?,
    val address: String?,
    val role: String?
)
