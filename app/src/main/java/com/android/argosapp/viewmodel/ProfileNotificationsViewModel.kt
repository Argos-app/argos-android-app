package com.android.argosapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.android.argosapp.data.cache.storage.NotificationStorage
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.model.NotificationRecord

class ProfileNotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = NotificationStorage(application)
    private val users = UserStorage(application)
    suspend fun load(): List<NotificationRecord> {
        val userId = requireNotNull(users.getUser()?.userId) { "User is unavailable." }
        return storage.getRecent(userId.toString()).map {
            NotificationRecord(it.title, it.body, it.receivedAt)
        }
    }
}
