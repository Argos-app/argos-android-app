package com.android.argosapp.data.repository

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationStockDetailsMappingTest {
    @Test
    fun stockListItemMapsExpandedBatchDetails() {
        val item = StockMedicineListItemDto(
            medicineBatchId = "batch-42",
            medicineName = "Ivermectina",
            supplier = "Fornecedor Alfa",
            manufactureDate = "2026-01-02",
            expiryDate = "2027-01-02",
            quantity = BigDecimal("12.500"),
            dose = BigDecimal("1.25"),
            doseUnit = "mL",
            withdrawalDays = 7,
            batch = "LOTE-2026-A",
            activeIngredient = "Ivermectina",
            therapeuticCategory = "Medicamento",
        ).toModel()

        assertEquals("batch-42", item.id)
        assertEquals("Ivermectina", item.medicationName)
        assertEquals("Fornecedor Alfa", item.supplierName)
        assertEquals("2026-01-02", item.manufactureDate.toString())
        assertEquals("2027-01-02", item.expirationDate.toString())
        assertEquals("12.5", item.quantity)
        assertEquals(BigDecimal("1.25"), item.dose)
        assertEquals("mL", item.doseUnit)
        assertEquals(7, item.withdrawalDays)
        assertEquals("LOTE-2026-A", item.batchNumber)
        assertEquals("Ivermectina", item.activeIngredient)
        assertEquals("Medicamento", item.therapeuticCategory)
    }

    @Test
    fun stockListItemMapsMissingSupplierToRequiredEmptyValue() {
        val item = StockMedicineListItemDto(
            medicineBatchId = "batch-43",
            medicineName = "Vacina",
            supplier = null,
            manufactureDate = "2026-01-02",
            expiryDate = "2027-01-02",
            quantity = BigDecimal("2"),
            dose = null,
            doseUnit = null,
            withdrawalDays = null,
            batch = "LOTE-2026-B",
            activeIngredient = null,
            therapeuticCategory = "Vacina",
        ).toModel()

        assertEquals("", item.supplierName)
    }
}
