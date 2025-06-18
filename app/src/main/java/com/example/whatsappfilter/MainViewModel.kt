package com.example.whatsappfilter

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappfilter.data.AppDatabase
import com.example.whatsappfilter.data.Message
import com.example.whatsappfilter.data.Reminder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val messageDao = database.messageDao()
    private val reminderDao = database.reminderDao()
    private val activeFilters = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _totalMessageCount = MutableStateFlow(0)
    val totalMessageCount: StateFlow<Int> = _totalMessageCount.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var currentStartDate: Date? = null
    private var currentEndDate: Date? = null
    
    init {
        viewModelScope.launch {
            try {
                // Get total message count
                _totalMessageCount.value = messageDao.getMessageCount()
                
                messageDao.getAllMessages().collect { messages ->
                    _messages.value = messages
                    Log.d("MainViewModel", "Messages updated: ${messages.size} messages")
                }
            } catch (e: Exception) {
                _error.value = "Error loading messages: ${e.message}"
                Log.e("MainViewModel", "Error loading messages", e)
            }
        }
    }
    
    val activeReminders: StateFlow<List<Reminder>> = reminderDao
        .getActiveReminders()
        .catch { e -> _error.value = "Error loading reminders: ${e.message}" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun updateFilter(filterType: String, isChecked: Boolean) {
        viewModelScope.launch {
            try {
                activeFilters.update { currentFilters ->
                    currentFilters.toMutableMap().apply {
                        if (isChecked) {
                            put(filterType, true)
                        } else {
                            remove(filterType)
                        }
                    }
                }
                // Refresh messages after filter update
                refreshMessages()
            } catch (e: Exception) {
                _error.value = "Error updating filter: ${e.message}"
                Log.e("MainViewModel", "Error updating filter", e)
            }
        }
    }
    
    fun clearAllMessages() {
        viewModelScope.launch {
            try {
                messageDao.deleteAll()
                _error.value = "All messages cleared successfully"
                refreshMessages()
            } catch (e: Exception) {
                _error.value = "Error clearing messages: ${e.message}"
            }
        }
    }

    fun createReminder(message: Message, reminderTime: Date) {
        viewModelScope.launch {
            try {
                val reminder = Reminder(
                    messageId = message.id,
                    title = "Reminder: ${message.sender}",
                    description = message.content,
                    reminderTime = reminderTime,
                    priority = Reminder.Priority.MEDIUM
                )
                database.reminderDao().insertReminder(reminder)
            } catch (e: Exception) {
                _error.value = "Error creating reminder: ${e.message}"
            }
        }
    }

    fun markReminderCompleted(reminder: Reminder) {
        viewModelScope.launch {
            try {
                database.reminderDao().updateReminder(
                    reminder.copy(isCompleted = true)
                )
            } catch (e: Exception) {
                _error.value = "Error marking reminder complete: ${e.message}"
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                database.reminderDao().deleteReminder(reminder)
            } catch (e: Exception) {
                _error.value = "Error deleting reminder: ${e.message}"
            }
        }
    }

    fun updateDateFilter(startDate: Date?, endDate: Date?) {
        currentStartDate = startDate
        currentEndDate = endDate
        viewModelScope.launch {
            try {
                if (startDate != null && endDate != null) {
                    messageDao.getMessagesBetweenDates(startDate, endDate).collect { messages ->
                        _messages.value = messages
                        Log.d("MainViewModel", "Filtered messages: ${messages.size} messages")
                    }
                } else {
                    messageDao.getAllMessages().collect { messages ->
                        _messages.value = messages
                        Log.d("MainViewModel", "Showing all messages: ${messages.size} messages")
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error filtering messages: ${e.message}"
                Log.e("MainViewModel", "Error filtering messages", e)
            }
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            try {
                // Since we removed single message deletion, we'll skip this operation
                _error.value = "Single message deletion is no longer supported"
            } catch (e: Exception) {
                _error.value = "Error deleting message: ${e.message}"
            }
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            try {
                val count = messageDao.getMessageCount()
                Log.d("MainViewModel", "Total messages in database: $count")
                
                messageDao.getAllMessages().collect { messages ->
                    val filteredMessages = if (activeFilters.value.isEmpty()) {
                        messages
                    } else {
                        messages.filter { message ->
                            activeFilters.value.any { (filterType, isActive) ->
                                if (!isActive) return@any false
                                when (filterType) {
                                    "isAssignment" -> message.content.contains(Regex(
                                        "(?i)(assignment|homework|project|task|exercise|problem set|lab work|practical|worksheet|coursework)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isExam" -> message.content.contains(Regex(
                                        "(?i)(exam|test|quiz|midterm|final|assessment|evaluation|viva|oral exam|written exam|practical exam|mock test)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isSchedule" -> message.content.contains(Regex(
                                        "(?i)(schedule|class|lecture|seminar|workshop|lab|practical|tutorial|course|meeting|appointment)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isImportantDate" -> message.content.contains(Regex(
                                        "(?i)(important|urgent|emergency|immediate|asap|deadline|due|last date|final|critical)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isStudyMaterial" -> message.content.contains(Regex(
                                        "(?i)(notes|material|resources|study|textbook|reference|handout|slides|presentation|lecture notes|study guide)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isGroupProject" -> message.content.contains(Regex(
                                        "(?i)(group project|team project|collaboration|teamwork|group work|team work)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isAnnouncement" -> message.content.contains(Regex(
                                        "(?i)(announcement|notice|notification|update|information|news|circular|bulletin)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isDeadline" -> message.content.contains(Regex(
                                        "(?i)(deadline|due date|submission|last date|cut-off|closing date|final date)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isGrade" -> message.content.contains(Regex(
                                        "(?i)(grade|marks|result|score|percentage|cgpa|gpa|ranking|position|merit|scholarship|award)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    "isCampusEvent" -> message.content.contains(Regex(
                                        "(?i)(event|festival|competition|hackathon|workshop|seminar|conference|symposium|exhibition|fair|show|concert|performance)",
                                        RegexOption.IGNORE_CASE
                                    ))
                                    else -> false
                                }
                            }
                        }
                    }
                    _messages.value = filteredMessages
                    Log.d("MainViewModel", "Filtered messages: ${filteredMessages.size} messages")
                }
            } catch (e: Exception) {
                _error.value = "Error refreshing messages: ${e.message}"
                Log.e("MainViewModel", "Error refreshing messages", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun markMessageAsRead(message: Message) {
        viewModelScope.launch {
            try {
                messageDao.markAsRead(message.id, true)
                refreshMessages()
            } catch (e: Exception) {
                _error.value = "Failed to mark message as read: ${e.message}"
            }
        }
    }
} 