package com.android.argosapp.auth.error

import com.android.argosapp.exception.auth.AuthErrorCode
import com.android.argosapp.exception.auth.AuthErrorMapper
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
        assertEquals(
            AuthErrorCode.INVALID_CREDENTIALS,
            AuthErrorMapper.fromFirebaseCode("ERROR_WRONG_PASSWORD"),
        )
    }

    @Test
    fun mapsExpiredPasswordResetCode() {
        assertEquals(
            AuthErrorCode.PASSWORD_RESET_INVALID,
            AuthErrorMapper.fromFirebaseCode("ERROR_EXPIRED_ACTION_CODE"),
        )
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
