package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockPage
import com.android.argosapp.model.StockProduct
import java.time.LocalDate

/**
 * Contrato compartilhado pelas implementações real e local da API Argos.
 */
interface ArgosApiRepository {
    /** Mantém o estoque no estado de integração pendente até implementar suas operações. */
    val isMedicationStockImplemented: Boolean

    suspend fun getMedicationStockPage(
        query: String,
        category: StockCategory?,
        cursor: String?,
        pageSize: Int,
    ): StockPage

    suspend fun getMedicationProducts(): List<StockProduct>

    suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse

    suspend fun updateUserAccessStatus(bearerToken: String)

    suspend fun getApplicationWithdrawalWindow(
        bearerToken: String,
        referenceDate: LocalDate,
        daysBefore: Int = referenceDate.dayOfMonth - 1,
        daysAfter: Int = referenceDate.lengthOfMonth() - referenceDate.dayOfMonth,
    ): List<ApplicationWithdrawal>
}
