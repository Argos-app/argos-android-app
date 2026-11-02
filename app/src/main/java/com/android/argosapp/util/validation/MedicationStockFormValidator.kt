package com.android.argosapp.util.validation

import java.math.BigDecimal
import java.time.LocalDate

class MedicationStockFormValidator {
    enum class Field {
        PRODUCT,
        BATCH_NUMBER,
        QUANTITY,
        MANUFACTURE_DATE,
        EXPIRY_DATE,
        SUPPLIER,
        SUPPLIER_CNPJ,
    }

    fun invalidFields(
        productRegistration: String?,
        batchNumber: String,
        quantity: String,
        manufactureDate: LocalDate?,
        expiryDate: LocalDate?,
        supplier: String,
        supplierCnpj: String,
    ): Set<Field> = buildSet {
        if (productRegistration.isNullOrBlank()) add(Field.PRODUCT)
        if (batchNumber.isBlank()) add(Field.BATCH_NUMBER)
        if (!isValidQuantity(quantity)) add(Field.QUANTITY)
        if (manufactureDate == null) add(Field.MANUFACTURE_DATE)
        if (expiryDate == null || manufactureDate != null && !expiryDate.isAfter(manufactureDate)) {
            add(Field.EXPIRY_DATE)
        }
        if (supplier.isBlank()) add(Field.SUPPLIER)
        if (supplierCnpj.count(Char::isDigit) != CNPJ_LENGTH) add(Field.SUPPLIER_CNPJ)
    }

    private fun isValidQuantity(value: String): Boolean =
        value.trim().replace(',', '.').toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true

    companion object {
        private const val CNPJ_LENGTH = 14
    }
}
