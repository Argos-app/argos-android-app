package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.android.argosapp.data.cache.entity.MedicineCacheMetadata
import com.android.argosapp.data.cache.entity.MedicineEntity

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY pageIndex, medicineName COLLATE NOCASE")
    suspend fun getAll(): List<MedicineEntity>

    @Query("SELECT medicineName FROM medicines ORDER BY pageIndex, medicineName COLLATE NOCASE")
    suspend fun getNamesForSelect(): List<String>

    @Query("SELECT * FROM medicine_cache_metadata WHERE id = 1 LIMIT 1")
    suspend fun getPaginationState(): MedicineCacheMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MedicineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(item: MedicineCacheMetadata)

    @Query("DELETE FROM medicines")
    suspend fun clear()

    @Transaction
    suspend fun replaceFirstPage(items: List<MedicineEntity>, metadata: MedicineCacheMetadata) {
        clear()
        if (items.isNotEmpty()) insertAll(items)
        saveMetadata(metadata)
    }

    @Transaction
    suspend fun appendPage(items: List<MedicineEntity>, metadata: MedicineCacheMetadata) {
        if (items.isNotEmpty()) insertAll(items)
        saveMetadata(metadata)
    }
}
