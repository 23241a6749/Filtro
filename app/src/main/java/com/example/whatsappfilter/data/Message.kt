package com.example.whatsappfilter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val content: String,
    val sender: String,
    val timestamp: Date,
    val hasMedia: Boolean = false,
    val hasLink: Boolean = false,
    val hasDocument: Boolean = false,
    val hasLocation: Boolean = false,
    val hasVoiceNote: Boolean = false,
    val isDeleted: Boolean = false,
    val isForwarded: Boolean = false,
    val isReply: Boolean = false,
    val isSystemMessage: Boolean = false,
    val isRead: Boolean = false,
    val isAccepted: Boolean = false,
    @ColumnInfo(name = "image_path")
    val imagePath: String? = null
)