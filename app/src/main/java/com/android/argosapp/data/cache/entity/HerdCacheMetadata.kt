package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "herd_cache_metadata")
data class HerdCacheMetadata(
    @PrimaryKey val id: Int = 1,
    val createdAt: Long,
)
