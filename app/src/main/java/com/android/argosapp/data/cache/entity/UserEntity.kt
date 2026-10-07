package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.android.argosapp.model.User

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int = ACTIVE_USER_ID,
    val userName: String,
    val userEmail: String,
    val farmName: String,
) {
    fun toModel() = User(userName = userName, userEmail = userEmail, farmName = farmName)

    companion object {
        const val ACTIVE_USER_ID = 1
    }
}