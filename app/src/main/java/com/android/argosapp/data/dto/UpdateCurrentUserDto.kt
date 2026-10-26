package com.android.argosapp.dto

data class UpdateCurrentUserDto(
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val address: String? = null,
)
