package com.android.argosapp.model

import java.time.LocalDate

data class Lot(
    val lotName: String,
    val openDate: LocalDate?,
    val releaseSlaughterDate: LocalDate?,
    val category: String?,
    val animalExceptions: List<LotAnimalException>,
    val headCount: Int? = null,
    val lotId: String? = null,
) {
    fun canSlaughter(today: LocalDate): Boolean =
        releaseSlaughterDate?.let { !it.isAfter(today) } ?: false
}

data class LotAnimalException(
    val earCode: String?,
    val medicine: String?,
    val releaseSlaughterDate: LocalDate?,
)
