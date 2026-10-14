package com.android.argosapp.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.android.argosapp.exception.auth.AuthErrorCode
import com.android.argosapp.exception.auth.AuthErrorMapper
import com.android.argosapp.exception.auth.messageRes
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.util.validation.PasswordPatternValidator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RedefinePasswordViewModel(
    private val authManager: AuthManager,
    private val context: Context,
) : ViewModel() {
    private val passwordValidator = PasswordPatternValidator()

    fun sendResetEmail(email: String, profileFlow: Boolean, onLoading: (Boolean) -> Unit, onSent: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank()) return onError("Informe seu e-mail.")
        viewModelScope.launch {
            onLoading(true)
            runCatching { authManager.redefinePassword(email.trim(), profileFlow) }
                .onSuccess { onSent() }
                .onFailure { onError(errorMessage(it)) }
            onLoading(false)
        }
    }

    fun verifyResetCode(code: String, onLoading: (Boolean) -> Unit, onValid: (String) -> Unit, onError: (String) -> Unit) {
        if (code.isBlank()) return
        viewModelScope.launch {
            onLoading(true)
            runCatching { authManager.verifyPasswordResetCode(code) }
                .onSuccess { onValid(code) }
                .onFailure { onError(errorMessage(it, AuthErrorCode.PASSWORD_RESET_INVALID)) }
            onLoading(false)
        }
    }

    fun changePassword(code: String, password: String, confirmation: String, onLoading: (Boolean) -> Unit, onSuccess: () -> Unit, onError: (String) -> Unit) {
        when {
            code.isBlank() -> return onError("Abra o link recebido por e-mail para redefinir sua senha.")
            !passwordValidator.validatePassword(password).values.all { it } -> return onError("Atenda aos critérios da nova senha.")
            password != confirmation -> return onError("As senhas informadas não coincidem.")
        }
        viewModelScope.launch {
            onLoading(true)
            runCatching { authManager.confirmPasswordReset(code, password) }
                .onSuccess { onSuccess() }
                .onFailure { onError(errorMessage(it)) }
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

    private fun errorMessage(error: Throwable, fallback: AuthErrorCode = AuthErrorCode.UNKNOWN): String {
        val errorCode = AuthErrorMapper.from(error).let { if (it == AuthErrorCode.UNKNOWN) fallback else it }
        return context.getString(errorCode.messageRes())
    }
}
