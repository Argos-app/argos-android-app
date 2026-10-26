package com.android.argosapp.dto

data class MedicineDto(
    val medicineBatchId: String,
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

data class MedicinePageDto(
    val data: List<MedicineDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
)
