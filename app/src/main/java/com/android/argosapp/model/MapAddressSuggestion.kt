package com.android.argosapp.model

data class MapAddressSuggestion(
    val primaryText: String,
    val secondaryText: String,
    val placeId: String,
) {
    val description: String
        get() = listOf(primaryText, secondaryText)
            .filter(String::isNotBlank)
            .joinToString(", ")

    override fun toString() = description
}
