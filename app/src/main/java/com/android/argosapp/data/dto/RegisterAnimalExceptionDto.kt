package com.android.argosapp.data.dto

import java.math.BigDecimal

data class RegisterAnimalExceptionDto(
    val originLotId: String,
    val earTag: String,
    val exceptionReason: String,
    val currentWeight: BigDecimal,
)
