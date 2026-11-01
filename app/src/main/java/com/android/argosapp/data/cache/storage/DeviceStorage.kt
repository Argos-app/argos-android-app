package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.DeviceEntity

class DeviceStorage(context: Context) {
    private val dao = ArgosCacheDatabase.getInstance(context).deviceDao()

    suspend fun getDeviceId(userId: String): String? = dao.getDeviceId(userId)

    suspend fun saveDeviceId(userId: String, deviceId: String) {
        dao.save(DeviceEntity(userId, deviceId))
    }
}
