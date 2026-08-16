package com.android.argosapp.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Inicia o fluxo do Google e devolve somente o ID token ao chamador. */
class GoogleSignInHelper(
    private val context: Context,
    private val webClientId: String,
    private val scope: CoroutineScope,
) {

    private val credentialManager = CredentialManager.create(context)

    fun launch(
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
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
            } catch (error: GetCredentialException) {
                onError(error)
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }
}
