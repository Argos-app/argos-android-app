package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.dto.ApplicationWithdrawalDto
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockPage
import com.android.argosapp.model.StockProduct
import com.android.argosapp.model.MedicationStockRegistration
import com.android.argosapp.model.MedicationStockItem
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
import retrofit2.http.Body
import java.time.LocalDate
import java.math.BigDecimal

private data class MedicinePageDto(
    val data: List<MedicineListItemDto> = emptyList(),
    val totalPages: Int = 0,
)

private data class MedicineListItemDto(
    val medicineBatchId: String,
    val medicineName: String,
    val expiryDate: String,
    val quantity: BigDecimal,
    val therapeuticCategory: String? = null,
) {
    fun toModel() = MedicationStockItem(
        id = medicineBatchId,
        medicationName = medicineName,
        expirationDate = LocalDate.parse(expiryDate),
        quantity = quantity.stripTrailingZeros().toPlainString(),
        category = if (therapeuticCategory.orEmpty().contains("vacina", ignoreCase = true)) {
            StockCategory.VACCINE
        } else {
            StockCategory.MEDICATION
        },
    )
}

private data class MedicineBatchRequestDto(
    val productRegistration: String,
    val batchNumber: String,
    val manufactureDate: String,
    val expiryDate: String,
    val quantity: BigDecimal,
    val supplierName: String,
    val supplierCnpj: String,
    val propertyId: String,
) {
    constructor(registration: MedicationStockRegistration) : this(
        productRegistration = registration.productRegistration,
        batchNumber = registration.batchNumber,
        manufactureDate = registration.manufactureDate.toString(),
        expiryDate = registration.expiryDate.toString(),
        quantity = registration.quantity,
        supplierName = registration.supplierName,
        supplierCnpj = registration.supplierCnpj,
        propertyId = registration.propertyId,
    )
}

private interface ArgosApiService {
    @GET("medicines")
    suspend fun getMedicines(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<MedicinePageDto>

    @POST("medicines")
    suspend fun registerMedicine(
        @Header("Authorization") authorization: String,
        @Body registration: MedicineBatchRequestDto,
    ): Response<Unit>

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
    private val bearerTokenProvider: () -> String? = { null },
) : ArgosApiRepository {
    override val isMedicationStockImplemented = true

    override suspend fun getMedicationStockPage(
        query: String,
        category: StockCategory?,
        cursor: String?,
        pageSize: Int,
    ): StockPage {
        val token = requireApiToken()
        val requestedPage = cursor?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val effectivePageSize = pageSize.coerceIn(1, 100)
        val items = mutableListOf<MedicationStockItem>()
        var page = requestedPage
        var totalPages = 0
        do {
            val response = api.getMedicines("Bearer $token", page, effectivePageSize)
            if (!response.isSuccessful) throw HttpException(response)
            val body = requireNotNull(response.body()) { "A API retornou uma página de medicamentos vazia." }
            totalPages = body.totalPages
            items += body.data.map(MedicineListItemDto::toModel).filter { item ->
                (query.isBlank() || item.medicationName.contains(query.trim(), ignoreCase = true)) &&
                    (category == null || item.category == category)
            }
            page++
        } while ((query.isNotBlank() || category != null) && items.size < effectivePageSize && page < totalPages)

        return StockPage(
            items = items,
            nextCursor = page.takeIf { it < totalPages }?.toString(),
        )
    }

    override suspend fun getMedicationProducts(): List<StockProduct> {
        // O backend ainda não disponibilizou uma rota de catálogo de produtos/MAPA.
        return emptyList()
    }

    override suspend fun registerMedicationStock(registration: MedicationStockRegistration) {
        require(registration.propertyId.isNotBlank()) { "Informe o propertyId antes de registrar o estoque." }
        val response = api.registerMedicine(
            authorization = "Bearer ${requireApiToken()}",
            registration = MedicineBatchRequestDto(registration),
        )
        if (!response.isSuccessful) throw HttpException(response)
    }

    private fun requireApiToken(): String =
        requireNotNull(bearerTokenProvider()?.takeIf(String::isNotBlank)) {
            "A sessão não possui bearer token da API Argos. Entre novamente antes de consultar medicamentos."
        }

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
