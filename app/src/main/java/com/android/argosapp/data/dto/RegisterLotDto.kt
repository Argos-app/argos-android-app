package com.android.argosapp.data.dto

data class RegisterLotDto(
    val lotName: String,
    val herdId: String? = null,
    val herdName: String,
    val breed: String? = null,
    val category: String,
    val headCount: Int,
    val releaseSlaughterDate: String,
)
