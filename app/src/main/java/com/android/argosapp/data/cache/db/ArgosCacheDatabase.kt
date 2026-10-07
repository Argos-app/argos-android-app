package com.android.argosapp.data.cache.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.android.argosapp.data.cache.entity.CachedMonthEntity
import com.android.argosapp.data.cache.entity.MonthApplicationEntity
import com.android.argosapp.data.cache.dao.MonthApplicationsDao
import com.android.argosapp.data.cache.dao.UserDao
import com.android.argosapp.data.cache.entity.UserEntity
import com.android.argosapp.data.cache.entity.ApplicationEntity
import com.android.argosapp.data.cache.entity.ApplicationsCacheMetadata
import com.android.argosapp.data.cache.dao.ApplicationsDao

@Database(
    entities = [
        UserEntity::class,
        CachedMonthEntity::class,
        MonthApplicationEntity::class,
        ApplicationEntity::class,
        ApplicationsCacheMetadata::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ArgosCacheDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun monthApplicationsDao(): MonthApplicationsDao
    abstract fun applicationsDao(): ApplicationsDao

    companion object {
        private const val DATABASE_NAME = "argos-cache.db"

        @Volatile
        private var instance: ArgosCacheDatabase? = null

        fun getInstance(context: Context): ArgosCacheDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder<ArgosCacheDatabase>(
                context.applicationContext,
                DATABASE_NAME,
            )
                .setDriver(AndroidSQLiteDriver())
                .build()
                .also { instance = it }
        }
    }
}
