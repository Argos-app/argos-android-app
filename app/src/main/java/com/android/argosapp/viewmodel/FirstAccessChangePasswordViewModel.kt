package com.android.argosapp.viewmodel

import com.android.argosapp.data.exception.auth.AuthErrorCode
import com.android.argosapp.data.exception.auth.AuthErrorMapper
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.util.logging.AppLogger
import com.android.argosapp.util.validation.PasswordPatternValidator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class FirstAccessChangePasswordViewModel(private val authManager: AuthManager) : ViewModel() {
    private val passwordValidator = PasswordPatternValidator()

    fun changePassword(
        newPassword: String,
        confirmation: String,
        onLoading: (Boolean) -> Unit,
        onError: (AuthErrorCode) -> Unit,
        onSuccess: () -> Unit,
    ) {
        when {
            !passwordValidator.validatePassword(newPassword).values.all { it } -> {
                AppLogger.debug(
                    AppLogger.FIRST_ACCESS_TAG,
                    "first_access_validation_rejected",
                    "code" to AuthErrorCode.INVALID_PASSWORD.name,
                )
                return onError(AuthErrorCode.INVALID_PASSWORD)
            }
            newPassword != confirmation -> {
                AppLogger.debug(
                    AppLogger.FIRST_ACCESS_TAG,
                    "first_access_validation_rejected",
                    "code" to AuthErrorCode.PASSWORD_MISMATCH.name,
                )
                return onError(AuthErrorCode.PASSWORD_MISMATCH)
            }
            authManager.getBearerToken().isNullOrBlank() -> {
                AppLogger.debug(
                    AppLogger.FIRST_ACCESS_TAG,
                    "first_access_validation_rejected",
                    "code" to AuthErrorCode.SESSION_EXPIRED.name,
                )
                return onError(AuthErrorCode.SESSION_EXPIRED)
            }
        }
        viewModelScope.launch {
            onLoading(true)
            runCatching {
                authManager.updatePassword(newPassword)
                authManager.updateUserAccessStatus()
            }.onSuccess {
                AppLogger.info(AppLogger.FIRST_ACCESS_TAG, "first_access_completed")
                onSuccess()
            }.onFailure { error ->
                val code = AuthErrorMapper.from(error)
                AppLogger.error(
                    AppLogger.FIRST_ACCESS_TAG,
                    "first_access_failure",
                    error,
                    "code" to code.name,
                )
                onError(code)
            }
            onLoading(false)
        }
    }
}
