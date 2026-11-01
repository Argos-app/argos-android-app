package com.android.argosapp.data.dto

import java.math.BigDecimal

data class RegisterAnimalApplicationDto(
    val earCode: String,
    val medicineBatchId: String,
    val dose: BigDecimal,
    val unit: String = "ml",
    val route: String = "ORAL",
    val goal: String? = null,
)
