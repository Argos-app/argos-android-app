package com.android.argosapp.dto

import com.android.argosapp.model.ApplicationWithdrawal
import java.time.LocalDate

data class ApplicationWithdrawalDto(
    val applicationId: String? = null,
    val userId: String? = null,
    val lotName: String? = null,
    val medicineName: String? = null,
    val applicationDate: String? = null,
    val meatWithdrawalEnd: String? = null,
    val daysFromReferenceDate: Long = 0,
    val daysUntilSlaughter: Long = 0,
) {
    fun toModel() = ApplicationWithdrawal(
        applicationId = applicationId,
        userId = userId,
        lotName = lotName,
        medicineName = medicineName,
        applicationDate = applicationDate.toLocalDateOrNull(),
        meatWithdrawalEnd = meatWithdrawalEnd.toLocalDateOrNull(),
        daysFromReferenceDate = daysFromReferenceDate,
        daysUntilSlaughter = daysUntilSlaughter,
    )
}

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
