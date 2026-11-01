package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val userId: String,
    val deviceId: String,
)
