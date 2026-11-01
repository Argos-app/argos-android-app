package com.android.argosapp.data.cache.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnTypeConverters
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.android.argosapp.data.cache.dao.ChatCacheDao
import com.android.argosapp.data.cache.entity.ChatMessageEntity
import com.android.argosapp.data.cache.entity.ChatMessagePageEntity
import com.android.argosapp.data.cache.entity.ChatSessionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ChatCacheConverters {
    @ColumnTypeConverter
    fun listToJson(value: List<String>): String = Gson().toJson(value)

    @ColumnTypeConverter
    fun jsonToList(value: String): List<String> = Gson().fromJson(value, object : TypeToken<List<String>>() {}.type)
}

@Database(entities = [ChatSessionEntity::class, ChatMessageEntity::class, ChatMessagePageEntity::class], version = 1, exportSchema = false)
@ColumnTypeConverters(ChatCacheConverters::class)
abstract class ChatCacheDatabase : RoomDatabase() {
    abstract fun chatCacheDao(): ChatCacheDao

    companion object {
        @Volatile private var instance: ChatCacheDatabase? = null

        fun getInstance(context: Context): ChatCacheDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder<ChatCacheDatabase>(context.applicationContext, "chat-cache.db")
                .setDriver(AndroidSQLiteDriver())
                .build()
                .also { instance = it }
        }
    }
}
