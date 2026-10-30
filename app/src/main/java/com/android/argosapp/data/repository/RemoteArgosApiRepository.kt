package com.android.argosapp.data.repository

import com.android.argosapp.data.dto.ArgosApiAuthResponse
import com.android.argosapp.data.dto.ListAllApplicationsDto
import com.android.argosapp.data.dto.RegisterLotApplicationDto
import com.android.argosapp.data.dto.RegisterLotDto
import com.android.argosapp.data.dto.LotDto
import com.android.argosapp.data.dto.MedicinePageDto
import com.android.argosapp.data.dto.AnimalExceptionPageDto
import com.android.argosapp.data.dto.ApplicationWithdrawalDto
import com.android.argosapp.data.dto.UpdateCurrentUserDto
import com.android.argosapp.model.Application
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.util.logging.RepositoryAppLogger
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PUT
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private interface ArgosApiService {
    @GET("lot/list-all")
    suspend fun listLots(@Header("Authorization") authorization: String): Response<List<LotDto>>

    @GET("medicines")
    suspend fun listMedicines(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<MedicinePageDto>

    @GET("animal-exceptions")
    suspend fun listAnimalExceptions(
        @Header("Authorization") authorization: String,
        @Query("active") active: Boolean?,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<AnimalExceptionPageDto>

    @POST("lot")
    suspend fun registerLot(
        @Header("Authorization") authorization: String,
        @Body data: RegisterLotDto,
    ): Response<Unit>
    @POST("application")
    suspend fun registerLotApplication(
        @Header("Authorization") authorization: String,
        @Header("userId") userId: String,
        @Body data: RegisterLotApplicationDto,
    ): Response<Unit>

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

    @PATCH("users/update-profile")
    suspend fun updateCurrentUser(
        @Header("Authorization") authorization: String,
        @Body data: UpdateCurrentUserDto,
    ): Response<Unit>
}

/** Implementação Retrofit da comunicação com a API Argos. */
class RemoteArgosApiRepository(
    homologation: Boolean = false,
) : ArgosApiRepository {

    private companion object {
        const val PRODUCTION_BASE_URL = "https://argos-backend.discloud.app/api/v1/"
        const val HOMOLOGATION_BASE_URL = "https://argos-backend-hmg.discloud.app/api/v1/"
        const val SECONDS_TO_TIMEOUT = 30L
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

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(SECONDS_TO_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(SECONDS_TO_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(SECONDS_TO_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    private val api by lazy {
        Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ArgosApiService::class.java)
    }

    override suspend fun registerLotApplication(
        bearerToken: String,
        userId: String,
        data: RegisterLotApplicationDto,
    ): Unit = logger.runSuspending(
        operation = "register_lot_application",
        failureFields = ::httpFailureFields,
    ) {
        val response = api.registerLotApplication("Bearer $bearerToken", userId, data)
        logResponse("register_lot_application", response.code(), response.isSuccessful)
        if (!response.isSuccessful) throw HttpException(response)
    }

    override suspend fun registerLot(bearerToken: String, data: RegisterLotDto): Unit = logger.runSuspending(
        operation = "register_lot",
        startFields = arrayOf(
            "environment" to environment,
            "method" to "POST",
            "endpoint" to "lot",
            "has_lot_name" to data.lotName.isNotBlank(),
            "has_herd_name" to data.herdName.isNotBlank(),
            "category" to data.category,
            "head_count" to data.headCount,
            "release_slaughter_date" to data.releaseSlaughterDate,
        ),
        failureFields = ::httpFailureFields,
    ) {
        val response = api.registerLot("Bearer $bearerToken", data)
        logResponse("register_lot", response.code(), response.isSuccessful)
        if (!response.isSuccessful) throw HttpException(response)
    }
    override suspend fun listLots(bearerToken: String): List<LotDto> = logger.runSuspending(
        operation = "list_application_lots",
        startFields = arrayOf("environment" to environment, "method" to "GET", "endpoint" to "lot/list-all"),
        failureFields = ::httpFailureFields,
    ) {
        val response = api.listLots("Bearer $bearerToken")

        logResponse("list_application_lots", response.code(), response.isSuccessful)

        if (!response.isSuccessful) throw HttpException(response)

        requireNotNull(response.body()) { "Successful lot list response contained no body." }
    }

    override suspend fun getMedicinesPage(
        bearerToken: String,
        page: Int,
        size: Int,
    ): MedicinePageDto = logger.runSuspending(
        operation = "get_medicines_page",
        startFields = arrayOf(
            "environment" to environment,
            "method" to "GET",
            "endpoint" to "medicines",
            "page" to page,
            "size" to size,
        ),
        failureFields = ::httpFailureFields,
    ) {
        val response = api.listMedicines("Bearer $bearerToken", page, size)
        logResponse("get_medicines_page", response.code(), response.isSuccessful)
        if (!response.isSuccessful) throw HttpException(response)
        requireNotNull(response.body()) { "Successful medicine page response contained no body." }
    }

    override suspend fun getAnimalExceptionsPage(
        bearerToken: String,
        active: Boolean?,
        page: Int,
        size: Int,
    ): AnimalExceptionPageDto = logger.runSuspending(
        operation = "get_animal_exceptions_page",
        startFields = arrayOf(
            "environment" to environment,
            "method" to "GET",
            "endpoint" to "animal-exceptions",
            "active" to active,
            "page" to page,
            "size" to size,
        ),
        failureFields = ::httpFailureFields,
    ) {
        val response = api.listAnimalExceptions("Bearer $bearerToken", active, page, size)
        logResponse("get_animal_exceptions_page", response.code(), response.isSuccessful)
        if (!response.isSuccessful) throw HttpException(response)
        requireNotNull(response.body()) { "Successful animal exception page response contained no body." }
    }
    override suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse = logger.runSuspending(
        operation = "authenticate",
        startFields = arrayOf(
            "environment" to environment,
            "method" to "POST",
            "endpoint" to "auth/signin",
        ),
        failureFields = ::httpFailureFields,


    ) {
        val response = api.signIn("Bearer $firebaseIdToken")

        logResponse("authenticate", response.code(), response.isSuccessful)

        if (!response.isSuccessful) throw HttpException(response)

        val body = requireNotNull(response.body()) {
            "Successful sign-in response contained no body."
        }

        ArgosApiAuthResponse(
            bearerToken = body.bearerToken,
            userName = body.userName,
            email = body.email,
            farmName = body.farmName,
            isFirstAccess = body.isFirstAccess,
            userId = body.userId,
            photoUrl = body.photoUrl,
            cpf = body.cpf,
            address = body.address,
            role = body.role
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
        failureFields = ::httpFailureFields,
    ) {
        val response = api.updateFirstAccess("Bearer $bearerToken")

        logResponse("update_user_access_status", response.code(), response.isSuccessful)

        if (!response.isSuccessful) throw HttpException(response)
    }

    override suspend fun updateCurrentUser(bearerToken: String, data: UpdateCurrentUserDto): Unit = logger.runSuspending(
        operation = "update_current_user",
        startFields = arrayOf(
            "environment" to environment,
            "method" to "PATCH",
            "endpoint" to "users/update-profile",
            "has_name" to (data.name != null),
            "has_email" to (data.email != null),
            "has_photo_url" to (data.photoUrl != null),
            "has_address" to  (data.address != null)
        ),
        failureFields = ::httpFailureFields,
    ) {
        val response = api.updateCurrentUser("Bearer $bearerToken", data)

        logResponse("update_current_user", response.code(), response.isSuccessful)

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
            failureFields = ::httpFailureFields,
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
            failureFields = ::httpFailureFields,
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

    private fun httpFailureFields(error: Throwable): Array<out Pair<String, Any?>> {
        val httpError = error as? HttpException
        val response = httpError?.response()
        val body = response?.errorBody()?.string()?.trim()?.takeIf(String::isNotEmpty)
        val responseMessage = body?.let { responseBody ->
            val parsedMessage = runCatching {
                extractApiErrorMessage(JSONObject(responseBody))
            }.getOrNull() ?: runCatching {
                extractApiErrorMessage(JSONArray(responseBody))
            }.getOrNull()
            (parsedMessage ?: responseBody).replace(Regex("[\\r\\n\\t]+"), " ").take(500)
        } ?: response?.message()?.takeIf(String::isNotBlank)

        return arrayOf(
            "http_status" to httpError?.code(),
            "response_message" to responseMessage,
            "error_type" to error.javaClass.simpleName,
        )
    }

    private fun extractApiErrorMessage(value: Any?): String? {
        if (value is JSONObject) {
            val keys = value.keys().asSequence().toList()
            val messageKeys = listOf("message", "error_description", "error", "detail", "title", "description", "msg")
            for (messageKey in messageKeys) {
                val actualKey = keys.firstOrNull { it.equals(messageKey, ignoreCase = true) } ?: continue
                val message = value.opt(actualKey)
                if (message is String && message.isNotBlank()) return message
                extractApiErrorMessage(message)?.let { return it }
            }
            keys.forEach { key -> extractApiErrorMessage(value.opt(key))?.let { return it } }
        } else if (value is JSONArray) {
            for (index in 0 until value.length()) {
                extractApiErrorMessage(value.opt(index))?.let { return it }
            }
        }
        return null
    }
    private fun logResponse(operation: String, statusCode: Int, successful: Boolean) {
        logger.info(
            "${operation}_response_received",
            "http_status" to statusCode,
            "successful" to successful,
        )
    }

}
