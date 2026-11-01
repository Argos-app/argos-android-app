package com.android.argosapp.data.repository

import com.android.argosapp.model.StockProduct
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicineCatalogPaginationTest {
    @Test
    fun `catalog item preserves backend id name and registration exactly`() {
        val product = mapMedicineCatalogItem(
            medicineId = "medicine-42",
            medicineName = "Ivermectina",
            productRegistration = " MAPA-12345 ",
        )

        assertEquals(
            StockProduct("medicine-42", "Ivermectina", " MAPA-12345 "),
            product,
        )
    }

    @Test
    fun `catalog fetches every page in zero based order`() = runBlocking {
        val requests = mutableListOf<Pair<Int, Int>>()
        val products = fetchAllMedicineCatalogProducts { page, size ->
            requests += page to size
            when (page) {
                0 -> MedicineCatalogPage(
                    products = listOf(StockProduct("medicine-1", "Vacina A", "MAPA-1")),
                    totalPages = 2,
                )
                1 -> MedicineCatalogPage(
                    products = listOf(StockProduct("medicine-2", "Vacina B", "MAPA-2")),
                    totalPages = 2,
                )
                else -> error("Unexpected page $page")
            }
        }

        assertEquals(listOf(0 to 20, 1 to 20), requests)
        assertEquals(
            listOf(
                StockProduct("medicine-1", "Vacina A", "MAPA-1"),
                StockProduct("medicine-2", "Vacina B", "MAPA-2"),
            ),
            products,
        )
    }

    @Test
    fun `empty catalog still requests page zero and returns empty list`() = runBlocking {
        val requests = mutableListOf<Pair<Int, Int>>()
        val products = fetchAllMedicineCatalogProducts { page, size ->
            requests += page to size
            MedicineCatalogPage(products = emptyList(), totalPages = 0)
        }

        assertEquals(listOf(0 to 20), requests)
        assertEquals(emptyList<StockProduct>(), products)
    }
}
