package com.android.argosapp.viewmodel

import com.android.argosapp.model.MedicationStockItem
import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockPage

data class MedicationStockState(
    val items: List<MedicationStockItem> = emptyList(),
    val query: String = "",
    val category: StockCategory? = null,
    val loading: Boolean = false,
    val error: Boolean = false,
    val hasMore: Boolean = true,
)

class MedicationStockPager {
    data class Request(val generation: Long, val query: String, val category: StockCategory?, val cursor: String?)
    var state = MedicationStockState()
        private set
    private var generation = 0L
    private var cursor: String? = null
    private var activeRequest: Request? = null

    fun reset(query: String, category: StockCategory?) {
        generation++
        cursor = null
        activeRequest = null
        state = MedicationStockState(query = query, category = category)
    }

    fun nextRequest(retry: Boolean = false): Request? {
        if (state.loading || !state.hasMore || (state.error && !retry)) return null
        val request = Request(generation, state.query, state.category, cursor)
        activeRequest = request
        state = state.copy(loading = true, error = false)
        return request
    }

    fun complete(request: Request, page: StockPage) {
        if (request !== activeRequest) return
        activeRequest = null
        cursor = page.nextCursor
        state = state.copy(
            items = (state.items + page.items).distinctBy { it.id },
            loading = false,
            hasMore = page.nextCursor != null && page.nextCursor != request.cursor,
        )
    }

    fun fail(request: Request) {
        if (request !== activeRequest) return
        activeRequest = null
        state = state.copy(loading = false, error = true)
    }
}
