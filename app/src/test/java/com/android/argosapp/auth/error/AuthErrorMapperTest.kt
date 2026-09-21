package com.android.argosapp.auth.error

import com.google.firebase.auth.FirebaseAuthException
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthErrorMapperTest {
    @Test
    fun mapsNetworkFailure() {
        assertEquals(
            AuthErrorCode.NETWORK,
            AuthErrorMapper.from(IOException()),
        )
    }

    @Test
    fun mapsInvalidCredentials() {
        val error = FirebaseAuthException(
            "ERROR_WRONG_PASSWORD",
            "wrong password",
        )

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, AuthErrorMapper.from(error))
    }

    @Test
    fun mapsExpiredPasswordResetCode() {
        val error = FirebaseAuthException(
            "ERROR_EXPIRED_ACTION_CODE",
            "expired code",
        )

        assertEquals(AuthErrorCode.PASSWORD_RESET_INVALID, AuthErrorMapper.from(error))
    }

    @Test
    fun mapsUnknownFailure() {
        assertEquals(
            AuthErrorCode.UNKNOWN,
            AuthErrorMapper.from(IllegalStateException("unexpected")),
        )
    }

    @Test
    fun rethrowsCancellation() {
        val cancellation = CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            AuthErrorMapper.from(cancellation)
        }
    }
}
