package com.android.argosapp.model

import java.time.LocalDate

data class Lot(
    val lotName: String,
    val openDate: LocalDate?,
    val releaseSlaughterDate: LocalDate?,
    val category: String?,
)
