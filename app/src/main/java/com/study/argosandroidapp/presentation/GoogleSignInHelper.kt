package com.study.argosandroidapp.presentation

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Abstração responsável por iniciar e processar o fluxo de autenticação via Google Sign-In
 * utilizando o Credential Manager — substituto moderno do GoogleSignIn depreciado.
 *
 * Não requer ActivityResultLauncher — o Credential Manager exibe um bottom sheet nativo.
 *
 * Exemplo de uso:
 * ```
 * val helper = GoogleSignInHelper(context, webClientId, scope)
 * helper.launch { idToken -> repository.googleLogin(idToken) }
 * ```
 *
 * @property context Contexto da Activity.
 * @property webClientId Client ID do projeto Firebase (R.string.default_web_client_id).
 * @property scope CoroutineScope para executar a requisição.
 */
class GoogleSignInHelper(
    private val context: Context,
    private val webClientId: String,
    private val scope: CoroutineScope
) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Inicia o fluxo de autenticação com o Google.
     *
     * @param onSuccess Callback invocado com o idToken ao concluir com sucesso.
     * @param onError Callback invocado com a exceção em caso de falha.
     */
    fun launch(
        onSuccess: (idToken: String) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  // mostra todas as contas, não só as já autorizadas
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                onSuccess(credential.idToken)
            } catch (e: GetCredentialException) {
                onError(e)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}