package com.android.argosapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.ArgosApiAuthManager
import com.android.argosapp.model.User
import com.android.argosapp.util.PasswordPatternValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FirstAccessChangePasswordState(
    val user: User? = null,
    val newPassword: String = "",
    val passwordConfirmation: String = "",
    val newPasswordVisible: Boolean = false,
    val passwordConfirmationVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isPasswordChanged: Boolean = false,
    val error: String? = null,
    val errorEventId: Long = 0,
)

class FirstAccessChangePasswordViewModel(
    private val firebaseAuthManager: FirebaseAuthManager,
    private val argosApiAuthManager: ArgosApiAuthManager,
) : ViewModel() {

    private val passwordValidator = PasswordPatternValidator()

    private val _uiState = MutableStateFlow(
        FirstAccessChangePasswordState(user = argosApiAuthManager.getUser()),
    )
    val uiState: StateFlow<FirstAccessChangePasswordState> = _uiState.asStateFlow()

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
        _uiState.update {
            it.copy(passwordConfirmationVisible = !it.passwordConfirmationVisible)
        }
    }

    fun changePassword() {
        val state = uiState.value
        val passwordRules = passwordValidator.validatePassword(state.newPassword)
        if (!passwordRules.values.all { it }) {
            showError("Atenda aos critérios da nova senha.")
            return
        }
        if (state.newPassword != state.passwordConfirmation) {
            showError("As senhas informadas não coincidem.")
            return
        }

        viewModelScope.launch {
            val bearerToken = argosApiAuthManager.getBearerToken()
            if (bearerToken.isNullOrBlank()) {
                showError("Sua sessão expirou. Entre novamente.")
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                firebaseAuthManager.updatePassword(state.newPassword)
                argosApiAuthManager.updateUserAccessStatus()
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPasswordChanged = true,
                        )
                    }
                }
                .onFailure { throwable ->
                    showError(
                        message = throwable.message ?: "Não foi possível alterar a senha.",
                        isLoading = false,
                    )
                }
        }
    }

    private fun showError(message: String, isLoading: Boolean = false) {
        _uiState.update {
            it.copy(
                isLoading = isLoading,
                error = message,
                errorEventId = it.errorEventId + 1,
            )
        }
    }

    companion object {
        fun factory(
            firebaseAuthManager: FirebaseAuthManager,
            argosApiAuthManager: ArgosApiAuthManager,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FirstAccessChangePasswordViewModel(
                        firebaseAuthManager = firebaseAuthManager,
                        argosApiAuthManager = argosApiAuthManager,
                    ) as T
                }
            }
    }
}
