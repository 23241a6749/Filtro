package com.example.whatsappfilter.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["reminderTime"]),
        Index(value = ["isCompleted"]),
        Index(value = ["priority"]),
        Index(value = ["messageId"])
    ]
)
data class Reminder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val messageId: String,
    val title: String,
    val description: String,
    val reminderTime: Date,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false
) {
    enum class Priority {
        LOW, MEDIUM, HIGH
    }
} 