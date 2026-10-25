package com.android.argosapp.data.repository

import com.android.argosapp.model.MedicationStockRegistration
import com.android.argosapp.model.StockCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class MockedArgosApiRepositoryTest {
    @Test
    fun `list supports multiple pages without repeating items`() = runBlocking {
        val repository = MockedArgosApiRepository()
        val first = repository.getMedicationStockPage("", null, null, 20)
        val second = repository.getMedicationStockPage("", null, first.nextCursor, 20)
        val third = repository.getMedicationStockPage("", null, second.nextCursor, 20)

        assertEquals(20, first.items.size)
        assertEquals(20, second.items.size)
        assertEquals(20, third.items.size)
        assertNotNull(first.nextCursor)
        assertNotNull(second.nextCursor)
        assertNull(third.nextCursor)
        assertEquals(60, (first.items + second.items + third.items).map { it.id }.distinct().size)
    }

    @Test
    fun `search matches medication name across result pages`() = runBlocking {
        val repository = MockedArgosApiRepository()
        val first = repository.getMedicationStockPage("hipocalcemia", null, null, 20)
        val second = repository.getMedicationStockPage("hipocalcemia", null, first.nextCursor, 20)

        assertTrue(first.items.isNotEmpty())
        assertTrue((first.items + second.items).all { it.medicationName.contains("hipocalcemia", ignoreCase = true) })
        assertEquals(40, (first.items + second.items).size)
        assertNull(second.nextCursor)
    }

    @Test
    fun `category filter returns only vaccines`() = runBlocking {
        val repository = MockedArgosApiRepository()
        val page = repository.getMedicationStockPage("", StockCategory.VACCINE, null, 20)

        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.category == StockCategory.VACCINE })
        assertNull(page.nextCursor)
    }

    @Test
    fun `registration inserts an item visible in the first page`() = runBlocking {
        val repository = MockedArgosApiRepository()
        val registration = MedicationStockRegistration(
            productRegistration = "12345",
            batchNumber = "LOTE-TESTE-001",
            manufactureDate = LocalDate.of(2026, 1, 10),
            expiryDate = LocalDate.of(2028, 1, 10),
            quantity = BigDecimal("12.5"),
            supplierName = "Distribuidora Teste",
            supplierCnpj = "12345678000199",
            propertyId = "mock-property-id",
            category = StockCategory.MEDICATION,
        )

        repository.registerMedicationStock(registration)
        val page = repository.getMedicationStockPage("hipocalcemia", null, null, 20)

        assertEquals("Cálcio intravenoso (hipocalcemia)", page.items.first().medicationName)
        assertEquals("12.5", page.items.first().quantity)
        assertFalse(page.items.first().id.isBlank())
    }
}
