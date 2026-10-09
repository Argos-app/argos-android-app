package com.android.argosapp.data.repository

import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockPage
import com.android.argosapp.model.StockProduct

/** Contrato de estoque compartilhado pelas implementações mock e remota. */
interface MedicationStockRepository {
    /** Os esqueletos mantêm a UI no estado de integração pendente. */
    val isImplemented: Boolean get() = true

    suspend fun getPage(query: String, category: StockCategory?, cursor: String?, pageSize: Int): StockPage
    suspend fun getProducts(): List<StockProduct>
}
