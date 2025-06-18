package com.example.whatsappfilter.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY reminderTime ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)

    @Query("SELECT * FROM reminders WHERE reminderTime BETWEEN :startTime AND :endTime")
    fun getRemindersInTimeRange(startTime: Date, endTime: Date): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE messageId = :messageId")
    suspend fun getReminderForMessage(messageId: String): Reminder?
} 