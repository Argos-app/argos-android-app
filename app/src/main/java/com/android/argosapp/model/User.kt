package com.android.argosapp.model

/**
 * DataClass que encapsula o acesso aos dados do usuário.
 */
data class User (
    val userName: String,
    val userEmail: String,
    val farmName: String
)