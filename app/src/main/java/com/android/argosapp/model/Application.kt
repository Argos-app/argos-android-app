package com.android.argosapp.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Application(
    val lotName: String?,
    val lotCode: String?,
    val earCode: String?,
    val medicineName: String?,
    val applicationDate: LocalDateTime?,
    val applicationTime: LocalTime?,
    val dose: BigDecimal?,
    val unit: String?,
    val route: String?,
    val headCountApplied: Int?,
    val headCount: Int?,
    val meatWithdrawalEnd: LocalDate?,
    val status: String?,
    val goal: String?,
    val notes: String?,
)
