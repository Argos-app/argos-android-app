package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.NotificationEntity
import com.android.argosapp.data.dto.ArgosNotification
import java.util.concurrent.TimeUnit

class NotificationStorage(context: Context) {
    private val dao = ArgosCacheDatabase.getInstance(context).notificationDao()

    suspend fun save(userId: String, notification: ArgosNotification) {
        val now = System.currentTimeMillis()
        dao.insert(NotificationEntity(
            userId = userId,
            title = notification.title,
            body = notification.body,
            type = notification.type,
            receivedAt = now,
        ))
        dao.deleteOlderThan(notificationCutoff(now))
    }

    suspend fun getRecent(userId: String): List<NotificationEntity> {
        val cutoff = notificationCutoff(System.currentTimeMillis())
        dao.deleteOlderThan(cutoff)
        return dao.getRecent(userId, cutoff)
    }
}

internal fun notificationCutoff(now: Long): Long = now - TimeUnit.DAYS.toMillis(30)
