package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.android.argosapp.data.cache.entity.DeviceEntity

@Dao
interface DeviceDao {
    @Query("SELECT deviceId FROM devices WHERE userId = :userId LIMIT 1")
    suspend fun getDeviceId(userId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(device: DeviceEntity)
}
