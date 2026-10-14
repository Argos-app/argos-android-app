package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.dto.UpdateCurrentUserDto
import com.android.argosapp.model.Application
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.util.logging.RepositoryAppLogger
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Implementação local do repositório da API Argos para desenvolvimento e testes.
 */
class MockedArgosApiRepository(
    private val notFound: Boolean = false,
    firstAccess: Boolean = false,
) : ArgosApiRepository {
    private val logger = RepositoryAppLogger("MockedArgosApiRepository")
    private var isFirstAccess = firstAccess

    init {
        logger.info(
            "repository_initialized",
            "not_found_enabled" to notFound,
            "first_access_enabled" to firstAccess,
        )
    }

    override suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse =
        logger.runSuspending("authenticate") {
            delay(1000)

            if (notFound) {
                throw NoSuchElementException("User not found.")
            }

            ArgosApiAuthResponse(
                bearerToken = "mocked-bearer-token",
                userName = "Tobby",
                email = "tobby@argos.com",
                farmName = "Tobby & CIA",
                isFirstAccess = isFirstAccess,
                userId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                photoUrl = null,
                cpf = "49752927858",
                address = "Rua Casa da Mãe Joana",
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

    override suspend fun updateCurrentUser(bearerToken: String, data: UpdateCurrentUserDto): Unit =
        logger.runSuspending(
            operation = "update_current_user",
            startFields = arrayOf(
                "method" to "PATCH",
                "endpoint" to "update-perfil",
                "has_name" to (data.name != null),
                "has_email" to (data.email != null),
                "has_photo_url" to (data.photoUrl != null),
            ),
        ) {
            delay(1000)

            if (bearerToken.isBlank()) {
                throw SecurityException("Bearer token inválido.")
            }
        }

    override suspend fun listAllApplications(bearerToken: String): List<Application> {
        return logger.runSuspending("list_all_applications") {
            delay(600)

            listOf(
                Application(
                    lotName = "Lote 100% real",
                    lotCode = "L-100",
                    earCode = null,
                    medicineName = "Vacina A",
                    applicationDate = LocalDateTime.now().minusDays(3),
                    applicationTime = LocalTime.of(9, 30),
                    dose = BigDecimal("2.5"),
                    unit = "mL",
                    route = "intramuscular",
                    headCountApplied = 12,
                    headCount = 12,
                    meatWithdrawalEnd = LocalDate.now().plusDays(4),
                    status = "Pendente",
                    goal = "Vacinação preventiva",
                    notes = null,
                ),
                Application(
                    lotName = "Lote 101% real",
                    lotCode = "L-101",
                    earCode = "BR-101",
                    medicineName = "Vermífugo Y",
                    applicationDate = LocalDateTime.now().minusDays(8),
                    applicationTime = LocalTime.of(14, 0),
                    dose = BigDecimal("5.0"),
                    unit = "mL",
                    route = "oral",
                    headCountApplied = 1,
                    headCount = 1,
                    meatWithdrawalEnd = LocalDate.now().minusDays(2),
                    status = "Concluida",
                    goal = "Controle de parasitas",
                    notes = null,
                ),
            ).also { applications ->
                logger.info("list_all_applications_result", "application_count" to applications.size)
            }
        }
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
