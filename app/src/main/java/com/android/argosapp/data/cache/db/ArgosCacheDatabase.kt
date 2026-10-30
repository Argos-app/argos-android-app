package com.android.argosapp.data.cache.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.execSQL
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
import com.android.argosapp.data.cache.dao.AnimalExceptionDao
import com.android.argosapp.data.cache.entity.AnimalExceptionEntity
import com.android.argosapp.data.cache.entity.AnimalExceptionCacheMetadata
import com.android.argosapp.data.cache.dao.NotificationDao
import com.android.argosapp.data.cache.entity.NotificationEntity
import com.android.argosapp.data.cache.dao.DeviceDao
import com.android.argosapp.data.cache.entity.DeviceEntity

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
        AnimalExceptionEntity::class,
        AnimalExceptionCacheMetadata::class,
        NotificationEntity::class,
        DeviceEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ArgosCacheDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun monthApplicationsDao(): MonthApplicationsDao
    abstract fun applicationsDao(): ApplicationsDao
    abstract fun lotDao(): LotDao
    abstract fun medicineDao(): MedicineDao
    abstract fun animalExceptionDao(): AnimalExceptionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun deviceDao(): DeviceDao

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
