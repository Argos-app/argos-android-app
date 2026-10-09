package com.android.argosapp.data.repository

import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockPage
import com.android.argosapp.model.StockProduct
import com.android.argosapp.util.logging.RepositoryAppLogger

/** Esqueleto local. Os dados e comportamentos do mock serão definidos posteriormente. */
class MockedMedicationStockRepository : MedicationStockRepository {
    private val logger = RepositoryAppLogger("MockedMedicationStockRepository")
    override val isImplemented = false

    init {
        logger.info("repository_initialized", "implementation_pending" to true)
    }

    override suspend fun getPage(
        query: String,
        category: StockCategory?,
        cursor: String?,
        pageSize: Int,
    ): StockPage {
        // TODO: implementar dados locais com busca, categoria e paginação; habilitar isImplemented.
        throw UnsupportedOperationException("Mock de estoque ainda não implementado.")
    }

    override suspend fun getProducts(): List<StockProduct> {
        // TODO: fornecer o catálogo de produtos existentes para seleção e preenchimento do código.
        throw UnsupportedOperationException("Catálogo mock de medicamentos ainda não implementado.")
    }
}
