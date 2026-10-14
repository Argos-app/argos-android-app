package com.android.argosapp.util.validation

class ProfileEditFormValidator {
    fun isValidName(value: String): Boolean =
        value.trim().isNotEmpty() && value.trim().length <= MAX_NAME_LENGTH

    fun isValidEmail(value: String): Boolean {
        val email = value.trim()
        return email.length <= MAX_EMAIL_LENGTH && EMAIL_PATTERN.matches(email)
    }

    fun isValidAddress(value: String): Boolean =
        value.trim().isNotEmpty() && value.trim().length <= MAX_ADDRESS_LENGTH

    companion object {
        const val MAX_NAME_LENGTH = 120
        const val MAX_EMAIL_LENGTH = 120
        const val MAX_ADDRESS_LENGTH = 512

        private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
