package com.android.argosapp.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

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
) {
    fun elapsedReturnPercentage(today: LocalDate): Int? {
        val start = exceptionDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val end = slaughterReleaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        if (end.isBefore(start)) return null
        val totalDays = ChronoUnit.DAYS.between(start, end)
        if (totalDays == 0L) return if (today.isBefore(start)) 0 else 100
        val elapsedDays = ChronoUnit.DAYS.between(start, today).coerceIn(0, totalDays)
        return (elapsedDays.toDouble() * 100 / totalDays).roundToInt()
    }
}
