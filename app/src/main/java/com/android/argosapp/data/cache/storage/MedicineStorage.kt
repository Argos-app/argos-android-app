package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.dao.MedicineDao
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.MedicineCacheMetadata
import com.android.argosapp.data.cache.entity.MedicineEntity
import com.android.argosapp.data.dto.MedicineDto
import com.android.argosapp.data.dto.MedicinePageDto

class MedicineStorage(context: Context) {
    private val dao: MedicineDao = ArgosCacheDatabase.getInstance(context).medicineDao()

    suspend fun getAll(): List<MedicineDto> = dao.getAll().map { entity ->
        MedicineDto(
            medicineBatchId = entity.medicineBatchId,
            medicineName = entity.medicineName,
            supplier = entity.supplier,
            manufactureDate = entity.manufactureDate,
            expiryDate = entity.expiryDate,
            quantity = entity.quantity,
            dose = entity.dose,
            withdrawalDays = entity.withdrawalDays,
            batch = entity.batch,
            activeIngredient = entity.activeIngredient,
            therapeuticCategory = entity.therapeuticCategory,
        )
    }

    suspend fun getNamesForSelect(): List<String> = dao.getNamesForSelect()
    suspend fun getPaginationState(): MedicineCacheMetadata? = dao.getPaginationState()

    suspend fun saveFirstPage(page: MedicinePageDto) {
        dao.replaceFirstPage(
            page.data.mapIndexed { index, medicine ->
                MedicineEntity(
                    medicineBatchId = medicine.medicineBatchId,
                    pageIndex = page.page * page.size + index,
                    medicineName = medicine.medicineName,
                    supplier = medicine.supplier,
                    manufactureDate = medicine.manufactureDate,
                    expiryDate = medicine.expiryDate,
                    quantity = medicine.quantity,
                    dose = medicine.dose,
                    withdrawalDays = medicine.withdrawalDays,
                    batch = medicine.batch,
                    activeIngredient = medicine.activeIngredient,
                    therapeuticCategory = medicine.therapeuticCategory,
                )
            },
            MedicineCacheMetadata(
                createdAt = System.currentTimeMillis(),
                nextPage = page.page + 1,
                isLastPage = page.last,
            ),
        )
    }

    suspend fun appendPage(page: MedicinePageDto, createdAt: Long) {
        dao.appendPage(
            page.data.mapIndexed { index, medicine ->
                MedicineEntity(
                    medicineBatchId = medicine.medicineBatchId,
                    pageIndex = page.page * page.size + index,
                    medicineName = medicine.medicineName,
                    supplier = medicine.supplier,
                    manufactureDate = medicine.manufactureDate,
                    expiryDate = medicine.expiryDate,
                    quantity = medicine.quantity,
                    dose = medicine.dose,
                    withdrawalDays = medicine.withdrawalDays,
                    batch = medicine.batch,
                    activeIngredient = medicine.activeIngredient,
                    therapeuticCategory = medicine.therapeuticCategory,
                )
            },
            MedicineCacheMetadata(
                createdAt = createdAt,
                nextPage = page.page + 1,
                isLastPage = page.last,
            ),
        )
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
