package com.android.argosapp.util.validation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

class LotFormValidator {
    enum class Field {
        LOT_NAME,
        HERD_NAME,
        CATEGORY,
        HEAD_COUNT,
        SLAUGHTER_DATE,
    }

    fun isValidName(value: String): Boolean = value.trim().isNotEmpty() && value.trim().length <= MAX_NAME_LENGTH

    fun isValidCategory(value: String, availableCategories: Collection<String>): Boolean =
        value in availableCategories

    fun isValidHeadCount(value: String): Boolean = value.toIntOrNull()?.let { it > 0 } == true

    fun isValidSlaughterDate(value: String): Boolean = try {
        LocalDate.parse(value.trim(), SLAUGHTER_DATE_FORMATTER).year > 0
    } catch (_: DateTimeParseException) {
        false
    }

    fun invalidFields(
        lotName: String,
        herdName: String,
        category: String,
        headCount: String,
        slaughterDate: String,
        availableCategories: Collection<String>,
    ): Set<Field> = buildSet {
        if (!isValidName(lotName)) add(Field.LOT_NAME)
        if (!isValidName(herdName)) add(Field.HERD_NAME)
        if (!isValidCategory(category, availableCategories)) add(Field.CATEGORY)
        if (!isValidHeadCount(headCount)) add(Field.HEAD_COUNT)
        if (!isValidSlaughterDate(slaughterDate)) add(Field.SLAUGHTER_DATE)
    }

    companion object {
        const val MAX_NAME_LENGTH = 120
        private val SLAUGHTER_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT)
    }
}
