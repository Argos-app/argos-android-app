package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.android.argosapp.data.cache.entity.HerdCacheMetadata
import com.android.argosapp.data.cache.entity.HerdEntity

@Dao
interface HerdDao {
    @Query("SELECT * FROM herds ORDER BY herdName COLLATE NOCASE, herdId")
    suspend fun getAll(): List<HerdEntity>

    @Query("SELECT createdAt FROM herd_cache_metadata WHERE id = 1 LIMIT 1")
    suspend fun getCreatedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(herds: List<HerdEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: HerdCacheMetadata)

    @Query("DELETE FROM herds")
    suspend fun clearHerds()

    @Query("DELETE FROM herd_cache_metadata")
    suspend fun clearMetadata()

    @Transaction
    suspend fun replaceAll(herds: List<HerdEntity>, metadata: HerdCacheMetadata) {
        clearHerds()
        if (herds.isNotEmpty()) insertAll(herds)
        saveMetadata(metadata)
    }

    @Transaction
    suspend fun clear() {
        clearHerds()
        clearMetadata()
    }
}
