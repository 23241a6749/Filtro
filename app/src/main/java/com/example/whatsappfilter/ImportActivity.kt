package com.example.whatsappfilter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.whatsappfilter.data.Message
import com.example.whatsappfilter.data.AppDatabase
import com.example.whatsappfilter.data.MessageDao
import com.example.whatsappfilter.ui.theme.WhatsAppFilterTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

class ImportActivity : ComponentActivity() {
    companion object {
        const val PICK_FILE_REQUEST = 1
        private const val IMAGE_QUALITY = 85
        private const val MAX_IMAGE_SIZE = 1024 // Maximum dimension in pixels
        
        // Regex patterns for parsing WhatsApp chat format
        private val datePattern = Regex("""\[(\d{2}/\d{2}/\d{2})""")
        private val messagePattern = Regex("""\[(\d{2}/\d{2}/\d{2}), (\d{2}:\d{2}:\d{2})\] ([^:]+): (.+)""")
        
        // Date and time formatters for parsing WhatsApp chat format
        private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy")
        private val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
    }

    private var isLoading by mutableStateOf(false)
    private var isDarkTheme by mutableStateOf(false)
    private var processedImages by mutableStateOf(0)
    private var totalImages by mutableStateOf(0)
    private lateinit var messageDao: MessageDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize messageDao
        messageDao = AppDatabase.getDatabase(this).messageDao()
        
        // Load saved theme preference
        lifecycleScope.launch {
            isDarkTheme = dataStore.data.first()[DARK_THEME_KEY] ?: false
        }
        
        setContent {
            val transition = updateTransition(targetState = isDarkTheme, label = "themeTransition")
            val backgroundColor by transition.animateColor(
                transitionSpec = { tween(durationMillis = 300) },
                label = "backgroundColor"
            ) { isDark ->
                if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background
            }
            
            WhatsAppFilterTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor
                ) {
                    ImportScreen()
                }
            }
        }
    }

    private fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        lifecycleScope.launch {
            dataStore.edit { preferences ->
                preferences[DARK_THEME_KEY] = isDarkTheme
            }
        }
    }

    @Composable
    fun ImportScreen() {
        val context = LocalContext.current
        var isExpanded by remember { mutableStateOf(false) }
        var showTooltip by remember { mutableStateOf(false) }
        val animatedProgress by animateFloatAsState(
            targetValue = if (isLoading) 1f else 0f,
            animationSpec = tween(durationMillis = 500)
        )
        
        val iconScale by animateFloatAsState(
            targetValue = if (isDarkTheme) 1.2f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "iconScale"
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            // Theme Toggle Button with Custom Tooltip
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = {
                        toggleTheme()
                        showTooltip = true
                        lifecycleScope.launch {
                            delay(2000) // Show tooltip for 2 seconds
                            showTooltip = false
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .scale(iconScale)
                        .alpha(animatedProgress)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Custom Tooltip
                AnimatedVisibility(
                    visible = showTooltip,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 8.dp)
                            .shadow(4.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated App Logo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            rotationZ = animatedProgress * 360f
                            scaleX = 1f + (animatedProgress * 0.2f)
                            scaleY = 1f + (animatedProgress * 0.2f)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.ImportContacts,
                        contentDescription = "Import Icon",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Animated Title
                Text(
                    text = "Import WhatsApp Chat",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = animatedProgress
                            translationY = (1f - animatedProgress) * 50f
                        }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Animated Description
                Text(
                    text = "Select a WhatsApp chat export file to filter and organize important messages",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = animatedProgress
                            translationY = (1f - animatedProgress) * 30f
                        }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Expandable File Type Info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Supported File Types",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    FileTypeItem(
                                        icon = Icons.Default.Description,
                                        text = "Text files (.txt)",
                                        description = "Standard WhatsApp chat exports"
                                    )
                                    FileTypeItem(
                                        icon = Icons.Default.Archive,
                                        text = "ZIP archives (.zip)",
                                        description = "Chat exports with media files"
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (isLoading) {
                    // Enhanced Loading State with Image Progress
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .animateContentSize()
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Processing your file...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (totalImages > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Processed $processedImages of $totalImages images",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = if (totalImages > 0) processedImages.toFloat() / totalImages else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Enhanced Import Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                                    "application/zip",
                                    "application/x-zip",
                                    "application/x-zip-compressed",
                                    "text/plain"
                                ))
                            }
                            startActivityForResult(intent, PICK_FILE_REQUEST)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer {
                                scaleX = 1f + (animatedProgress * 0.1f)
                                scaleY = 1f + (animatedProgress * 0.1f)
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.medium,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select File",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Animated Help Text
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .graphicsLayer {
                            alpha = animatedProgress
                            translationY = (1f - animatedProgress) * 20f
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "How to export WhatsApp chats",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Open WhatsApp\n" +
                                   "2. Select the chat\n" +
                                   "3. Tap ⋮ → More → Export chat\n" +
                                   "4. Choose whether to include media",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun FileTypeItem(
        icon: ImageVector,
        text: String,
        description: String
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                processChatFile(uri)
            }
        }
    }

    private fun processChatFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                isLoading = true
                processedImages = 0
                totalImages = 0
                
                Log.d("ImportActivity", "Starting to process file: $uri")
                val fileName = getFileName(uri)
                Log.d("ImportActivity", "Processing file: $fileName")
                
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    when {
                        fileName?.endsWith(".zip", ignoreCase = true) == true -> {
                            Log.d("ImportActivity", "Detected ZIP file, starting processing")
                            processZipFile(inputStream)
                        }
                        fileName?.endsWith(".txt", ignoreCase = true) == true -> {
                            Log.d("ImportActivity", "Detected text file, starting processing")
                            processTextFile(inputStream)
                        }
                        else -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ImportActivity,
                                    "Unsupported file type: $fileName. Please use .zip or .txt files.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            Log.e("ImportActivity", "Unsupported file type: $fileName")
                        }
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ImportActivity,
                            "Failed to open file",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Log.e("ImportActivity", "Failed to open input stream for URI: $uri")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ImportActivity,
                        "Error processing file: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.e("ImportActivity", "Error processing file", e)
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun processZipFile(inputStream: InputStream) {
        try {
            ZipInputStream(inputStream).use { zipInputStream ->
                // First pass: count total images
                var totalEntries = 0
                var imageEntries = 0
                var textEntries = 0
                
                var entry: ZipEntry?
                while (zipInputStream.nextEntry.also { entry = it } != null) {
                    totalEntries++
                    when {
                        entry?.name?.endsWith(".jpg", ignoreCase = true) == true ||
                        entry?.name?.endsWith(".jpeg", ignoreCase = true) == true ||
                        entry?.name?.endsWith(".png", ignoreCase = true) == true -> {
                            imageEntries++
                        }
                        entry?.name?.endsWith(".txt", ignoreCase = true) == true -> {
                            textEntries++
                        }
                    }
                    zipInputStream.closeEntry()
                }
                
                Log.d("ImportActivity", "ZIP contains: $totalEntries total entries, $imageEntries images, $textEntries text files")
                
                // Reset stream for second pass
                inputStream.reset()
                ZipInputStream(inputStream).use { resetZipStream ->
                    totalImages = imageEntries
                    processedImages = 0
                    
                    while (resetZipStream.nextEntry.also { entry = it } != null) {
                        entry?.let { currentEntry ->
                            val entryName = currentEntry.name
                            Log.d("ImportActivity", "Processing ZIP entry: $entryName")
                            
                            when {
                                entryName.endsWith(".txt", ignoreCase = true) -> {
                                    processTextFile(resetZipStream)
                                }
                                entryName.endsWith(".jpg", ignoreCase = true) ||
                                entryName.endsWith(".jpeg", ignoreCase = true) ||
                                entryName.endsWith(".png", ignoreCase = true) -> {
                                    processImageFile(resetZipStream, entryName)
                                    processedImages++
                                }
                                else -> {
                                    Log.d("ImportActivity", "Skipping unsupported entry: $entryName")
                                }
                            }
                        }
                        resetZipStream.closeEntry()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImportActivity", "Error processing ZIP file", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ImportActivity,
                    "Error processing ZIP file: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun processTextFile(inputStream: InputStream) {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val messages = mutableListOf<Message>()
            var currentDate: LocalDate? = null
            var lineCount = 0
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    lineCount++
                    if (lineCount % 1000 == 0) {
                        Log.d("ImportActivity", "Processing line $lineCount")
                    }
                    
                    // Skip empty lines
                    if (line.isBlank()) return@forEach
                    
                    // Try to parse date from line
                    val dateMatch = datePattern.find(line)
                    if (dateMatch != null) {
                        try {
                            currentDate = LocalDate.parse(dateMatch.value, dateFormatter)
                            return@forEach
                        } catch (e: DateTimeParseException) {
                            Log.w("ImportActivity", "Failed to parse date: ${dateMatch.value}")
                        }
                    }
                    
                    // Try to parse message from line
                    val messageMatch = messagePattern.find(line)
                    if (messageMatch != null && currentDate != null) {
                        val (time, sender, content) = messageMatch.destructured
                        try {
                            val timestamp = LocalDateTime.of(
                                currentDate,
                                LocalTime.parse(time, timeFormatter)
                            ).atZone(ZoneId.systemDefault()).toInstant()
                            
                            messages.add(Message(
                                id = UUID.randomUUID().toString(),
                                timestamp = Date.from(timestamp),
                                sender = sender.trim(),
                                content = content.trim(),
                                isRead = true
                            ))
                            
                            // Batch insert every 100 messages
                            if (messages.size >= 100) {
                                messageDao.insertAll(messages)
                                messages.clear()
                            }
                        } catch (e: Exception) {
                            Log.w("ImportActivity", "Failed to parse message: $line", e)
                        }
                    }
                }
            }
            
            // Insert any remaining messages
            if (messages.isNotEmpty()) {
                messageDao.insertAll(messages)
            }
            
            Log.d("ImportActivity", "Processed $lineCount lines")
            
        } catch (e: Exception) {
            Log.e("ImportActivity", "Error processing text file", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ImportActivity,
                    "Error processing text file: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun processImageFile(inputStream: InputStream, fileName: String?) {
        try {
            totalImages++
            // Read the image
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                // Resize image if needed
                val resizedBitmap = resizeBitmap(bitmap)
                
                // Create output directory
                val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "WhatsAppFilter")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                // Generate unique filename
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val outputFile = File(outputDir, "filtered_${timestamp}_${fileName}")
                
                // Save the processed image
                FileOutputStream(outputFile).use { out ->
                    resizedBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        IMAGE_QUALITY,
                        out
                    )
                }
                
                // Update progress
                processedImages++
                Log.d("ImportActivity", "Processed image: ${outputFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("ImportActivity", "Error processing image: $fileName", e)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        return if (width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE) {
            val scale = if (width > height) {
                MAX_IMAGE_SIZE.toFloat() / width
            } else {
                MAX_IMAGE_SIZE.toFloat() / height
            }
            
            Bitmap.createScaledBitmap(
                bitmap,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
    }

    private fun getFileName(uri: Uri): String? {
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (columnIndex != -1) {
                    return cursor.getString(columnIndex)
                }
            }
        }
        return null
    }
} 
