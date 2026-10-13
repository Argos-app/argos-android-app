package com.android.argosapp.model

data class AnimalExceptionRecord(
    val earTag: String,
    val lotName: String,
    val isFitForReturn: Boolean,
    val reason: String,
    val startDate: String,
    val endDate: String,
    val returnProgress: Int,
)
