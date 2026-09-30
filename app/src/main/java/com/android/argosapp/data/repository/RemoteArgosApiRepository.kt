package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.User
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

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

/** Implementação Retrofit da comunicação com a API Argos. */
class RemoteArgosApiRepository(
    homologation: Boolean = false,
) : ArgosApiRepository {

    private companion object {
        const val PRODUCTION_BASE_URL = "https://argos-backend.discloud.app/api/v1/"
        const val HOMOLOGATION_BASE_URL = "https://argos-backend-hmg.discloud.app/api/v1/"
    }

    private val api by lazy {
        Retrofit.Builder()
            .baseUrl(if (homologation) HOMOLOGATION_BASE_URL else PRODUCTION_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ArgosApiService::class.java)
    }

    override suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse {
        val response = api.signIn("Bearer $firebaseIdToken")

        return ArgosApiAuthResponse(
            bearerToken = response.bearerToken,
            user = User(
                userName = response.user.userName,
                userEmail = response.user.userEmail,
                farmName = response.user.farmName,
            ),
            isFirstAccess = response.isFirstAccess,
        )
    }

    override suspend fun updateUserAccessStatus(bearerToken: String) {
        api.updateFirstAccess("Bearer $bearerToken")
    }
}
