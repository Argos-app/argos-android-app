package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.android.argosapp.data.cache.entity.LotCacheMetadata
import com.android.argosapp.data.cache.entity.LotAnimalExceptionEntity
import com.android.argosapp.data.cache.entity.LotEntity

@Dao
interface LotDao {
    @Query("SELECT * FROM lots ORDER BY lotName COLLATE NOCASE, rowId")
    suspend fun getAll(): List<LotEntity>

    @Query("SELECT lotName FROM lots ORDER BY lotName COLLATE NOCASE, rowId")
    suspend fun getNamesForSelect(): List<String>

    @Query("SELECT * FROM lot_animal_exceptions WHERE lotRowId = :lotRowId ORDER BY rowId")
    suspend fun getAnimalExceptions(lotRowId: Long): List<LotAnimalExceptionEntity>

    @Query("SELECT createdAt FROM lot_cache_metadata WHERE id = 1 LIMIT 1")
    suspend fun getCreatedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLots(items: List<LotEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimalExceptions(items: List<LotAnimalExceptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(item: LotCacheMetadata)

    @Query("DELETE FROM lots")
    suspend fun clearLots()

    @Query("DELETE FROM lot_animal_exceptions")
    suspend fun clearAnimalExceptions()

    @Transaction
    suspend fun replaceAll(
        lots: List<LotEntity>,
        animalsByLot: List<List<LotAnimalExceptionEntity>>,
        metadata: LotCacheMetadata,
    ) {
        clearAnimalExceptions()
        clearLots()
        val lotIds = if (lots.isEmpty()) emptyList() else insertLots(lots)
        val animals = lotIds.flatMapIndexed { index, lotId ->
            animalsByLot[index].map { it.copy(lotRowId = lotId) }
        }
        if (animals.isNotEmpty()) insertAnimalExceptions(animals)
        saveMetadata(metadata)
    }
}
