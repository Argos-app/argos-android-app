package com.android.argosapp.data.dto

import java.math.BigDecimal

data class RegisterMedicineDto(
    val productRegistration: String,
    val batchNumber: String,
    val manufactureDate: String,
    val expiryDate: String,
    val quantity: BigDecimal,
    val supplierName: String,
    val supplierCnpj: String,
)

data class RegisterMedicineResponseDto(val medicineBatchId: String)
