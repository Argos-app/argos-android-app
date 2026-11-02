package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val lotName: String?,
    val lotCode: String?,
    val earCode: String?,
    val medicineName: String?,
    val applicationDate: String?,
    val applicationTime: String?,
    val dose: String?,
    val unit: String?,
    val route: String?,
    val headCountApplied: Int?,
    val headCount: Int?,
    val meatWithdrawalEnd: String?,
    val status: String?,
    val goal: String?,
    val notes: String?,
)

@Entity(tableName = "applications_cache_metadata")
data class ApplicationsCacheMetadata(
    @PrimaryKey val id: Int = CACHE_ID,
    val createdAt: Long,
) {
    companion object {
        const val CACHE_ID = 1
    }
}
