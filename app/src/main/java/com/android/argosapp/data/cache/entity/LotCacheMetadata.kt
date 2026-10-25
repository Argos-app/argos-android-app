package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "lot_cache_metadata")
data class LotCacheMetadata(
    @PrimaryKey val id: Int = 1,
    val createdAt: Long,
)
