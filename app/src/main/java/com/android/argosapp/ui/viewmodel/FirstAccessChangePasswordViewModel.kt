package com.android.argosapp.ui.viewmodel

import com.android.argosapp.auth.error.AuthErrorCode
import com.android.argosapp.auth.error.AuthErrorMapper
import com.android.argosapp.auth.manager.AuthManager
import com.android.argosapp.util.AppLogger
import com.android.argosapp.util.PasswordPatternValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirstAccessChangePasswordViewModel(private val authManager: AuthManager) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val passwordValidator = PasswordPatternValidator()
    val userName = authManager.getUser()?.userName.orEmpty()

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
        scope.launch {
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
