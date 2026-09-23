package com.android.argosapp.ui.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.ui.screen.RedefinePasswordFlowState
import com.android.argosapp.util.PasswordPatternValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RedefinePasswordState(
    val flowState: RedefinePasswordFlowState = RedefinePasswordFlowState.REQUEST_CODE,
    val email: String = "",
    val newPassword: String = "",
    val passwordConfirmation: String = "",
    val newPasswordVisible: Boolean = false,
    val passwordConfirmationVisible: Boolean = false,
    val resetCode: String? = null,
    val resendSecondsRemaining: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorEventId: Long = 0,
)

sealed interface RedefinePasswordEvent {
    data object NavigateToLogin : RedefinePasswordEvent
}

class RedefinePasswordViewModel(
    private val firebaseAuthManager: FirebaseAuthManager,
    context: Context,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val passwordValidator = PasswordPatternValidator()
    private val _uiState = MutableStateFlow(RedefinePasswordState())
    val uiState: StateFlow<RedefinePasswordState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<RedefinePasswordEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, error = null) }
    }

    fun onPasswordConfirmationChanged(value: String) {
        _uiState.update { it.copy(passwordConfirmation = value, error = null) }
    }

    fun onNewPasswordVisibilityChanged() {
        _uiState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    fun onPasswordConfirmationVisibilityChanged() {
        _uiState.update { it.copy(passwordConfirmationVisible = !it.passwordConfirmationVisible) }
    }

    fun sendResetEmail() {
        val email = uiState.value.email.trim()
        if (email.isBlank()) {
            showError("Informe seu e-mail.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { firebaseAuthManager.redefinePassword(email) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            email = email,
                            flowState = RedefinePasswordFlowState.EMAIL_SENT,
                            resendSecondsRemaining = RESEND_DELAY_SECONDS,
                            isLoading = false,
                        )
                    }
                    startResendTimer()
                }
                .onFailure { error ->
                    showError(error.message ?: "Não foi possível enviar o e-mail.")
                }
        }
    }

    fun resendResetEmail() {
        if (uiState.value.resendSecondsRemaining == 0) sendResetEmail()
    }

    fun onOpenEmailAppClick() {
        try {
            appContext.startActivity(
                Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_EMAIL,
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
            showError("Nenhum aplicativo de e-mail está instalado.")
        }
    }

    fun onPasswordResetCodeReceived(oobCode: String) {
        if (oobCode.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { firebaseAuthManager.verifyPasswordResetCode(oobCode) }
                .onSuccess { email ->
                    _uiState.update {
                        it.copy(
                            email = email,
                            resetCode = oobCode,
                            flowState = RedefinePasswordFlowState.CHANGE_PASSWORD,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    showError(error.message ?: "O link de redefinição é inválido ou expirou.")
                }
        }
    }

    fun changePassword() {
        val state = uiState.value
        val resetCode = state.resetCode
        when {
            resetCode.isNullOrBlank() -> {
                showError("Abra o link recebido por e-mail para redefinir sua senha.")
                return
            }

            !passwordValidator.validatePassword(state.newPassword).values.all { it } -> {
                showError("Atenda aos critérios da nova senha.")
                return
            }

            state.newPassword != state.passwordConfirmation -> {
                showError("As senhas informadas não coincidem.")
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { firebaseAuthManager.confirmPasswordReset(resetCode, state.newPassword) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            flowState = RedefinePasswordFlowState.PASSWORD_CHANGED,
                            newPassword = "",
                            passwordConfirmation = "",
                            resetCode = null,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    showError(error.message ?: "Não foi possível alterar a senha.")
                }
        }
    }

    fun onBackClick() {
        if (uiState.value.flowState == RedefinePasswordFlowState.REQUEST_CODE) {
            _events.tryEmit(RedefinePasswordEvent.NavigateToLogin)
            return
        }

        _uiState.update { state ->
            when (state.flowState) {
                RedefinePasswordFlowState.EMAIL_SENT -> state.copy(
                    flowState = RedefinePasswordFlowState.REQUEST_CODE,
                    error = null,
                )

                RedefinePasswordFlowState.CHANGE_PASSWORD -> state.copy(
                    flowState = RedefinePasswordFlowState.EMAIL_SENT,
                    resetCode = null,
                    error = null,
                )

                else -> state
            }
        }
    }

    private fun startResendTimer() {
        viewModelScope.launch {
            for (seconds in RESEND_DELAY_SECONDS downTo 0) {
                _uiState.update { it.copy(resendSecondsRemaining = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = message,
                errorEventId = it.errorEventId + 1,
            )
        }
    }

    companion object {
        private const val RESEND_DELAY_SECONDS = 60

        fun factory(
            firebaseAuthManager: FirebaseAuthManager,
            context: Context,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RedefinePasswordViewModel(firebaseAuthManager, context) as T
                }
            }
    }
}
