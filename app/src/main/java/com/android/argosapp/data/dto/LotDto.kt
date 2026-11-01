package com.android.argosapp.data.dto

import com.android.argosapp.model.Lot
import com.android.argosapp.model.LotAnimalException
import java.time.LocalDate

data class LotDto(
    val lotName: String,
    val openDate: String?,
    val releaseSlaughterDate: String?,
    val category: String?,
    val animalExceptionOutList: List<LotAnimalExceptionDto>?,
    val lotId: String? = null,
    val headCount: Int? = null,
) {
    fun toModel(): Lot = Lot(
        lotName = lotName,
        lotId = lotId,
        openDate = openDate.toLocalDateOrNull(),
        releaseSlaughterDate = releaseSlaughterDate.toLocalDateOrNull(),
        category = category,
        headCount = headCount,
        animalExceptions = animalExceptionOutList.orEmpty().map { animal ->
            LotAnimalException(
                earCode = animal.earCode,
                medicine = animal.medicine,
                releaseSlaughterDate = animal.releaseSlaughterDate.toLocalDateOrNull(),
            )
        },
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
