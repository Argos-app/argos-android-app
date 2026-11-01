<<<<<<<< HEAD:app/src/main/java/com/android/argosapp/data/exception/auth/AuthErrorCode.kt
package com.android.argosapp.data.exception.auth
========
package com.android.argosapp.exception.auth
>>>>>>>> origin/develop:app/src/main/java/com/android/argosapp/exception/auth/AuthErrorCode.kt

enum class AuthErrorCode {
    EMPTY_FIELDS,
    INVALID_CREDENTIALS,
    NETWORK,
    GOOGLE_CANCELLED,
    SESSION_EXPIRED,
    INVALID_PASSWORD,
    PASSWORD_MISMATCH,
    USER_NOT_FOUND,
    PASSWORD_RESET_INVALID,
    UNKNOWN,
}
