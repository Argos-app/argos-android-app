package com.android.argosapp.model

data class AnimalExceptionRecord(
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
)