package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SyncMessageEntity::class], version = 1, exportSchema = false)
abstract class SyncDatabase : RoomDatabase() {
    abstract fun syncMessageDao(): SyncMessageDao

    companion object {
        @Volatile
        private var INSTANCE: SyncDatabase? = null

        fun getDatabase(context: Context): SyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SyncDatabase::class.java,
                    "sync_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
