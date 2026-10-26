package com.android.argosapp.data.dto

import com.android.argosapp.model.Application
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ListAllApplicationsDto(
    val lotName: String? = null,
    val lotCode: String? = null,
    val earCode: String? = null,
    val medicineName: String? = null,
    val applicationDate: String? = null,
    val applicationTime: String? = null,
    val dose: BigDecimal? = null,
    val unit: String? = null,
    val route: String? = null,
    val headCountApplied: Int? = null,
    val headCount: Int? = null,
    val meatWithdrawalEnd: String? = null,
    val status: String? = null,
    val goal: String? = null,
    val notes: String? = null,
) {
    fun toModel(): Application {
        return Application(
            lotName = lotName,
            lotCode = lotCode,
            earCode = earCode,
            medicineName = medicineName,
            applicationDate = applicationDate.toLocalDateTimeOrNull(),
            applicationTime = applicationTime.toLocalTimeOrNull(),
            dose = dose,
            unit = unit,
            route = route,
            headCountApplied = headCountApplied,
            headCount = headCount,
            meatWithdrawalEnd = meatWithdrawalEnd.toLocalDateOrNull(),
            status = status,
            goal = goal,
            notes = notes,
        )
    }
}

private fun String?.toLocalDateTimeOrNull(): LocalDateTime? {
    return this?.let { value ->
        runCatching { LocalDateTime.parse(value) }.getOrNull()
            ?: runCatching { LocalDate.parse(value).atStartOfDay() }.getOrNull()
    }
}

private fun String?.toLocalTimeOrNull(): LocalTime? {
    return this?.let { value -> runCatching { LocalTime.parse(value) }.getOrNull() }
}

private fun String?.toLocalDateOrNull(): LocalDate? {
    return this?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
}
