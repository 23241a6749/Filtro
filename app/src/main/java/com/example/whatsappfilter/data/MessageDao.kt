package com.example.whatsappfilter.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getMessagesBetweenDates(startDate: Date, endDate: Date): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>)

    @Query("UPDATE messages SET isRead = :isRead WHERE id = :messageId")
    suspend fun markAsRead(messageId: String, isRead: Boolean)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    @Query("UPDATE messages SET isAccepted = :accepted WHERE id IN (:messageIds)")
    suspend fun updateMessagesAcceptance(messageIds: List<Long>, accepted: Boolean)

    @Query("SELECT * FROM messages WHERE isAccepted = 1 ORDER BY timestamp DESC")
    fun getAcceptedMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE isAccepted = 0 ORDER BY timestamp DESC")
    fun getPendingMessages(): Flow<List<Message>>
}