package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.android.argosapp.data.cache.entity.CachedMonthEntity
import com.android.argosapp.data.cache.entity.MonthApplicationEntity


@Dao
interface MonthApplicationsDao {
    @Query("SELECT * FROM cached_months WHERE month_key = :monthKey LIMIT 1")
    suspend fun getCachedMonth(monthKey: String): CachedMonthEntity?

    @Query("SELECT * FROM month_applications WHERE month_key = :monthKey ORDER BY applicationDate, lotName")
    suspend fun getApplications(monthKey: String): List<MonthApplicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedMonth(month: CachedMonthEntity)

    @Insert
    suspend fun saveApplications(applications: List<MonthApplicationEntity>)

    @Query("DELETE FROM month_applications WHERE month_key = :monthKey")
    suspend fun deleteApplications(monthKey: String)

    @Query("DELETE FROM cached_months WHERE month_key = :monthKey")
    suspend fun deleteCachedMonth(monthKey: String)

    @Query("DELETE FROM month_applications")
    suspend fun clearApplications()

    @Query("DELETE FROM cached_months")
    suspend fun clearCachedMonths()

    @Transaction
    suspend fun replaceMonth(month: CachedMonthEntity, applications: List<MonthApplicationEntity>) {
        deleteApplications(month.monthKey)
        deleteCachedMonth(month.monthKey)
        saveCachedMonth(month)
        if (applications.isNotEmpty()) saveApplications(applications)
    }

    @Transaction
    suspend fun clearAll() {
        clearApplications()
        clearCachedMonths()
    }
}
