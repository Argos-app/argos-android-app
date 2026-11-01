package com.android.argosapp.data.dto

import com.android.argosapp.model.AnimalExceptionRecord

data class AnimalExceptionDto(
    val id: String,
    val earTag: String,
    val currentLotCode: String?,
    val originLotCode: String?,
    val exceptionDate: String?,
    val slaughterReleaseDate: String?,
    val exceptionReason: String?,
    val status: String?,
    val fitForSlaughter: Boolean,
    val eligibleToReturn: Boolean,
    val returnFitnessPercentage: Int,
    val eligibilityTag: String?,
    val latestMedicine: String?,
    val latestMedicineWithdrawalEnd: String?,
) {
    fun toModel() = AnimalExceptionRecord(
        id = id,
        earTag = earTag,
        currentLotCode = currentLotCode,
        originLotCode = originLotCode,
        exceptionDate = exceptionDate,
        slaughterReleaseDate = slaughterReleaseDate,
        exceptionReason = exceptionReason,
        status = status,
        fitForSlaughter = fitForSlaughter,
        eligibleToReturn = eligibleToReturn,
        returnFitnessPercentage = returnFitnessPercentage,
        eligibilityTag = eligibilityTag,
        latestMedicine = latestMedicine,
        latestMedicineWithdrawalEnd = latestMedicineWithdrawalEnd,
    )
}

data class AnimalExceptionPageDto(
    val data: List<AnimalExceptionDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
)