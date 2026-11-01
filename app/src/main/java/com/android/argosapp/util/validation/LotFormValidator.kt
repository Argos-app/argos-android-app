package com.android.argosapp.util.validation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

class LotFormValidator {
    enum class Field {
        LOT_NAME,
        HERD_NAME,
        BREED,
        CATEGORY,
        HEAD_COUNT,
        SLAUGHTER_DATE,
    }

    private fun isValidName(value: String, maxLength: Int): Boolean = value.trim().isNotEmpty() && value.trim().length <= maxLength

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
        herdId: String?,
        breed: String?,
        isNewHerd: Boolean,
        category: String,
        headCount: String,
        slaughterDate: String,
        availableCategories: Collection<String>,
    ): Set<Field> = buildSet {
        if (!isValidName(lotName, 20)) add(Field.LOT_NAME)
        if (isNewHerd && !isValidName(herdName, 60)) add(Field.HERD_NAME)
        if (isNewHerd && (breed == null || !isValidName(breed, 40))) add(Field.BREED)
        if (!isNewHerd && herdId.isNullOrBlank()) add(Field.HERD_NAME)
        if (!isValidCategory(category, availableCategories)) add(Field.CATEGORY)
        if (!isValidHeadCount(headCount)) add(Field.HEAD_COUNT)
        if (!isValidSlaughterDate(slaughterDate)) add(Field.SLAUGHTER_DATE)
    }

    companion object {
        private val SLAUGHTER_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT)
    }
}
