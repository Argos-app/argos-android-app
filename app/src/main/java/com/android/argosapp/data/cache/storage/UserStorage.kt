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
                userEmail = user.userEmail,
                farmName = user.farmName,
            ),
        )
    }

    suspend fun clearUser() {
        dao.clearUser()
    }
}
