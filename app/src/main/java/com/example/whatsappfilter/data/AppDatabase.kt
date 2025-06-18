package com.example.whatsappfilter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.*

@Database(
    entities = [Message::class, Reminder::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add missing columns if they don't exist
                database.execSQL("ALTER TABLE messages ADD COLUMN IF NOT EXISTS isRead INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE messages ADD COLUMN IF NOT EXISTS isAccepted INTEGER NOT NULL DEFAULT 0")
                
                // Create reminders table if it doesn't exist
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id TEXT PRIMARY KEY NOT NULL,
                        messageId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        reminderTime INTEGER NOT NULL,
                        priority INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (messageId) REFERENCES messages(id) ON DELETE CASCADE
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whatsapp_filter_db"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}