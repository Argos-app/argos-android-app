package com.android.argosapp.data.repository

import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockPage
import com.android.argosapp.model.StockProduct
import com.android.argosapp.util.logging.RepositoryAppLogger

/** Esqueleto remoto. Serviço Retrofit e DTOs dependem do contrato das rotas de medicamentos. */
class RemoteMedicationStockRepository(
    private val homologation: Boolean = false,
) : MedicationStockRepository {
    private val logger = RepositoryAppLogger("RemoteMedicationStockRepository")
    override val isImplemented = false

    init {
        logger.info(
            "repository_initialized",
            "environment" to if (homologation) "homologation" else "production",
            "implementation_pending" to true,
        )
    }

    override suspend fun getPage(
        query: String,
        category: StockCategory?,
        cursor: String?,
        pageSize: Int,
    ): StockPage {
        // TODO: definir serviço/DTOs, autenticação e mapeamento da paginação após receber a rota.
        // Usar homologation para selecionar o ambiente e habilitar isImplemented ao concluir.
        throw UnsupportedOperationException("Rota de estoque de medicamentos ainda não definida.")
    }

    override suspend fun getProducts(): List<StockProduct> {
        // TODO: consultar o catálogo existente e mapear os produtos após receber seu contrato.
        throw UnsupportedOperationException("Rota do catálogo de medicamentos ainda não definida.")
    }
}
