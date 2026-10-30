package com.android.argosapp.data.cache.storage

import android.content.Context
import com.android.argosapp.data.cache.db.ArgosCacheDatabase
import com.android.argosapp.data.cache.entity.UserEntity
import com.android.argosapp.model.User

class UserStorage(context: Context) {
    private val dao = ArgosCacheDatabase.getInstance(context).userDao()

    suspend fun getUser(): User? = dao.getUser(UserEntity.ACTIVE_USER_ID)?.toModel()

    suspend fun saveUser(user: User) {
        dao.saveUser(
            UserEntity(
                userName = user.userName,
                email = user.email,
                farmName = user.farmName,
                userId = user.userId.toString(),
                photoUrl = user.photoUrl,
                cpf = user.cpf,
                address = user.address,
                role = user.role
            ),
        )
    }

    suspend fun clearUser() {
        dao.clearUser()
    }
}
