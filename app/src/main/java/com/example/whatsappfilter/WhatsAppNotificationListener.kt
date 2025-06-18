package com.example.whatsappfilter

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.whatsappfilter.data.AppDatabase
import com.example.whatsappfilter.data.Message
import com.example.whatsappfilter.data.MessageDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class WhatsAppNotificationListener : NotificationListenerService() {
    private lateinit var database: AppDatabase
    private lateinit var messageDao: MessageDao
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    
    // List of important contacts to filter
    private val importantContacts = setOf(
        "Professor",
        "Teacher",
        "Lecturer",
        "Coordinator",
        "Admin",
        "Department",
        "University",
        "College",
        "School",
        "Institute",
        "Faculty",
        "Dean",
        "Head",
        "Director",
        "Manager",
        "Supervisor",
        "Mentor",
        "Advisor",
        "Counselor",
        "Tutor"
    )
    
    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationListener", "Service created")
        try {
            database = AppDatabase.getDatabase(this@WhatsAppNotificationListener)
            messageDao = database.messageDao()
            Log.d("NotificationListener", "Database initialized successfully")
        } catch (e: Exception) {
            Log.e("NotificationListener", "Error initializing database", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val packageName = sbn.packageName
            Log.d("NotificationListener", "Received notification from: $packageName")
            
            if (packageName == "com.whatsapp") {
                val notification = sbn.notification
                val extras = notification.extras
                
                val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
                val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
                
                Log.d("NotificationListener", "Processing message - Title: $title, Text: $text")
                
                if (isRelevantMessage(text) || isImportantContact(title)) {
                    Log.d("NotificationListener", "Message is relevant or from important contact, saving to database")
                    scope.launch {
                        try {
                            saveMessage(title, text)
                        } catch (e: Exception) {
                            Log.e("NotificationListener", "Error saving message", e)
                        }
                    }
                } else {
                    Log.d("NotificationListener", "Message is not relevant and not from important contact, skipping")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationListener", "Error processing notification", e)
        }
    }

    private fun isRelevantMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Study-related keywords
        val studyKeywords = listOf(
            "assignment", "homework", "project", "exam", "test", "quiz",
            "deadline", "due date", "submission", "presentation", "report",
            "research", "study", "lecture", "class", "course", "seminar",
            "workshop", "lab", "practical", "tutorial", "hackathon",
            "competition", "internship", "scholarship", "fellowship", "grant"
        )
        
        // Urgent keywords
        val urgentKeywords = listOf(
            "urgent", "important", "emergency", "immediate", "asap",
            "deadline", "due", "last date", "final", "critical"
        )
        
        // Check if message contains any study-related keywords
        val hasStudyKeyword = studyKeywords.any { it in lowerMessage }
        val hasUrgentKeyword = urgentKeywords.any { it in lowerMessage }
        
        Log.d("NotificationListener", "Message check - Study: $hasStudyKeyword, Urgent: $hasUrgentKeyword")
        
        return hasStudyKeyword || hasUrgentKeyword
    }

    private fun isImportantContact(contactName: String): Boolean {
        val isImportant = importantContacts.any { contactName.contains(it, ignoreCase = true) }
        Log.d("NotificationListener", "Contact check - $contactName is ${if (isImportant) "important" else "not important"}")
        return isImportant
    }

    private suspend fun saveMessage(sender: String, content: String) {
        try {
            val message = Message(
                id = UUID.randomUUID().toString(),
                content = content,
                sender = sender,
                timestamp = Date(),
                isRead = false
            )
            
            messageDao.insert(message)
            Log.d("WhatsAppNotificationListener", "Saved message: $content")
        } catch (e: Exception) {
            Log.e("WhatsAppNotificationListener", "Error saving message", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        database.close()
        Log.d("NotificationListener", "Service destroyed")
    }
} 