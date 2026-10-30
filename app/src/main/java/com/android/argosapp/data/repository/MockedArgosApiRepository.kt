package com.android.argosapp.data.repository

import com.android.argosapp.data.dto.ArgosApiAuthResponse
import com.android.argosapp.data.dto.UpdateCurrentUserDto
import com.android.argosapp.data.dto.RegisterLotApplicationDto
import com.android.argosapp.data.dto.RegisterLotDto
import com.android.argosapp.data.dto.LotDto
import com.android.argosapp.data.dto.MedicineDto
import com.android.argosapp.data.dto.MedicinePageDto
import com.android.argosapp.data.dto.AnimalExceptionDto
import com.android.argosapp.data.dto.AnimalExceptionPageDto
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
    // shortcut: mock registrations last only for this session; persist them when testing app restarts.
    private val registeredApplications = mutableListOf<Application>()
    private val registeredLots = mutableListOf<RegisterLotDto>()

    override suspend fun registerLotApplication(
        bearerToken: String,
        userId: String,
        data: RegisterLotApplicationDto,
    ): Unit = logger.runSuspending("register_lot_application") {
        if (bearerToken.isBlank() || userId.isBlank()) throw SecurityException("User session is unavailable.")
        require(data.dose.signum() > 0) { "Dose must be positive." }
        require(data.earCode == null || (data.earCode.length >= 2 && data.earCode.all(Char::isDigit))) { "Ear code is invalid." }
        require(listLots(bearerToken).any { it.lotName == data.lotName }) { "Lot not found." }
        val medicine = medicines.firstOrNull { it.medicineName == data.medicineName }
            ?: throw NoSuchElementException("Medicine not found.")
        val date = LocalDate.parse(data.applicationDate)
        val time = LocalTime.now()
        val withdrawalEnd = date.plusDays(medicine.withdrawalDays?.toLong() ?: 0)
        delay(600)
        registeredApplications.add(0, Application(
            lotName = data.lotName,
            lotCode = null,
            earCode = data.earCode,
            medicineName = data.medicineName,
            applicationDate = date.atTime(time),
            applicationTime = time,
            dose = data.dose,
            unit = data.unit,
            route = null,
            headCountApplied = if (data.earCode == null) null else 1,
            headCount = if (data.earCode == null) null else 1,
            meatWithdrawalEnd = withdrawalEnd,
            status = if (withdrawalEnd.isAfter(LocalDate.now())) "Pendente" else "Concluida",
            goal = null,
            notes = null,
        ))
        Unit
    }

    override suspend fun registerLot(bearerToken: String, data: RegisterLotDto): Unit = logger.runSuspending("register_lot") {
        require(bearerToken.isNotBlank()) { "Bearer token is unavailable." }
        require(data.lotName.isNotBlank()) { "Lot name is required." }
        require(data.herdName.isNotBlank()) { "Herd name is required." }
        require(data.category.isNotBlank()) { "Lot category is required." }
        require(data.headCount > 0) { "Head count must be positive." }
        LocalDate.parse(data.releaseSlaughterDate)
        delay(600)
        registeredLots.add(0, data)
        Unit
    }

    override suspend fun listLots(bearerToken: String): List<LotDto> = registeredLots.map { lot ->
        LotDto(
            lotName = lot.lotName,
            openDate = LocalDate.now().toString(),
            releaseSlaughterDate = lot.releaseSlaughterDate,
            category = lot.category,
            animalExceptionOutList = emptyList(),
        )
    } + listOf(
        LotDto("Lote 100% real", null, null, "ENGORDA", emptyList()),
        LotDto("Lote 101% real", null, null, "RECRIA", emptyList()),
    ) + List(28) { index ->
        LotDto("Lote ${index + 102}% real", null, null, "ENGORDA", emptyList())
    }

    private val medicines = listOf(
        MedicineDto("00000000-0000-0000-0000-000000000001", "Vacina A", "Fornecedor mock", null, null, "500", "2.5", 0, "VAC-001", "Vacina A", "Vacina"),
        MedicineDto("00000000-0000-0000-0000-000000000002", "Vermífugo Y", "Fornecedor mock", null, null, "300", "5.0", 28, "VER-001", "Ivermectina", "Antiparasitário"),
    ) + List(63) { index ->
        MedicineDto(
            medicineBatchId = UUID(0L, index.toLong() + 3).toString(),
            medicineName = "Medicamento mock ${index + 3}",
            supplier = "Fornecedor mock",
            manufactureDate = null,
            expiryDate = null,
            quantity = "500",
            dose = "5.0",
            withdrawalDays = 7,
            batch = "MOCK-${index + 3}",
            activeIngredient = "Princípio ativo mock",
            therapeuticCategory = "Teste",
        )
    }

    override suspend fun getAnimalExceptionsPage(
        bearerToken: String,
        active: Boolean?,
        page: Int,
        size: Int,
    ): AnimalExceptionPageDto {
        require(bearerToken.isNotBlank()) { "Bearer token is unavailable." }
        require(page >= 0 && size in 1..100) { "Invalid pagination parameters." }
        val lots = listOf(
            "Sub Engorda 001", "Engorda 002", "Recria 001", "Maternidade 001",
            "Engorda 003", "Recria 002", "Sub Engorda 002", "Engorda 004",
            "Sub Terminacao 001", "Engorda 001", "Recria 003", "Sub Engorda 003",
            "Terminacao 002", "Engorda 005", "Recria 004", "Terminacao 003",
            "Sub Terminacao 002", "Engorda 006",
        )
        val tags = listOf("075", "102", "118", "124", "131", "145", "163", "177", "098", "105", "119", "128", "137", "149", "158", "166", "181", "194")
        val records = lots.mapIndexed { index, lot ->
            val eligible = index < 8
            AnimalExceptionDto(
                id = UUID(0L, index.toLong() + 200).toString(),
                earTag = tags[index],
                currentLotCode = lot,
                originLotCode = lot,
                exceptionDate = "2026-06-04",
                slaughterReleaseDate = "2026-08-04",
                exceptionReason = "Animal com suspeita de febre aftosa.",
                status = "ATIVO",
                fitForSlaughter = eligible,
                eligibleToReturn = eligible,
                returnFitnessPercentage = if (eligible) 100 else 0,
                eligibilityTag = if (eligible) "APTO" else "INAPTO",
                latestMedicine = null,
                latestMedicineWithdrawalEnd = null,
            )
        }.filter { active == null || it.status == (if (active) "ATIVO" else "ENCERRADO") }
        val fromIndex = (page * size).coerceIn(0, records.size)
        val toIndex = (fromIndex + size).coerceIn(fromIndex, records.size)
        return AnimalExceptionPageDto(
            data = records.subList(fromIndex, toIndex),
            page = page,
            size = size,
            totalElements = records.size.toLong(),
            totalPages = if (records.isEmpty()) 0 else (records.size + size - 1) / size,
            first = page == 0,
            last = toIndex >= records.size,
        )
    }
    override suspend fun getMedicinesPage(bearerToken: String, page: Int, size: Int): MedicinePageDto {
        val fromIndex = (page * size).coerceAtMost(medicines.size)
        val toIndex = (fromIndex + size).coerceAtMost(medicines.size)
        return MedicinePageDto(
            data = medicines.subList(fromIndex, toIndex),
            page = page,
            size = size,
            totalElements = medicines.size.toLong(),
            totalPages = if (medicines.isEmpty()) 0 else (medicines.size + size - 1) / size,
            first = page == 0,
            last = toIndex >= medicines.size,
        )
    }

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
                role = "_SET"
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
                "has_address" to (data.address != null),
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

            (registeredApplications.toList() + listOf(
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
            )).also { applications ->
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
            delay(1000)

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
