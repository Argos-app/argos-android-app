package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "lot_animal_exceptions")
data class LotAnimalExceptionEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val lotRowId: Long,
    val earCode: String?,
    val medicine: String?,
    val releaseSlaughterDate: String?,
)
