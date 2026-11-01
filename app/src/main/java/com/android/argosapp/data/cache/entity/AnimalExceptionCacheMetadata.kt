package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "animal_exception_cache_metadata")
data class AnimalExceptionCacheMetadata(
    @PrimaryKey val id: Int = 1,
    val createdAt: Long,
)