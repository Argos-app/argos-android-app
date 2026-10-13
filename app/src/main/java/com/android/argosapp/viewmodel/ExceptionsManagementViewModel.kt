package com.android.argosapp.viewmodel

import androidx.lifecycle.ViewModel
import com.android.argosapp.model.AnimalExceptionRecord

class ExceptionsManagementViewModel : ViewModel() {
    val exceptions = buildList {
        val aptLots = listOf(
            "Sub Engorda 001",
            "Engorda 002",
            "Recria 001",
            "Maternidade 001",
            "Engorda 003",
            "Recria 002",
            "Sub Engorda 002",
            "Engorda 004",
        )

        val aptTags = listOf("075", "102", "118", "124", "131", "145", "163", "177")

        aptTags.forEachIndexed { index, earTag ->
            add(
                AnimalExceptionRecord(
                    earTag = earTag,
                    lotName = aptLots[index],
                    isFitForReturn = true,
                    reason = "Animal com suspeita de febre aftosa.",
                    startDate = "04/06/2026",
                    endDate = "04/08/2026",
                    returnProgress = 60,
                ),
            )
        }

        val unfitLots = listOf(
            "Sub Terminação 001",
            "Engorda 001",
            "Recria 003",
            "Sub Engorda 003",
            "Terminação 002",
            "Engorda 005",
            "Recria 004",
            "Terminação 003",
            "Sub Terminação 002",
            "Engorda 006",
        )
        val unfitTags = listOf("098", "105", "119", "128", "137", "149", "158", "166", "181", "194")
        unfitTags.forEachIndexed { index, earTag ->
            add(
                AnimalExceptionRecord(
                    earTag = earTag,
                    lotName = unfitLots[index],
                    isFitForReturn = false,
                    reason = "Animal com suspeita de febre aftosa.",
                    startDate = "04/06/2026",
                    endDate = "04/08/2026",
                    returnProgress = 60,
                ),
            )
        }
    }

    val fitCount: Int = exceptions.count(AnimalExceptionRecord::isFitForReturn)
    val unfitCount: Int = exceptions.count { !it.isFitForReturn }

    fun filter(filter: ExceptionFilter): List<AnimalExceptionRecord> = when (filter) {
        ExceptionFilter.ALL -> exceptions
        ExceptionFilter.FIT -> exceptions.filter(AnimalExceptionRecord::isFitForReturn)
        ExceptionFilter.UNFIT -> exceptions.filterNot(AnimalExceptionRecord::isFitForReturn)
    }

    enum class ExceptionFilter {
        ALL,
        FIT,
        UNFIT,
    }
}
