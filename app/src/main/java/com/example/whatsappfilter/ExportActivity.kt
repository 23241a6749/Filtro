package com.example.whatsappfilter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whatsappfilter.data.AppDatabase
import com.example.whatsappfilter.data.Message
import com.example.whatsappfilter.ui.theme.WhatsAppFilterTheme
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.rememberCoroutineScope

class ExportActivity : ComponentActivity() {
    companion object {
        const val CREATE_FILE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WhatsAppFilterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExportScreen()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                setContent {
                    exportMessages(uri)
                }
            }
        }
    }

    @Composable
    private fun exportMessages(uri: Uri) {
        val viewModel: MainViewModel = viewModel()
        val scope = rememberCoroutineScope()
        
        scope.launch {
            try {
                val messages = AppDatabase.getDatabase(this@ExportActivity)
                    .messageDao()
                    .getAllMessages()
                    .collect { messageList ->
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                                val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault())
                                
                                messageList.forEach { message ->
                                    writer.write("[${dateFormat.format(message.timestamp)}] ${message.sender}: ${message.content}\n")
                                }
                            }
                        }
                        
                        Toast.makeText(this@ExportActivity, "Messages exported successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } catch (e: Exception) {
                Toast.makeText(this@ExportActivity, "Error exporting messages: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun ExportScreen() {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf("TXT") }
    var includeImages by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Export Messages",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Export Format Selection
        Text(
            text = "Select Export Format",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFormat == "TXT",
                onClick = { selectedFormat = "TXT" },
                label = { Text("Text (.txt)") }
            )
            FilterChip(
                selected = selectedFormat == "CSV",
                onClick = { selectedFormat = "CSV" },
                label = { Text("CSV (.csv)") }
            )
        }
        
        // Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = includeImages,
                onCheckedChange = { includeImages = it }
            )
            Text(
                text = "Include Images",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Export Button
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = when (selectedFormat) {
                        "TXT" -> "text/plain"
                        "CSV" -> "text/csv"
                        else -> "*/*"
                    }
                    putExtra(Intent.EXTRA_TITLE, "WhatsAppFilter_Export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.${selectedFormat.lowercase()}")
                }
                (context as Activity).startActivityForResult(intent, ExportActivity.CREATE_FILE_REQUEST)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Export Messages")
        }
    }
} 