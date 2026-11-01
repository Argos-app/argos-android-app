package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "herds")
data class HerdEntity(
    @PrimaryKey val herdId: String,
    val propertyId: String,
    val herdName: String,
    val breed: String,
    val headCount: Int,
)
