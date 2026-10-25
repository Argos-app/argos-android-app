package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.dto.UpdateCurrentUserDto
import com.android.argosapp.model.Application
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.dto.RegisterLotApplicationDto
import com.android.argosapp.dto.LotDto
import com.android.argosapp.dto.MedicinePageDto
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

    suspend fun listLots(bearerToken: String): List<LotDto>

    suspend fun getMedicinesPage(bearerToken: String, page: Int, size: Int): MedicinePageDto
}
