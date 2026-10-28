package com.android.argosapp.data.repository

import com.android.argosapp.data.dto.ArgosApiAuthResponse
import com.android.argosapp.data.dto.UpdateCurrentUserDto
import com.android.argosapp.model.Application
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.data.dto.RegisterLotApplicationDto
import com.android.argosapp.data.dto.RegisterLotDto
import com.android.argosapp.data.dto.LotDto
import com.android.argosapp.data.dto.MedicinePageDto
import com.android.argosapp.data.dto.AnimalExceptionPageDto
import java.time.LocalDate

/**
 * Contrato compartilhado pelas implementações real e local da API Argos.
 */
interface ArgosApiRepository {
    suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse

    suspend fun updateUserAccessStatus(bearerToken: String)

    suspend fun updateCurrentUser(bearerToken: String, data: UpdateCurrentUserDto)

    suspend fun registerLotApplication(bearerToken: String, userId: String, data: RegisterLotApplicationDto)

    suspend fun listAllApplications(bearerToken: String): List<Application>

    suspend fun getApplicationWithdrawalWindow(
        bearerToken: String,
        referenceDate: LocalDate,
        daysBefore: Int = referenceDate.dayOfMonth - 1,
        daysAfter: Int = referenceDate.lengthOfMonth() - referenceDate.dayOfMonth,
    ): List<ApplicationWithdrawal>

    suspend fun registerLot(bearerToken: String, data: RegisterLotDto)

    suspend fun listLots(bearerToken: String): List<LotDto>

    suspend fun getMedicinesPage(bearerToken: String, page: Int, size: Int): MedicinePageDto

    suspend fun getAnimalExceptionsPage(bearerToken: String, active: Boolean?, page: Int, size: Int): AnimalExceptionPageDto
}
