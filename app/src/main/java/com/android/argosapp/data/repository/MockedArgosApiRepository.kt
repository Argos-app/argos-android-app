package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockPage
import com.android.argosapp.model.StockProduct
import com.android.argosapp.model.User
import com.android.argosapp.util.logging.RepositoryAppLogger
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Implementação local do repositório da API Argos para desenvolvimento e testes.
 */
class MockedArgosApiRepository(
    private val notFound: Boolean = false,
    firstAccess: Boolean = false,
) : ArgosApiRepository {
    private val logger = RepositoryAppLogger("MockedArgosApiRepository")
    private var isFirstAccess = firstAccess
    override val isMedicationStockImplemented = false

    init {
        logger.info(
            "repository_initialized",
            "not_found_enabled" to notFound,
            "first_access_enabled" to firstAccess,
        )
    }

    override suspend fun getMedicationStockPage(
        query: String,
        category: StockCategory?,
        cursor: String?,
        pageSize: Int,
    ): StockPage {
        // TODO: implementar dados locais com busca, categoria e paginação; habilitar isMedicationStockImplemented.
        throw UnsupportedOperationException("Mock de estoque ainda não implementado.")
    }

    override suspend fun getMedicationProducts(): List<StockProduct> {
        // TODO: fornecer o catálogo de produtos existentes para seleção e preenchimento do código.
        throw UnsupportedOperationException("Catálogo mock de medicamentos ainda não implementado.")
    }

    override suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse =
        logger.runSuspending("authenticate") {
            delay(1000)

            if (notFound) {
                throw NoSuchElementException("User not found.")
            }

            ArgosApiAuthResponse(
                bearerToken = "mocked-bearer-token",
                user = User(
                    userName = "Tobby",
                    userEmail = "tobby@argos.com",
                    farmName = "Tobby & CIA"
                ),
                isFirstAccess = isFirstAccess,
            ).also {
                logger.info(
                    "authenticate_result_mapped",
                    "first_access" to it.isFirstAccess,
                    "has_bearer_token" to it.bearerToken.isNotBlank(),
                    "has_user" to true,
                )
            }
        }

    override suspend fun updateUserAccessStatus(bearerToken: String): Unit =
        logger.runSuspending("update_user_access_status") {
            delay(1000)

            if (bearerToken.isBlank()) {
                throw SecurityException("Bearer token inválido.")
            }

            isFirstAccess = false
        }

    override suspend fun getApplicationWithdrawalWindow(
        bearerToken: String,
        referenceDate: LocalDate,
        daysBefore: Int,
        daysAfter: Int,
    ): List<ApplicationWithdrawal> =
        logger.runSuspending("get_application_withdrawal_window") {
            delay(6000)

            val windowStart = referenceDate.minusDays(daysBefore.coerceAtLeast(0).toLong())
            val windowEnd = referenceDate.plusDays(daysAfter.coerceAtLeast(0).toLong())
            val month = YearMonth.from(referenceDate)
            fun monthDay(day: Int) = month.atDay(day.coerceAtMost(month.lengthOfMonth()))

            listOf(
                ApplicationWithdrawal(
                    applicationId = "mocked-lot-application",
                    userId = "mocked-user",
                    lotName = "Lote 100% real",
                    medicineName = "Vacina A",
                    applicationDate = monthDay(3),
                    meatWithdrawalEnd = monthDay(7),
                    daysFromReferenceDate = ChronoUnit.DAYS.between(referenceDate, monthDay(3)),
                    daysUntilSlaughter = ChronoUnit.DAYS.between(referenceDate, monthDay(7)),
                ),
                ApplicationWithdrawal(
                    applicationId = "mocked-animal-application",
                    userId = "mocked-user",
                    lotName = "Lote 101% real",
                    medicineName = "Vermífugo Y",
                    applicationDate = monthDay(10),
                    meatWithdrawalEnd = monthDay(15),
                    daysFromReferenceDate = ChronoUnit.DAYS.between(referenceDate, monthDay(10)),
                    daysUntilSlaughter = ChronoUnit.DAYS.between(referenceDate, monthDay(15)),
                ),
            ).filter { application ->
                listOfNotNull(application.applicationDate, application.meatWithdrawalEnd)
                    .any { date -> date in windowStart..windowEnd }
            }
        }

}
