package com.android.argosapp.util.validation

class ApplicationFormValidator {
    fun isValidDose(value: String): Boolean =
        value.isNotEmpty() &&
            value.all { it in '0'..'9' } &&
            value.toLongOrNull()?.let { it > 0L } == true

    fun isValidEarTag(value: String): Boolean =
        value.length >= MIN_EAR_TAG_LENGTH && value.all { it in '0'..'9' }

    companion object {
        const val MIN_EAR_TAG_LENGTH = 2
    }
}
