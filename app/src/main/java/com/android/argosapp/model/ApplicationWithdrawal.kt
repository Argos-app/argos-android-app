package com.android.argosapp.model

import java.time.LocalDate

data class ApplicationWithdrawal(
    val applicationId: String?,
    val userId: String?,
    val lotName: String?,
    val medicineName: String?,
    val applicationDate: LocalDate?,
    val meatWithdrawalEnd: LocalDate?,
    val daysFromReferenceDate: Long,
    val daysUntilSlaughter: Long,
)
