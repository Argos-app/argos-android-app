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
import com.android.argosapp.data.cache.dao.LotDao
import com.android.argosapp.data.cache.dao.MedicineDao
import com.android.argosapp.data.cache.entity.MedicineEntity
import com.android.argosapp.data.cache.entity.LotEntity
import com.android.argosapp.data.cache.entity.LotCacheMetadata
import com.android.argosapp.data.cache.entity.MedicineCacheMetadata
import com.android.argosapp.data.cache.entity.LotAnimalExceptionEntity

@Database(
    entities = [
        UserEntity::class,
        CachedMonthEntity::class,
        MonthApplicationEntity::class,
        ApplicationEntity::class,
        ApplicationsCacheMetadata::class,
        MedicineEntity::class,
        LotEntity::class,
        LotCacheMetadata::class,
        MedicineCacheMetadata::class,
        LotAnimalExceptionEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class ArgosCacheDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun monthApplicationsDao(): MonthApplicationsDao
    abstract fun applicationsDao(): ApplicationsDao
    abstract fun lotDao(): LotDao
    abstract fun medicineDao(): MedicineDao

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
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
