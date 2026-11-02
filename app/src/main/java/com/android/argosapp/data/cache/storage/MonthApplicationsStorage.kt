package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.CachedMonthEntity
import com.android.argosapp.data.cache.entity.MonthApplicationEntity
import com.android.argosapp.model.ApplicationWithdrawal
import java.time.LocalDate
import java.time.YearMonth

data class MonthApplicationsCache(
    val applications: List<ApplicationWithdrawal>,
    val coveredThrough: LocalDate?,
)

class MonthApplicationsStorage(context: Context) {
    private val dao = ArgosCacheDatabase.getInstance(context).monthApplicationsDao()

    suspend fun getMonth(month: YearMonth): MonthApplicationsCache? {
        val monthKey = cacheKey(month)
        val cachedMonth = dao.getCachedMonth(monthKey) ?: return null
        return MonthApplicationsCache(
            applications = dao.getApplications(monthKey).map { it.toModel() },
            coveredThrough = cachedMonth.coverageThrough.toLocalDateOrNull(),
        )
    }

    suspend fun saveMonth(
        month: YearMonth,
        coveredThrough: LocalDate,
        applications: List<ApplicationWithdrawal>,
    ) {
        val monthKey = cacheKey(month)
        dao.replaceMonth(
            month = CachedMonthEntity(monthKey, coveredThrough.toString()),
            applications = applications.map { application ->
                application.toEntity(monthKey)
            },
        )
    }

    suspend fun appendMonth(
        month: YearMonth,
        coveredThrough: LocalDate,
        newApplications: List<ApplicationWithdrawal>,
    ): List<ApplicationWithdrawal> {
        val existing = getMonth(month)
        val existingCoverage = existing?.coveredThrough
            ?.takeIf { YearMonth.from(it) == month }
        val mergedApplications = if (existingCoverage == null) {
            newApplications
        } else {
            (existing.applications + newApplications)
                .associateBy { it.cacheIdentity() }
                .values
                .toList()
        }
        saveMonth(month, coveredThrough, mergedApplications)
        return mergedApplications
    }

    suspend fun clear() = dao.clearAll()

    private fun cacheKey(month: YearMonth) = "$CACHE_FORMAT_VERSION:$month"

    private fun MonthApplicationEntity.toModel() = ApplicationWithdrawal(
        applicationId = applicationId,
        userId = userId,
        lotName = lotName,
        medicineName = medicineName,
        applicationDate = applicationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        meatWithdrawalEnd = meatWithdrawalEnd?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        daysFromReferenceDate = daysFromReferenceDate,
        daysUntilSlaughter = daysUntilSlaughter,
    )

    private fun ApplicationWithdrawal.toEntity(monthKey: String) = MonthApplicationEntity(
        monthKey = monthKey,
        applicationId = applicationId,
        userId = userId,
        lotName = lotName,
        medicineName = medicineName,
        applicationDate = applicationDate?.toString(),
        meatWithdrawalEnd = meatWithdrawalEnd?.toString(),
        daysFromReferenceDate = daysFromReferenceDate,
        daysUntilSlaughter = daysUntilSlaughter,
    )

    private fun ApplicationWithdrawal.cacheIdentity(): String = applicationId?.let { "id:$it" }
        ?: listOf(userId, lotName, medicineName, applicationDate, meatWithdrawalEnd).joinToString("|")

    private fun String?.toLocalDateOrNull(): LocalDate? =
        this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private companion object {
        const val CACHE_FORMAT_VERSION = 2
    }
}
