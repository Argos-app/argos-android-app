package com.android.argosapp.exception.auth

import androidx.credentials.exceptions.GetCredentialException
import com.google.firebase.auth.FirebaseAuthException
import java.io.IOException
import java.util.concurrent.CancellationException

object AuthErrorMapper {
    fun from(error: Throwable): AuthErrorCode {
        if (error is CancellationException) throw error

        return when (error) {
            is IOException -> AuthErrorCode.NETWORK
            is GetCredentialException -> AuthErrorCode.GOOGLE_CANCELLED
            is FirebaseAuthException -> mapFirebaseCode(error.errorCode)
            else -> AuthErrorCode.UNKNOWN
        }
    }

    private fun mapFirebaseCode(errorCode: String): AuthErrorCode = when (errorCode) {
        "ERROR_USER_NOT_FOUND" -> AuthErrorCode.USER_NOT_FOUND
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_CREDENTIALS" -> AuthErrorCode.INVALID_CREDENTIALS
        "ERROR_USER_TOKEN_EXPIRED",
        "ERROR_REQUIRES_RECENT_LOGIN" -> AuthErrorCode.SESSION_EXPIRED
        "ERROR_INVALID_OOB_CODE",
        "ERROR_EXPIRED_ACTION_CODE" -> AuthErrorCode.PASSWORD_RESET_INVALID
        else -> AuthErrorCode.UNKNOWN
    }
}
