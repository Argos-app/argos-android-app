package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "animal_exceptions")
data class AnimalExceptionEntity(
    @PrimaryKey val id: String,
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