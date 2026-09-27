package com.android.argosapp.ui.viewmodel

import android.content.Context
import com.android.argosapp.auth.error.AuthErrorCode
import com.android.argosapp.auth.error.AuthErrorMapper
import com.android.argosapp.auth.manager.ArgosApiAuthManager
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.util.PasswordPatternValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirstAccessChangePasswordViewModel(
    context: Context,
    private val firebaseAuthManager: FirebaseAuthManager,
    private val argosApiAuthManager: ArgosApiAuthManager,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val passwordValidator = PasswordPatternValidator()
    val userName = argosApiAuthManager.getUser()?.userName.orEmpty()

    fun changePassword(
        newPassword: String,
        confirmation: String,
        onLoading: (Boolean) -> Unit,
        onError: (AuthErrorCode) -> Unit,
        onSuccess: () -> Unit,
    ) {
        when {
            !passwordValidator.validatePassword(newPassword).values.all { it } -> return onError(AuthErrorCode.INVALID_PASSWORD)
            newPassword != confirmation -> return onError(AuthErrorCode.PASSWORD_MISMATCH)
            argosApiAuthManager.getBearerToken().isNullOrBlank() -> return onError(AuthErrorCode.SESSION_EXPIRED)
        }
        scope.launch {
            onLoading(true)
            runCatching {
                firebaseAuthManager.updatePassword(newPassword)
                argosApiAuthManager.updateUserAccessStatus()
            }.onSuccess { onSuccess() }
                .onFailure { onError(AuthErrorMapper.from(it)) }
            onLoading(false)
        }
    }
}
