package com.android.argosapp.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.util.validation.PasswordPatternValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RedefinePasswordViewModel(
    private val authManager: AuthManager,
    private val context: Context,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val passwordValidator = PasswordPatternValidator()

    fun sendResetEmail(email: String, onLoading: (Boolean) -> Unit, onSent: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank()) return onError("Informe seu e-mail.")
        scope.launch {
            onLoading(true)
            runCatching { authManager.redefinePassword(email.trim()) }
                .onSuccess { onSent() }
                .onFailure { onError(it.message ?: "Não foi possível enviar o e-mail.") }
            onLoading(false)
        }
    }

    fun verifyResetCode(code: String, onLoading: (Boolean) -> Unit, onValid: (String) -> Unit, onError: (String) -> Unit) {
        if (code.isBlank()) return
        scope.launch {
            onLoading(true)
            runCatching { authManager.verifyPasswordResetCode(code) }
                .onSuccess { onValid(code) }
                .onFailure { onError(it.message ?: "O link de redefinição é inválido ou expirou.") }
            onLoading(false)
        }
    }

    fun changePassword(code: String, password: String, confirmation: String, onLoading: (Boolean) -> Unit, onSuccess: () -> Unit, onError: (String) -> Unit) {
        when {
            code.isBlank() -> return onError("Abra o link recebido por e-mail para redefinir sua senha.")
            !passwordValidator.validatePassword(password).values.all { it } -> return onError("Atenda aos critérios da nova senha.")
            password != confirmation -> return onError("As senhas informadas não coincidem.")
        }
        scope.launch {
            onLoading(true)
            runCatching { authManager.confirmPasswordReset(code, password) }
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Não foi possível alterar a senha.") }
            onLoading(false)
        }
    }

    fun openEmailApp(onError: (String) -> Unit) {
        try {
            context.startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            onError("Nenhum aplicativo de e-mail está instalado.")
        }
    }
}
