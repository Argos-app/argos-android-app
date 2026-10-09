package com.android.argosapp.model

import java.time.LocalDate

enum class StockCategory { VACCINE, MEDICATION }

data class MedicationStockItem(
    val id: String,
    val medicationName: String,
    val expirationDate: LocalDate,
    val quantity: String,
    val category: StockCategory,
)

data class StockProduct(val id: String, val name: String, val registrationCode: String)

data class StockPage(val items: List<MedicationStockItem>, val nextCursor: String?)
