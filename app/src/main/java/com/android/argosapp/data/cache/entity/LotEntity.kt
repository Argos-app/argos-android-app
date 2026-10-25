package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "lots")
data class LotEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val lotName: String,
    val openDate: String?,
    val releaseSlaughterDate: String?,
    val category: String?,
    val animalExceptionOutListIsNull: Boolean,
)
