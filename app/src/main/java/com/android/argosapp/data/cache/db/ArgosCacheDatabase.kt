package com.android.argosapp.data.cache.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.android.argosapp.data.cache.dao.ChatSessionDao
import com.android.argosapp.data.cache.entity.CachedMonthEntity
import com.android.argosapp.data.cache.entity.ChatMessageEntity
import com.android.argosapp.data.cache.entity.ChatSessionEntity
import com.android.argosapp.data.cache.entity.MonthApplicationEntity
import com.android.argosapp.data.cache.dao.MonthApplicationsDao
import com.android.argosapp.data.cache.dao.UserDao
import com.android.argosapp.data.cache.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CachedMonthEntity::class,
        MonthApplicationEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ArgosCacheDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun monthApplicationsDao(): MonthApplicationsDao
    abstract fun chatSessionDao(): ChatSessionDao

    companion object {
        private const val DATABASE_NAME = "argos-cache.db"

        private val MIGRATION_2_3 = Migration(2, 3) { connection ->
            connection.execute(
                """CREATE TABLE IF NOT EXISTS chat_sessions (
                    id TEXT NOT NULL,
                    userKey TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )""".trimIndent(),
            )
            connection.execute(
                """CREATE TABLE IF NOT EXISTS chat_messages (
                    id TEXT NOT NULL,
                    sessionId TEXT NOT NULL,
                    userKey TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    sender TEXT NOT NULL,
                    text TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    optionsJson TEXT NOT NULL,
                    responseState TEXT,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent(),
            )
            connection.execute(
                "CREATE INDEX IF NOT EXISTS index_chat_sessions_userKey_updatedAt ON chat_sessions(userKey, updatedAt)",
            )
            connection.execute(
                "CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId_position ON chat_messages(sessionId, position)",
            )
            connection.execute(
                "CREATE INDEX IF NOT EXISTS index_chat_messages_userKey_sessionId ON chat_messages(userKey, sessionId)",
            )
        }

        private suspend fun SQLiteConnection.execute(sql: String) {
            prepare(sql).use { statement -> statement.step() }
        }

        @Volatile
        private var instance: ArgosCacheDatabase? = null

        fun getInstance(context: Context): ArgosCacheDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder<ArgosCacheDatabase>(
                context.applicationContext,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_2_3)
                .setDriver(AndroidSQLiteDriver())
                .build()
                .also { instance = it }
        }
    }
}
