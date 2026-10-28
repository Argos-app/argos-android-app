package com.android.argosapp.data.dto

data class RegisterLotDto(
    val lotName: String,
    val herdName: String,
    val category: String,
    val headCount: Int,
    val releaseSlaughterDate: String,
)
