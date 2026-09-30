package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse

/**
 * Contrato compartilhado pelas implementações real e local da API Argos.
 */
interface ArgosApiRepository {
    suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse

    suspend fun updateUserAccessStatus(bearerToken: String)
}
