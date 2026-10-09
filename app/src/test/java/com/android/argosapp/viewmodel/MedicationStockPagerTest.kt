package com.android.argosapp.viewmodel

import com.android.argosapp.model.MedicationStockItem
import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockPage
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MedicationStockPagerTest {
    private fun item(id: String) = MedicationStockItem(id, "Produto $id", LocalDate.of(2028, 6, 23), "30", StockCategory.MEDICATION)

    @Test fun `only one request is allowed and last page stops pagination`() {
        val pager = MedicationStockPager()
        val first = requireNotNull(pager.nextRequest())
        assertNull(pager.nextRequest())
        pager.complete(first, StockPage(listOf(item("1")), "next"))
        val second = requireNotNull(pager.nextRequest())
        assertEquals("next", second.cursor)
        pager.complete(second, StockPage(listOf(item("2")), null))
        assertEquals(listOf("1", "2"), pager.state.items.map { it.id })
        assertNull(pager.nextRequest())
    }

    @Test fun `new search rejects stale pages and preserves category`() {
        val pager = MedicationStockPager()
        pager.reset("calcio", StockCategory.VACCINE)
        val stale = requireNotNull(pager.nextRequest())
        pager.reset("outro", StockCategory.VACCINE)
        val current = requireNotNull(pager.nextRequest())
        pager.complete(stale, StockPage(listOf(item("stale")), "next"))
        assertTrue(pager.state.items.isEmpty())
        assertTrue(pager.state.loading)
        assertEquals("outro", current.query)
        assertEquals(StockCategory.VACCINE, current.category)
        pager.complete(current, StockPage(listOf(item("current")), null))
        assertEquals("current", pager.state.items.single().id)
    }

    @Test fun `failed page requires explicit retry and keeps previously loaded data`() {
        val pager = MedicationStockPager()
        pager.reset("busca", StockCategory.MEDICATION)
        val first = requireNotNull(pager.nextRequest())
        pager.complete(first, StockPage(listOf(item("1")), "next"))
        val failed = requireNotNull(pager.nextRequest())
        pager.fail(failed)
        assertNull(pager.nextRequest())
        val retry = requireNotNull(pager.nextRequest(retry = true))
        assertEquals(failed.cursor, retry.cursor)
        assertEquals("busca", retry.query)
        assertEquals(StockCategory.MEDICATION, retry.category)
        pager.complete(retry, StockPage(listOf(item("1"), item("2")), null))
        assertEquals(listOf("1", "2"), pager.state.items.map { it.id })
    }

    @Test fun `refresh keeps search and filter and restarts the first page`() {
        val pager = MedicationStockPager()
        pager.reset("calcio", StockCategory.MEDICATION)
        val first = requireNotNull(pager.nextRequest())
        pager.complete(first, StockPage(listOf(item("1")), "next"))
        pager.reset(pager.state.query, pager.state.category)
        val refreshed = requireNotNull(pager.nextRequest())
        assertEquals("calcio", refreshed.query)
        assertEquals(StockCategory.MEDICATION, refreshed.category)
        assertNull(refreshed.cursor)
        assertTrue(pager.state.items.isEmpty())
    }

    @Test fun `repeated cursor stops automatic requests`() {
        val pager = MedicationStockPager()
        val first = requireNotNull(pager.nextRequest())
        pager.complete(first, StockPage(listOf(item("1")), "same"))
        val second = requireNotNull(pager.nextRequest())
        pager.complete(second, StockPage(listOf(item("2")), "same"))
        assertNull(pager.nextRequest())
    }
}
