package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.android.argosapp.data.cache.entity.AnimalExceptionCacheMetadata
import com.android.argosapp.data.cache.entity.AnimalExceptionEntity

@Dao
interface AnimalExceptionDao {
    @Query("SELECT * FROM animal_exceptions ORDER BY exceptionDate DESC, id")
    suspend fun getAll(): List<AnimalExceptionEntity>

    @Query("SELECT createdAt FROM animal_exception_cache_metadata WHERE id = 1 LIMIT 1")
    suspend fun getCreatedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AnimalExceptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(item: AnimalExceptionCacheMetadata)

    @Query("DELETE FROM animal_exceptions")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<AnimalExceptionEntity>, metadata: AnimalExceptionCacheMetadata) {
        clear()
        if (items.isNotEmpty()) insertAll(items)
        saveMetadata(metadata)
    }

    @Transaction
    suspend fun clearAll() {
        clear()
        clearMetadata()
    }

    @Query("DELETE FROM animal_exception_cache_metadata")
    suspend fun clearMetadata()
}