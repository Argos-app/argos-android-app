package com.android.argosapp.data.repository

import com.android.argosapp.model.StockProduct

internal data class MedicineCatalogPage(
    val products: List<StockProduct>,
    val totalPages: Int,
)

internal fun mapMedicineCatalogItem(
    medicineId: String,
    medicineName: String,
    productRegistration: String,
): StockProduct = StockProduct(
    id = medicineId,
    name = medicineName,
    registrationCode = productRegistration,
)

internal suspend fun fetchAllMedicineCatalogProducts(
    fetchPage: suspend (page: Int, size: Int) -> MedicineCatalogPage,
): List<StockProduct> {
    val products = mutableListOf<StockProduct>()
    var page = 0
    var totalPages = 1

    while (page < totalPages) {
        val result = fetchPage(page, MEDICINE_CATALOG_PAGE_SIZE)
        products += result.products
        totalPages = result.totalPages
        page++
    }

    return products
}

internal const val MEDICINE_CATALOG_PAGE_SIZE = 20
