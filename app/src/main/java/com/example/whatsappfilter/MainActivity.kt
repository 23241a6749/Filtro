package com.example.whatsappfilter

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.window.Dialog
import com.example.whatsappfilter.data.Message
import com.example.whatsappfilter.data.Reminder
import com.example.whatsappfilter.data.AppDatabase
import com.example.whatsappfilter.ui.theme.WhatsAppFilterTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = MainViewModel(applicationContext as Application)
        
        setContent {
            WhatsAppFilterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isNotificationServiceEnabled()) {
                        NotificationAccessRequired()
                    } else {
                        MainScreen(viewModel)
                    }
                }
            }
        }

        // Call this in onCreate if needed
        // clearDatabase()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ImportActivity.PICK_FILE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                // Refresh messages after successful import
                viewModel.refreshMessages()
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }

    private fun requestNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun clearDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.getDatabase(this@MainActivity).clearAllTables()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error clearing database", e)
            }
        }
    }
}

@Composable
fun NotificationAccessRequired() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Notification Access Required",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please enable notification access to filter WhatsApp messages",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { 
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }) {
            Text("Enable Access")
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Messages") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Reminders") }
            )
        }
        
        when (selectedTab) {
            0 -> MessagesScreen(viewModel)
            1 -> RemindersScreen(viewModel)
        }
    }
}

@Composable
fun MessagesScreen(viewModel: MainViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var selectedDateField by remember { mutableStateOf<Int?>(null) }
    var dateInput by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current

    // Collect messages and error state
    val messages by viewModel.messages.collectAsState()
    val totalMessages by viewModel.totalMessageCount.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Show error if any
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Message Count and Menu Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Messages: $totalMessages",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtered: ${messages.size}",
                    style = MaterialTheme.typography.titleMedium
                )
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import Chats") },
                            onClick = {
                                showMenu = false
                                val intent = Intent(context, ImportActivity::class.java)
                                context.startActivity(intent)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = "Import Chats"
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear All Chats") },
                            onClick = {
                                showMenu = false
                                showClearConfirmation = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear All Chats",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Chats") },
                            onClick = {
                                showMenu = false
                                val intent = Intent(context, ExportActivity::class.java)
                                context.startActivity(intent)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Export Chats"
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        )
                    }
                }
            }
        }
        
        // Date Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter by Date:",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        selectedDateField = 0
                        dateInput = TextFieldValue(startDate?.let { dateFormatter.format(it) } ?: "")
                        showDatePicker = true
                    }
                ) {
                    Text(
                        text = startDate?.let { dateFormatter.format(it) } ?: "Start Date"
                    )
                }
                
                Text("to")
                
                OutlinedButton(
                    onClick = {
                        selectedDateField = 1
                        dateInput = TextFieldValue(endDate?.let { dateFormatter.format(it) } ?: "")
                        showDatePicker = true
                    }
                ) {
                    Text(
                        text = endDate?.let { dateFormatter.format(it) } ?: "End Date"
                    )
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Select Date") },
                text = {
                    Column {
                        TextField(
                            value = dateInput,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 10) {
                                    dateInput = newValue
                                }
                            },
                            label = { Text("Enter date (dd/MM/yyyy)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = DateTransformation()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            try {
                                val parsedDate = dateFormatter.parse(dateInput.text)
                                when (selectedDateField) {
                                    0 -> startDate = parsedDate
                                    1 -> endDate = parsedDate
                                }
                                viewModel.updateDateFilter(startDate, endDate)
                            } catch (e: Exception) {
                                // Handle invalid date format
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        var showFilters by remember { mutableStateOf(false) }
        
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Student Message Filter",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            }
            
            if (showFilters) {
                StudentFilterOptions(viewModel)
            }
            
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages found")
                }
            } else {
                MessagesList(messages, viewModel, startDate, endDate)
            }
        }

        // Clear Chat Confirmation Dialog
        if (showClearConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation = false },
                title = { Text("Clear Chat") },
                text = { Text("Are you sure you want to clear all messages? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllMessages()
                            showClearConfirmation = false
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearConfirmation = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RemindersScreen(viewModel: MainViewModel) {
    val reminders by viewModel.activeReminders.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reminders) { reminder ->
                ReminderItem(reminder, viewModel)
            }
        }
    }
}

@Composable
fun ReminderItem(reminder: Reminder, viewModel: MainViewModel) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (reminder.priority) {
                Reminder.Priority.HIGH -> MaterialTheme.colorScheme.errorContainer
                Reminder.Priority.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                Reminder.Priority.LOW -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reminder.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Due: ${dateFormat.format(reminder.reminderTime)}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { viewModel.markReminderCompleted(reminder) }) {
                    Icon(Icons.Default.Check, contentDescription = "Mark Complete")
                }
                IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun StudentFilterOptions(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Filter Options",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getFilterTypes()) { (filterType, displayName) ->
                FilterChipItem(filterType, displayName, viewModel)
            }
        }
    }
}

private fun getFilterTypes() = listOf(
    "isAssignment" to "Assignments",
    "isExam" to "Exams",
    "isSchedule" to "Class Schedule",
    "isImportantDate" to "Important Dates",
    "isStudyMaterial" to "Study Material",
    "isGroupProject" to "Group Projects",
    "isAnnouncement" to "Announcements",
    "isDeadline" to "Deadlines",
    "isGrade" to "Grades",
    "isCampusEvent" to "Campus Events"
)

@Composable
private fun FilterChipItem(
    filterType: String,
    displayName: String,
    viewModel: MainViewModel
) {
    var isSelected by remember { mutableStateOf(false) }
    
    FilterChip(
        selected = isSelected,
        onClick = {
            isSelected = !isSelected
            viewModel.updateFilter(filterType, isSelected)
        },
        label = { Text(displayName) }
    )
}

@Composable
fun MessagesList(
    messages: List<Message>,
    viewModel: MainViewModel,
    startDate: Date?,
    endDate: Date?
) {
    var showReminderDialog by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        items(messages) { message ->
            // Filter messages by date range
            if ((startDate == null || message.timestamp >= startDate) &&
                (endDate == null || message.timestamp <= endDate)) {
                MessageItem(
                    message = message,
                    onDelete = { viewModel.deleteMessage(message) },
                    onMarkAsRead = { viewModel.markMessageAsRead(message) },
                    onSetReminder = {
                        selectedMessage = message
                        showReminderDialog = true
                    }
                )
            }
        }
    }
    
    if (showReminderDialog && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Set Reminder") },
            text = { Text("Set a reminder for this message?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedMessage?.let { message ->
                            viewModel.createReminder(message, Date())
                        }
                        showReminderDialog = false
                    }
                ) {
                    Text("Set Reminder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageItem(
    message: Message,
    onDelete: () -> Unit,
    onMarkAsRead: () -> Unit,
    onSetReminder: () -> Unit
) {
    var showImage by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        .format(message.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (message.imagePath != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(message.imagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Message image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RectangleShape)
                        .clickable { showImage = true },
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onMarkAsRead) {
                    Icon(
                        imageVector = if (message.isRead) Icons.Default.Done else Icons.Default.DoneAll,
                        contentDescription = "Mark as read"
                    )
                }
                IconButton(onClick = onSetReminder) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Set reminder"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete message"
                    )
                }
            }
        }
    }
    
    if (showImage && message.imagePath != null) {
        Dialog(onDismissRequest = { showImage = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.imagePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full size image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { showImage = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Add this class for date input transformation
class DateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 10) text.text.substring(0..9) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) out += "/"
        }
        return TransformedText(AnnotatedString(out), DateOffsetMapping)
    }
}

// Add this class for cursor position mapping
object DateOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return when {
            offset <= 2 -> offset
            offset <= 4 -> offset + 1
            else -> offset + 2
        }
    }

    override fun transformedToOriginal(offset: Int): Int {
        return when {
            offset <= 2 -> offset
            offset <= 5 -> offset - 1
            else -> offset - 2
        }
    }
}