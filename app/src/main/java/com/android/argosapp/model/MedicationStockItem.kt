package com.android.argosapp.model

import java.time.LocalDate
import java.math.BigDecimal

enum class StockCategory { VACCINE, MEDICATION }

data class MedicationStockItem(
    val id: String,
    val medicationName: String,
    val expirationDate: LocalDate,
    val quantity: String,
    val category: StockCategory,
    val supplierName: String,
    val manufactureDate: LocalDate? = null,
    val batchNumber: String? = null,
    val dose: BigDecimal? = null,
    val doseUnit: String? = null,
    val withdrawalDays: Int? = null,
    val activeIngredient: String? = null,
    val therapeuticCategory: String? = null,
)

data class StockProduct(val id: String, val name: String, val registrationCode: String)

data class StockPage(val items: List<MedicationStockItem>, val nextCursor: String?)

/** Campos aceitos pelo POST /medicines. */
data class MedicationStockRegistration(
    val productRegistration: String,
    val batchNumber: String,
    val manufactureDate: LocalDate,
    val expiryDate: LocalDate,
    val quantity: BigDecimal,
    val supplierName: String,
    val supplierCnpj: String,
    val category: StockCategory? = null,
)
