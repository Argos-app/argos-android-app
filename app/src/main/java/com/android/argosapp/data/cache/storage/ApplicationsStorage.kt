package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.dao.ApplicationsDao
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.ApplicationEntity
import com.android.argosapp.data.cache.entity.ApplicationsCacheMetadata
import com.android.argosapp.model.Application
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ApplicationsStorage(context: Context) {
    private val dao: ApplicationsDao = ArgosCacheDatabase.getInstance(context).applicationsDao()

    suspend fun listAll(textFilter: String?, statusFilter: String?): List<Application> {
        return dao.listAll(textFilter, statusFilter)
            .map { entity -> entity.toModel() }
    }

    suspend fun getCreatedAt(): Long? {
        return dao.getCreatedAt()
    }

    suspend fun replaceAll(applications: List<Application>) {
        dao.replaceAll(
            applications = applications.map { application -> application.toEntity() },
            metadata = ApplicationsCacheMetadata(createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun clear() {
        dao.clearAll()
    }

    private fun ApplicationEntity.toModel(): Application {
        return Application(
            lotName = lotName,
            lotCode = lotCode,
            earCode = earCode,
            medicineName = medicineName,
            applicationDate = applicationDate.toLocalDateTimeOrNull(),
            applicationTime = applicationTime.toLocalTimeOrNull(),
            dose = dose.toBigDecimalOrNull(),
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

    private fun Application.toEntity(): ApplicationEntity {
        return ApplicationEntity(
            lotName = lotName,
            lotCode = lotCode,
            earCode = earCode,
            medicineName = medicineName,
            applicationDate = applicationDate?.toString(),
            applicationTime = applicationTime?.toString(),
            dose = dose?.toPlainString(),
            unit = unit,
            route = route,
            headCountApplied = headCountApplied,
            headCount = headCount,
            meatWithdrawalEnd = meatWithdrawalEnd?.toString(),
            status = status,
            goal = goal,
            notes = notes,
        )
    }

    private fun String?.toLocalDateTimeOrNull(): LocalDateTime? {
        return this?.let { value -> runCatching { LocalDateTime.parse(value) }.getOrNull() }
    }

    private fun String?.toLocalTimeOrNull(): LocalTime? {
        return this?.let { value -> runCatching { LocalTime.parse(value) }.getOrNull() }
    }

    private fun String?.toLocalDateOrNull(): LocalDate? {
        return this?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
    }

    private fun String?.toBigDecimalOrNull(): BigDecimal? {
        return this?.let { value -> runCatching { BigDecimal(value) }.getOrNull() }
    }
}
