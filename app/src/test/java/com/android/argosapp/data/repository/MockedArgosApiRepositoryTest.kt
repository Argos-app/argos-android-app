package com.android.argosapp.data.repository

import com.android.argosapp.data.dto.RegisterLotApplicationDto
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MockedArgosApiRepositoryTest {
    private val request = RegisterLotApplicationDto(
        lotName = "Lote 100% real",
        medicineName = "Vacina A",
        applicationDate = "2026-10-20",
        dose = BigDecimal("50"),
    )

    @Test
    fun registeredApplicationAppearsInNextListWithoutChangingSeedRecords() = runBlocking {
        val repository = MockedArgosApiRepository()
        val before = repository.listAllApplications("token")

        repository.registerLotApplication("token", "user", request)
        val after = repository.listAllApplications("token")

        assertEquals(before.size + 1, after.size)
        val saved = after.first()
        assertEquals(request.lotName, saved.lotName)
        assertEquals(request.medicineName, saved.medicineName)
        assertEquals(LocalDate.parse(request.applicationDate), saved.applicationDate?.toLocalDate())
        assertEquals(request.dose, saved.dose)
        assertEquals("mL", saved.unit)
        assertEquals(before.map { it.lotName }, after.drop(1).map { it.lotName })
    }

    @Test
    fun invalidFormDoesNotCreateApplications() = runBlocking {
        val repository = MockedArgosApiRepository()
        val before = repository.listAllApplications("token").size
        val invalidRequests = listOf(
            request.copy(lotName = "missing"),
            request.copy(medicineName = "missing"),
            request.copy(dose = BigDecimal.ZERO),
            request.copy(applicationDate = "invalid"),
        )
        for (invalid in invalidRequests) {
            try {
                repository.registerLotApplication("token", "user", invalid)
                fail("Invalid request was accepted: $invalid")
            } catch (_: IllegalArgumentException) {
            } catch (_: NoSuchElementException) {
            } catch (_: java.time.format.DateTimeParseException) {
            }
        }
        assertEquals(before, repository.listAllApplications("token").size)
    }

    @Test
    fun missingSessionDoesNotCreateApplications() = runBlocking {
        val repository = MockedArgosApiRepository()
        try {
            repository.registerLotApplication("", "user", request)
            fail("Missing session was accepted")
        } catch (_: SecurityException) {
        }
        assertEquals(2, repository.listAllApplications("token").size)
    }

    @Test
    fun optionsAllowScrollingThroughFourMedicinePagesWithoutDuplicates() = runBlocking {
        val repository = MockedArgosApiRepository()
        val lots = repository.listLots("token")
        assertEquals(30, lots.size)
        assertEquals(30, lots.map { it.lotName }.distinct().size)

        val pages = (0..3).map { repository.getMedicinesPage("token", it, 20) }
        assertEquals(listOf(20, 20, 20, 5), pages.map { it.data.size })
        assertEquals(listOf(false, false, false, true), pages.map { it.last })
        assertEquals(listOf(0, 1, 2, 3), pages.map { it.page })
        assertTrue(pages.all { it.totalElements == 65L && it.totalPages == 4 })
        assertEquals(65, pages.flatMap { it.data }.map { it.medicineBatchId }.distinct().size)
        val beyondLastPage = repository.getMedicinesPage("token", 4, 20)
        assertTrue(beyondLastPage.data.isEmpty())
        assertTrue(beyondLastPage.last)
    }
}