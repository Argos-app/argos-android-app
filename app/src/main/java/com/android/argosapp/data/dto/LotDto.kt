package com.android.argosapp.data.dto

import com.android.argosapp.model.Lot
import java.time.LocalDate

data class LotDto(
    val lotName: String,
    val openDate: String?,
    val releaseSlaughterDate: String?,
    val category: String?,
    val animalExceptionOutList: List<LotAnimalExceptionDto>?,
) {
    fun toModel(): Lot = Lot(
        lotName = lotName,
        openDate = openDate.toLocalDateOrNull(),
        releaseSlaughterDate = releaseSlaughterDate.toLocalDateOrNull(),
        category = category,
    )

    private fun String?.toLocalDateOrNull(): LocalDate? = this?.let { value ->
        runCatching { LocalDate.parse(value) }.getOrNull()
    }
}

data class LotAnimalExceptionDto(
    val earCode: String?,
    val medicine: String?,
    val releaseSlaughterDate: String?,
)
