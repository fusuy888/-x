package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NotificationHistory::class], version = 1, exportSchema = false)
abstract class NotificationDatabase : RoomDatabase() {
    abstract val notificationDao: NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
