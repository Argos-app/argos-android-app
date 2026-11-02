package com.android.argosapp.data.cache.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "cached_months")
data class CachedMonthEntity(
    @PrimaryKey @ColumnInfo(name = "month_key") val monthKey: String,
    @ColumnInfo(name = "coverage_through") val coverageThrough: String? = null,
)

@Entity(
    tableName = "month_applications",
    indices = [Index(value = ["month_key"])],
)
data class MonthApplicationEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    @ColumnInfo(name = "month_key") val monthKey: String,
    val applicationId: String?,
    val userId: String?,
    val lotName: String?,
    val medicineName: String?,
    val applicationDate: String?,
    val meatWithdrawalEnd: String?,
    val daysFromReferenceDate: Long,
    val daysUntilSlaughter: Long,
)
