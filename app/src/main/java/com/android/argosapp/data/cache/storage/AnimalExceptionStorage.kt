package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.dao.AnimalExceptionDao
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.AnimalExceptionCacheMetadata
import com.android.argosapp.data.cache.entity.AnimalExceptionEntity
import com.android.argosapp.model.AnimalExceptionRecord

class AnimalExceptionStorage(context: Context) {
    private val dao: AnimalExceptionDao = ArgosCacheDatabase.getInstance(context).animalExceptionDao()

    suspend fun getAll(): List<AnimalExceptionRecord> = dao.getAll().map { entity ->
        AnimalExceptionRecord(
            id = entity.id,
            earTag = entity.earTag,
            currentLotCode = entity.currentLotCode,
            originLotCode = entity.originLotCode,
            exceptionDate = entity.exceptionDate,
            slaughterReleaseDate = entity.slaughterReleaseDate,
            exceptionReason = entity.exceptionReason,
            status = entity.status,
            fitForSlaughter = entity.fitForSlaughter,
            eligibleToReturn = entity.eligibleToReturn,
            returnFitnessPercentage = entity.returnFitnessPercentage,
            eligibilityTag = entity.eligibilityTag,
            latestMedicine = entity.latestMedicine,
            latestMedicineWithdrawalEnd = entity.latestMedicineWithdrawalEnd,
        )
    }

    suspend fun getCreatedAt(): Long? = dao.getCreatedAt()

    suspend fun replaceAll(items: List<AnimalExceptionRecord>) {
        dao.replaceAll(
            items = items.map { record ->
                AnimalExceptionEntity(
                    id = record.id,
                    earTag = record.earTag,
                    currentLotCode = record.currentLotCode,
                    originLotCode = record.originLotCode,
                    exceptionDate = record.exceptionDate,
                    slaughterReleaseDate = record.slaughterReleaseDate,
                    exceptionReason = record.exceptionReason,
                    status = record.status,
                    fitForSlaughter = record.fitForSlaughter,
                    eligibleToReturn = record.eligibleToReturn,
                    returnFitnessPercentage = record.returnFitnessPercentage,
                    eligibilityTag = record.eligibilityTag,
                    latestMedicine = record.latestMedicine,
                    latestMedicineWithdrawalEnd = record.latestMedicineWithdrawalEnd,
                )
            },
            metadata = AnimalExceptionCacheMetadata(createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun clear() = dao.clearAll()
}