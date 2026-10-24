package com.android.argosapp.data.repository

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.model.ChatMessageKind
import com.android.argosapp.model.ChatReply
import com.android.argosapp.model.ChatRequest
import com.android.argosapp.model.ChatResponseState
import com.android.argosapp.model.User
import com.android.argosapp.util.logging.RepositoryAppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Implementação local do repositório da API Argos para desenvolvimento e testes.
 */
class MockedArgosApiRepository(
    private val notFound: Boolean = false,
    firstAccess: Boolean = false,
) : ArgosApiRepository {
    private val logger = RepositoryAppLogger("MockedArgosApiRepository")
    private var isFirstAccess = firstAccess

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
                user = User(
                    userName = "Tobby",
                    userEmail = "tobby@argos.com",
                    farmName = "Tobby & CIA"
                ),
                isFirstAccess = isFirstAccess,
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

    override suspend fun getApplicationWithdrawalWindow(
        bearerToken: String,
        referenceDate: LocalDate,
        daysBefore: Int,
        daysAfter: Int,
    ): List<ApplicationWithdrawal> =
        logger.runSuspending("get_application_withdrawal_window") {
            delay(6000)

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

    override fun sendChatMessage(request: ChatRequest): Flow<ChatReply> = flow {
        delay(250)

        val previousPrompt = request.previousPrompt
        if (previousPrompt == ChatMessageKind.CONFIRMATION && request.confirmed == null) {
            emit(
                ChatReply.Prompt(
                    text = "Confirme para continuar\n\nEnviar o relatório por e-mail aos responsáveis autorizados?\n\nO envio só continua após a sua confirmação.",
                    kind = ChatMessageKind.CONFIRMATION,
                ),
            )
            return@flow
        }
        val prompt = when {
            previousPrompt == ChatMessageKind.SINGLE_CHOICE ||
                previousPrompt == ChatMessageKind.CONFIRMATION ||
                previousPrompt == ChatMessageKind.FREE_TEXT_PROMPT -> null
            request.text.contains("relat", ignoreCase = true) &&
                (request.text.contains("email", ignoreCase = true) || request.text.contains("e-mail", ignoreCase = true)) ->
                ChatReply.Prompt(
                    text = "Confirme para continuar\n\nEnviar o relatório por e-mail aos responsáveis autorizados?\n\nO envio só continua após a sua confirmação.",
                    kind = ChatMessageKind.CONFIRMATION,
                )
            request.text.contains("rebanho", ignoreCase = true) ||
                request.text.contains("resumo", ignoreCase = true) ->
                ChatReply.Prompt(
                    text = "Tobby precisa de uma informação\n\nDe qual fazenda você quer o resumo?\n\nEscolha uma das opções abaixo.",
                    kind = ChatMessageKind.SINGLE_CHOICE,
                    options = listOf(
                        "Fazenda São Bento",
                        "Fazenda Santa Clara",
                        "Fazenda Boa Vista",
                        "Fazenda São José",
                    ),
                )
            request.text.contains("lote", ignoreCase = true) ||
                request.text.contains("consumo", ignoreCase = true) ||
                request.text.contains("medicamento", ignoreCase = true) ->
                ChatReply.Prompt(
                    text = "Só falta um detalhe\n\nQual lote você quer consultar?\n\nDigite o nome ou o código do lote no campo abaixo.",
                    kind = ChatMessageKind.FREE_TEXT_PROMPT,
                )
            else -> null
        }

        if (prompt != null) {
            emit(prompt)
            return@flow
        }

        val partialText = when {
            previousPrompt == ChatMessageKind.CONFIRMATION && request.confirmed == false ->
                "Tudo bem, não vou enviar o relatório."
            previousPrompt == ChatMessageKind.CONFIRMATION ->
                "O relatório do lote L-10 foi preparado."
            previousPrompt == ChatMessageKind.SINGLE_CHOICE ->
                "${request.text} possui 24 animais cadastrados."
            else -> "O lote L-10 possui 24 animais cadastrados."
        }
        val completedText = when {
            previousPrompt == ChatMessageKind.CONFIRMATION && request.confirmed == false ->
                "Tudo bem, o relatório não foi enviado. Como posso ajudar agora?"
            previousPrompt == ChatMessageKind.CONFIRMATION ->
                "O relatório do lote L-10 foi enviado por e-mail aos responsáveis autorizados. Como posso ajudar agora?"
            previousPrompt == ChatMessageKind.SINGLE_CHOICE ->
                "${request.text} possui 24 animais cadastrados.\n\n" +
                    "Você pode consultar os detalhes do rebanho, conferir medicamentos ou pedir uma previsão de consumo.\n\n" +
                    "Como posso ajudar agora?"
            else -> "O lote L-10 possui 24 animais cadastrados.\n\n" +
                "Você pode consultar os detalhes do rebanho, conferir medicamentos ou pedir uma previsão de consumo.\n\n" +
                "Como posso ajudar agora?"
        }

        emit(ChatReply.Answer(partialText, ChatResponseState.PARTIAL))
        delay(1_200)
        emit(ChatReply.Answer(completedText, ChatResponseState.COMPLETED))
    }

}
