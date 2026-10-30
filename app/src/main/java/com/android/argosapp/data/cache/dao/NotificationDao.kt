package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.android.argosapp.data.cache.entity.NotificationEntity

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE receivedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT * FROM notifications WHERE userId = :userId AND receivedAt >= :cutoff ORDER BY receivedAt DESC, id DESC")
    suspend fun getRecent(userId: String, cutoff: Long): List<NotificationEntity>
}
