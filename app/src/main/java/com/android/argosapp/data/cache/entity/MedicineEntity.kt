package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey val medicineBatchId: String,
    val pageIndex: Int,
    val medicineName: String,
    val supplier: String?,
    val manufactureDate: String?,
    val expiryDate: String?,
    val quantity: String?,
    val dose: String?,
    val withdrawalDays: Int?,
    val batch: String?,
    val activeIngredient: String?,
    val therapeuticCategory: String?,
)
