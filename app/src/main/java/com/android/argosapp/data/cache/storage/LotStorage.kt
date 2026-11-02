package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.dao.LotDao
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.LotAnimalExceptionEntity
import com.android.argosapp.data.cache.entity.LotCacheMetadata
import com.android.argosapp.data.cache.entity.LotEntity
import com.android.argosapp.data.dto.LotAnimalExceptionDto
import com.android.argosapp.data.dto.LotDto

class LotStorage(context: Context) {
    private val dao: LotDao = ArgosCacheDatabase.getInstance(context).lotDao()

    suspend fun getAll(): List<LotDto> = dao.getAll().map { entity ->
        LotDto(
            lotName = entity.lotName,
            lotId = entity.lotId,
            openDate = entity.openDate,
            releaseSlaughterDate = entity.releaseSlaughterDate,
            category = entity.category,
            headCount = entity.headCount,
            animalExceptionOutList = if (entity.animalExceptionOutListIsNull) null else {
                dao.getAnimalExceptions(entity.rowId).map { animal ->
                    LotAnimalExceptionDto(
                        earCode = animal.earCode,
                        medicine = animal.medicine,
                        releaseSlaughterDate = animal.releaseSlaughterDate,
                    )
                }
            },
        )
    }

    suspend fun getNamesForSelect(): List<String> = dao.getNamesForSelect()
    suspend fun getCreatedAt(): Long? = dao.getCreatedAt()
    suspend fun invalidateCache() = dao.invalidateCache()
    suspend fun deleteById(lotId: String) = dao.deleteById(lotId)

    suspend fun replaceAll(items: List<LotDto>) {
        val lots = items.map { lot ->
            LotEntity(
                lotName = lot.lotName,
                lotId = lot.lotId,
                openDate = lot.openDate,
                releaseSlaughterDate = lot.releaseSlaughterDate,
                category = lot.category,
                headCount = lot.headCount,
                animalExceptionOutListIsNull = lot.animalExceptionOutList == null,
            )
        }
        val animalsByLot = items.map { lot ->
            lot.animalExceptionOutList.orEmpty().map { animal ->
                LotAnimalExceptionEntity(
                    lotRowId = 0,
                    earCode = animal.earCode,
                    medicine = animal.medicine,
                    releaseSlaughterDate = animal.releaseSlaughterDate,
                )
            }
        }
        dao.replaceAll(lots, animalsByLot, LotCacheMetadata(createdAt = System.currentTimeMillis()))
    }
}
