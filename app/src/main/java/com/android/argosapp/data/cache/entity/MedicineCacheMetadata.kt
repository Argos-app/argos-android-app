package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "medicine_cache_metadata")
data class MedicineCacheMetadata(
    @PrimaryKey val id: Int = 1,
    val createdAt: Long,
    val nextPage: Int,
    val isLastPage: Boolean,
)
