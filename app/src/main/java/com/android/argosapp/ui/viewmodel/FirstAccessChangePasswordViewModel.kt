package com.android.argosapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.argosapp.auth.error.AuthErrorCode
import com.android.argosapp.auth.error.AuthErrorMapper
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.ArgosApiAuthManager
import com.android.argosapp.model.User
import com.android.argosapp.ui.event.AuthUiEvent
import com.android.argosapp.util.AppLogger
import com.android.argosapp.util.PasswordPatternValidator
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value) }
    }

    fun onPasswordConfirmationChanged(value: String) {
        _uiState.update { it.copy(passwordConfirmation = value) }
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
            AppLogger.debug(
                AppLogger.FIRST_ACCESS_TAG,
                "first_access_validation_failed",
                "code" to AuthErrorCode.INVALID_PASSWORD.name,
            )
            showError(AuthErrorCode.INVALID_PASSWORD)
            return
        }
        if (state.newPassword != state.passwordConfirmation) {
            AppLogger.debug(
                AppLogger.FIRST_ACCESS_TAG,
                "first_access_validation_failed",
                "code" to AuthErrorCode.PASSWORD_MISMATCH.name,
            )
            showError(AuthErrorCode.PASSWORD_MISMATCH)
            return
        }

        viewModelScope.launch {
            val bearerToken = argosApiAuthManager.getBearerToken()
            if (bearerToken.isNullOrBlank()) {
                AppLogger.debug(
                    AppLogger.FIRST_ACCESS_TAG,
                    "first_access_validation_failed",
                    "code" to AuthErrorCode.SESSION_EXPIRED.name,
                )
                showError(AuthErrorCode.SESSION_EXPIRED)
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            try {
                firebaseAuthManager.updatePassword(state.newPassword)
                argosApiAuthManager.updateUserAccessStatus(bearerToken)
                AppLogger.info(AppLogger.FIRST_ACCESS_TAG, "first_access_completed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPasswordChanged = true,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val code = AuthErrorMapper.from(error)
                AppLogger.error(
                    AppLogger.FIRST_ACCESS_TAG,
                    "first_access_failure",
                    error,
                    "code" to code.name,
                )
                showError(code)
            }
        }
    }

    private fun showError(code: AuthErrorCode) {
        _uiState.update { it.copy(isLoading = false) }
        _events.tryEmit(AuthUiEvent.ShowError(code))
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
