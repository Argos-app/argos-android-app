package com.android.argosapp.model

data class MedicationStockItem(
    val id: String,
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
