package com.android.argosapp.data.dto

data class MedicineCatalogPageDto(
    val data: List<MedicineCatalogDto> = emptyList(),
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
)

data class MedicineCatalogDto(
    val productRegistration: String?,
    val medicineName: String?,
    val activeIngredient: String? = null,
    val therapeuticCategory: String? = null,
)
