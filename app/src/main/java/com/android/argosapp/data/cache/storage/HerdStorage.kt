package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.HerdCacheMetadata
import com.android.argosapp.data.cache.entity.HerdEntity
import com.android.argosapp.model.Herd

class HerdStorage(context: Context) {
    private val dao = ArgosCacheDatabase.getInstance(context).herdDao()

    suspend fun getAll(): List<Herd> = dao.getAll().map {
        Herd(it.herdId, it.propertyId, it.herdName, it.breed, it.headCount)
    }

    suspend fun getCreatedAt(): Long? = dao.getCreatedAt()

    suspend fun replaceAll(herds: List<Herd>) {
        dao.replaceAll(
            herds.map { HerdEntity(it.herdId, it.propertyId, it.herdName, it.breed, it.headCount) },
            HerdCacheMetadata(createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun clear() = dao.clear()
}
