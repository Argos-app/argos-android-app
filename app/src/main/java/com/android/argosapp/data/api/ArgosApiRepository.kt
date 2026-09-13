package com.android.argosapp.data.api

import com.android.argosapp.dto.ArgosApiAuthResponse
import retrofit2.Retrofit
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.converter.gson.GsonConverterFactory

private interface ArgosApiService {
    @POST("auth/signin")
    suspend fun signIn(
        @Header("Authorization") authorization: String,
    ): ArgosApiAuthResponse

    @PUT("auth/first-access")
    suspend fun updateFirstAccess(
        @Header("Authorization") authorization: String,
    )
}

/**
 * Abstrai a comunicação Retrofit com a API do Argos.
 *
 * Por padrão, usa a API de produção. Defina [homologation] como `true` para
 * direcionar as requisições ao ambiente de homologação.
 */
class ArgosApiRepository(
    homologation: Boolean = false,
) {

    private companion object {
        const val PRODUCTION_BASE_URL = "https://argos-backend.discloud.app/"
        const val HOMOLOGATION_BASE_URL = "https://argos-backend-hmg.discloud.app/"
    }

    private val api = Retrofit.Builder()
        .baseUrl(if (homologation) HOMOLOGATION_BASE_URL else PRODUCTION_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ArgosApiService::class.java)

    suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse {
        val response = api.signIn("Bearer $firebaseIdToken")

        return ArgosApiAuthResponse(
            bearerToken = response.bearerToken,
            userName = response.userName,
            email = response.email,
            farmName = response.farmName,
            isFirstAccess = response.isFirstAccess
        )
    }

    suspend fun updateUserAccessStatus(bearerToken: String) {
        api.updateFirstAccess("Bearer $bearerToken")
    }

}
