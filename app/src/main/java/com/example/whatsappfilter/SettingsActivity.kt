package com.example.whatsappfilter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.whatsappfilter.ui.theme.WhatsAppFilterTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.whatsappfilter.data.AppDatabase

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
private val AUTO_FILTER_KEY = booleanPreferencesKey("auto_filter")
private val NOTIFICATION_KEY = booleanPreferencesKey("notifications")
private val BACKUP_KEY = booleanPreferencesKey("auto_backup")

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WhatsAppFilterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDarkTheme by remember { mutableStateOf(false) }
    var autoFilter by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(false) }
    var autoBackup by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    // Load saved preferences
    LaunchedEffect(Unit) {
        isDarkTheme = context.dataStore.data.first()[DARK_THEME_KEY] ?: false
        autoFilter = context.dataStore.data.first()[AUTO_FILTER_KEY] ?: false
        notifications = context.dataStore.data.first()[NOTIFICATION_KEY] ?: true
        autoBackup = context.dataStore.data.first()[BACKUP_KEY] ?: false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Appearance Settings
        SettingsCategory(title = "Appearance") {
            SettingsSwitch(
                title = "Dark Theme",
                description = "Enable dark mode for the app",
                checked = isDarkTheme,
                onCheckedChange = { newValue ->
                    isDarkTheme = newValue
                    scope.launch {
                        context.dataStore.edit { preferences ->
                            preferences[DARK_THEME_KEY] = newValue
                        }
                    }
                }
            )
        }
        
        // Filter Settings
        SettingsCategory(title = "Filter Settings") {
            SettingsSwitch(
                title = "Auto Filter",
                description = "Automatically filter incoming messages",
                checked = autoFilter,
                onCheckedChange = { newValue ->
                    autoFilter = newValue
                    scope.launch {
                        context.dataStore.edit { preferences ->
                            preferences[AUTO_FILTER_KEY] = newValue
                        }
                    }
                }
            )
        }
        
        // Notification Settings
        SettingsCategory(title = "Notifications") {
            SettingsSwitch(
                title = "Enable Notifications",
                description = "Receive notifications for filtered messages",
                checked = notifications,
                onCheckedChange = { newValue ->
                    notifications = newValue
                    scope.launch {
                        context.dataStore.edit { preferences ->
                            preferences[NOTIFICATION_KEY] = newValue
                        }
                    }
                }
            )
            
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Notification Settings")
            }
        }
        
        // Backup Settings
        SettingsCategory(title = "Backup") {
            SettingsSwitch(
                title = "Auto Backup",
                description = "Automatically backup messages",
                checked = autoBackup,
                onCheckedChange = { newValue ->
                    autoBackup = newValue
                    scope.launch {
                        context.dataStore.edit { preferences ->
                            preferences[BACKUP_KEY] = newValue
                        }
                    }
                }
            )
            
            Button(
                onClick = {
                    Toast.makeText(context, "Backup feature coming soon!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Backup Now")
            }
        }
        
        // About Section
        SettingsCategory(title = "About") {
            Text(
                text = "WhatsApp Filter v1.0",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "A simple app to filter and organize important WhatsApp messages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Clear Data Dialog
        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear All Data") },
                text = { Text("Are you sure you want to clear all messages? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    AppDatabase.getDatabase(context).messageDao().deleteAll()
                                    Toast.makeText(context, "All data cleared successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error clearing data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showClearDataDialog = false
                        }
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
} 