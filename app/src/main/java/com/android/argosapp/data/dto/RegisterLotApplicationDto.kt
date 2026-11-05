package com.android.argosapp.data.dto

import java.math.BigDecimal

data class RegisterLotApplicationDto(
    val medicineBatchId: String,
    val lotId: String,
    val lotName: String,
    val medicineName: String,
    val applicationDate: String,
    val dose: BigDecimal,
    val goal: String,
    val unit: String = "ml",
    val route: String = "ORAL",
)
