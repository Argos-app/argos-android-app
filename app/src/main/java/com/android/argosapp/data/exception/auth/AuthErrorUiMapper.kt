package com.android.argosapp.data.exception.auth

import androidx.annotation.StringRes
import com.android.argosapp.R

@StringRes
fun AuthErrorCode.messageRes(): Int = when (this) {
    AuthErrorCode.EMPTY_FIELDS -> R.string.auth_error_empty_fields
    AuthErrorCode.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthErrorCode.NETWORK -> R.string.auth_error_network
    AuthErrorCode.GOOGLE_CANCELLED -> R.string.auth_error_google
    AuthErrorCode.SESSION_EXPIRED -> R.string.auth_error_session_expired
    AuthErrorCode.INVALID_PASSWORD -> R.string.auth_error_invalid_password
    AuthErrorCode.PASSWORD_MISMATCH -> R.string.auth_error_password_mismatch
    AuthErrorCode.USER_NOT_FOUND -> R.string.auth_error_user_not_found
    AuthErrorCode.PASSWORD_RESET_INVALID -> R.string.auth_error_reset_invalid
    AuthErrorCode.UNKNOWN -> R.string.auth_error_unknown
}
