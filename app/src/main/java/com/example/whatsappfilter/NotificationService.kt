package com.example.whatsappfilter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.whatsappfilter.data.AppDatabase
import com.example.whatsappfilter.data.Message
import com.example.whatsappfilter.data.MessageDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

class NotificationService : NotificationListenerService() {

    private val WHATSAPP_PACKAGE_NAME = "com.whatsapp"
    private val CHANNEL_ID = "WhatsAppFilterChannel"
    private val NOTIFICATION_ID = 1
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private lateinit var messageDao: MessageDao

    // Academic Keywords
    private val academicKeywords = listOf(
        // Assignments & Projects
        "assignment", "homework", "project", "task", "exercise", "problem set",
        "lab work", "practical", "worksheet", "coursework", "thesis", "dissertation",
        
        // Exams & Tests
        "exam", "test", "quiz", "midterm", "final", "assessment", "evaluation",
        "viva", "oral exam", "written exam", "practical exam", "mock test",
        
        // Study Materials
        "notes", "material", "resources", "study", "textbook", "reference",
        "handout", "slides", "presentation", "lecture notes", "study guide",
        
        // Grades & Results
        "grade", "marks", "result", "score", "percentage", "cgpa", "gpa",
        "ranking", "position", "merit", "scholarship", "award"
    )

    // Schedule & Time Management
    private val scheduleKeywords = listOf(
        "schedule", "timetable", "class", "lecture", "lab", "tutorial",
        "seminar", "workshop", "conference", "meeting", "appointment",
        "deadline", "due date", "submission", "last date", "cutoff",
        "holiday", "break", "vacation", "recess", "term break"
    )

    // Collaboration & Group Work
    private val collaborationKeywords = listOf(
        "group", "team", "collaboration", "partner", "member",
        "discussion", "meeting", "brainstorming", "planning",
        "presentation", "demo", "pitch", "defense", "review"
    )

    // Events & Activities
    private val eventKeywords = listOf(
        "event", "workshop", "seminar", "conference", "symposium",
        "hackathon", "competition", "contest", "festival", "fair",
        "exhibition", "showcase", "demo day", "career fair", "job fair"
    )

    // Career & Opportunities
    private val careerKeywords = listOf(
        "internship", "job", "career", "placement", "recruitment",
        "interview", "resume", "cv", "application", "selection",
        "training", "orientation", "induction", "workshop", "skill"
    )

    // Urgent & Important
    private val urgentKeywords = listOf(
        "urgent", "important", "asap", "emergency", "deadline",
        "meeting", "reminder", "alert", "notice", "immediate",
        "priority", "critical", "must", "required", "compulsory",
        "mandatory", "essential", "necessary", "vital", "crucial"
    )

    // Research & Development
    private val researchKeywords = listOf(
        "research", "paper", "thesis", "publication", "journal",
        "conference", "presentation", "defense", "proposal",
        "methodology", "experiment", "survey", "analysis", "findings"
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        database = AppDatabase.getDatabase(this)
        messageDao = AppDatabase.getDatabase(this).messageDao()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == WHATSAPP_PACKAGE_NAME) {
            val notification = sbn.notification
            val extras = notification.extras
            
            val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
            val content = extras.getString(Notification.EXTRA_TEXT) ?: return
            
            // Check if message contains important keywords
            val isImportant = content.contains(Regex(
                "(?i)(assignment|exam|schedule|deadline|important|urgent|due|submit|test|quiz|project|homework|class|lecture|meeting|appointment|reminder|note|todo|task)",
                RegexOption.IGNORE_CASE
            ))
            
            if (isImportant) {
                serviceScope.launch {
                    saveImportantMessage(
                        sender = sender,
                        content = content,
                        timestamp = Date(sbn.postTime)
                    )
                }
            }
        }
    }

    private suspend fun saveImportantMessage(
        sender: String,
        content: String,
        timestamp: Date,
        hasMedia: Boolean = false,
        hasLink: Boolean = false,
        hasDocument: Boolean = false,
        hasLocation: Boolean = false,
        hasVoiceNote: Boolean = false,
        isDeleted: Boolean = false,
        isForwarded: Boolean = false,
        isReply: Boolean = false,
        isSystemMessage: Boolean = false
    ) {
        try {
            val message = Message(
                id = UUID.randomUUID().toString(),
                content = content,
                sender = sender,
                timestamp = timestamp,
                hasMedia = hasMedia,
                hasLink = hasLink,
                hasDocument = hasDocument,
                hasLocation = hasLocation,
                hasVoiceNote = hasVoiceNote,
                isDeleted = isDeleted,
                isForwarded = isForwarded,
                isReply = isReply,
                isSystemMessage = isSystemMessage,
                isRead = false,
                isAccepted = false
            )
            
            messageDao.insert(message)
            Log.d("NotificationService", "Saved important message: $content")
        } catch (e: Exception) {
            Log.e("NotificationService", "Error saving message", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "WhatsApp Filter"
            val descriptionText = "Channel for filtered WhatsApp messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}