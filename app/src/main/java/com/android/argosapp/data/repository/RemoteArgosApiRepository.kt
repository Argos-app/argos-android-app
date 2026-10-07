package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.dto.ListAllApplicationsDto
import com.android.argosapp.dto.ApplicationWithdrawalDto
import com.android.argosapp.model.Application
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.model.User
import com.android.argosapp.util.logging.RepositoryAppLogger
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PUT
import java.time.LocalDate

private interface ArgosApiService {
    @GET("application/list-all")
    suspend fun listAllApplications(
        @Header("Authorization") authorization: String,
    ): Response<List<ListAllApplicationsDto>>

    @GET("application/withdrawal-window")
    suspend fun getApplicationWithdrawalWindow(
        @Header("Authorization") authorization: String,
        @Query("referenceDate") referenceDate: String,
        @Query("daysBefore") daysBefore: Int,
        @Query("daysAfter") daysAfter: Int,
    ): Response<List<ApplicationWithdrawalDto>>

    @POST("auth/signin")
    suspend fun signIn(
        @Header("Authorization") authorization: String,
    ): Response<ArgosApiAuthResponse>

    @PUT("auth/first-access")
    suspend fun updateFirstAccess(
        @Header("Authorization") authorization: String,
    ): Response<Unit>
}

/** Implementação Retrofit da comunicação com a API Argos. */
class RemoteArgosApiRepository(
    homologation: Boolean = false,
) : ArgosApiRepository {

    private companion object {
        const val PRODUCTION_BASE_URL = "https://argos-backend.discloud.app/api/v1/"
        const val HOMOLOGATION_BASE_URL = "https://argos-backend-hmg.onrender.com/api/v1/"
    }

    private val logger = RepositoryAppLogger("RemoteArgosApiRepository")
    private val environment = if (homologation) "homologation" else "production"
    private val baseUrl = if (homologation) HOMOLOGATION_BASE_URL else PRODUCTION_BASE_URL

    init {
        logger.info(
            "repository_initialized",
            "environment" to environment,
            "base_url" to baseUrl,
        )
    }

    private val api by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ArgosApiService::class.java)
    }

    override suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse = logger.runSuspending(
        operation = "authenticate",
        startFields = arrayOf(
            "environment" to environment,
            "method" to "POST",
            "endpoint" to "auth/signin",
        ),
        failureFields = { error ->
            arrayOf(
                "http_status" to (error as? HttpException)?.code(),
                "error_type" to error.javaClass.simpleName,
            )
        },


    ) {
        val response = api.signIn("Bearer $firebaseIdToken")

        logResponse("authenticate", response.code(), response.isSuccessful)

        if (!response.isSuccessful) throw HttpException(response)

        val body = requireNotNull(response.body()) {
            "Successful sign-in response contained no body."
        }

        ArgosApiAuthResponse(
            bearerToken = body.bearerToken,
            user = User(
                userName = body.user.userName,
                userEmail = body.user.userEmail,
                farmName = body.user.farmName,
            ),
            isFirstAccess = body.isFirstAccess,
        ).also {
            logger.info(
                "authenticate_result_mapped",
                "first_access" to it.isFirstAccess,
                "has_bearer_token" to it.bearerToken.isNotBlank(),
                "has_user" to true,
            )
        }
    }

    override suspend fun updateUserAccessStatus(bearerToken: String): Unit = logger.runSuspending(
        operation = "update_user_access_status",
        startFields = arrayOf(
            "environment" to environment,
            "method" to "PUT",
            "endpoint" to "auth/first-access",
        ),
        failureFields = { error ->
            arrayOf(
                "http_status" to (error as? HttpException)?.code(),
                "error_type" to error.javaClass.simpleName,
            )
        },
    ) {
        val response = api.updateFirstAccess("Bearer $bearerToken")

        logResponse("update_user_access_status", response.code(), response.isSuccessful)

        if (!response.isSuccessful) throw HttpException(response)
    }

    override suspend fun listAllApplications(bearerToken: String): List<Application> {
        return logger.runSuspending(
            operation = "list_all_applications",
            startFields = arrayOf(
                "environment" to environment,
                "method" to "GET",
                "endpoint" to "application/list-all",
            ),
            successFields = { applications ->
                arrayOf("application_count" to applications.size)
            },
            failureFields = { error ->
                arrayOf(
                    "http_status" to (error as? HttpException)?.code(),
                    "error_type" to error.javaClass.simpleName,
                )
            },
        ) {
            val response = api.listAllApplications("Bearer $bearerToken")

            logResponse("list_all_applications", response.code(), response.isSuccessful)

            if (!response.isSuccessful) throw HttpException(response)

            requireNotNull(response.body()) {
                "Successful application list response contained no body."
            }.map(ListAllApplicationsDto::toModel)
        }
    }

    override suspend fun getApplicationWithdrawalWindow(
        bearerToken: String,
        referenceDate: LocalDate,
        daysBefore: Int,
        daysAfter: Int,
    ): List<ApplicationWithdrawal> =
        logger.runSuspending(
            operation = "get_application_withdrawal_window",
            startFields = arrayOf(
                "environment" to environment,
                "method" to "GET",
                "endpoint" to "application/withdrawal-window",
                "reference_date" to referenceDate,
            ),
            successFields = { applications ->
                arrayOf("application_count" to applications.size)
            },
            failureFields = { error ->
                arrayOf(
                    "http_status" to (error as? HttpException)?.code(),
                    "error_type" to error.javaClass.simpleName,
                )
            },
        ) {
            val response = api.getApplicationWithdrawalWindow(
                authorization = "Bearer $bearerToken",
                referenceDate = referenceDate.toString(),
                daysBefore = daysBefore,
                daysAfter = daysAfter,
            )

            logResponse(
                "get_application_withdrawal_window",
                response.code(),
                response.isSuccessful,
            )

            if (!response.isSuccessful) throw HttpException(response)

            requireNotNull(response.body()) {
                "Successful application withdrawal window response contained no body."
            }.map(ApplicationWithdrawalDto::toModel)
        }

    private fun logResponse(operation: String, statusCode: Int, successful: Boolean) {
        logger.info(
            "${operation}_response_received",
            "http_status" to statusCode,
            "successful" to successful,
        )
    }
}
