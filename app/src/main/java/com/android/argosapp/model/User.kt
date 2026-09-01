package com.android.argosapp.model

/**
 * Contrato que encapsula o acesso aos dados do usuário.
 */
data class User (
    val userName: String,
    val userEmail: String,
    val farmName: String
)