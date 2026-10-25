package com.android.argosapp.dto

data class LotDto(
    val lotName: String,
    val openDate: String?,
    val releaseSlaughterDate: String?,
    val category: String?,
    val animalExceptionOutList: List<LotAnimalExceptionDto>?,
)

data class LotAnimalExceptionDto(
    val earCode: String?,
    val medicine: String?,
    val releaseSlaughterDate: String?,
)
