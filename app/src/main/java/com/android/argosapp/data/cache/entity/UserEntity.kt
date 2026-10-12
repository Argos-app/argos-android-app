package com.android.argosapp.data.cache.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.android.argosapp.model.User
import java.util.UUID

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int = ACTIVE_USER_ID,
    val userName: String,
    val email: String,
    val farmName: String,
    val userId: String,
    val photoUrl: String?,
    val cpf: String?,
    val address: String?,
) {
    fun toModel() = User(
        userName = userName,
        email = email,
        farmName = farmName,
        userId = UUID.fromString(userId),
        photoUrl = photoUrl,
        cpf = cpf,
        address = address,
    )

    companion object {
        const val ACTIVE_USER_ID = 1
    }
}
