package com.android.argosapp.data.dto

import java.math.BigDecimal

data class RegisterLotApplicationDto(
    val lotName: String,
    val medicineName: String,
    val applicationDate: String,
    val dose: BigDecimal,
    val unit: String = "mL",
    val earCode: String? = null,
)