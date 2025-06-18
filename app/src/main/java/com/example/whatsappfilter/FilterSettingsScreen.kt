package com.example.whatsappfilter

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.whatsappfilter.data.FilterCategory
import kotlinx.coroutines.launch

@Composable
fun FilterSettingsScreen(
    categories: List<FilterCategory>,
    onCategoryEnabled: (String, Boolean) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (FilterCategory) -> Unit,
    onDeleteCategory: (FilterCategory) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<FilterCategory?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FilterCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.NAME) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Search and Sort
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Categories",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search categories...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sort Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort by:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortChip(
                        text = "Name",
                        selected = sortOrder == SortOrder.NAME,
                        onClick = { sortOrder = SortOrder.NAME }
                    )
                    SortChip(
                        text = "Enabled",
                        selected = sortOrder == SortOrder.ENABLED,
                        onClick = { sortOrder = SortOrder.ENABLED }
                    )
                    SortChip(
                        text = "Keywords",
                        selected = sortOrder == SortOrder.KEYWORDS,
                        onClick = { sortOrder = SortOrder.KEYWORDS }
                    )
                }
            }
        }
        
        // Categories List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filteredAndSortedCategories = categories
                .filter { category ->
                    searchQuery.isEmpty() || 
                    category.name.contains(searchQuery, ignoreCase = true) ||
                    category.description.contains(searchQuery, ignoreCase = true) ||
                    category.keywords.any { it.contains(searchQuery, ignoreCase = true) }
                }
                .sortedWith(
                    when (sortOrder) {
                        SortOrder.NAME -> compareBy { it.name }
                        SortOrder.ENABLED -> compareByDescending { it.isEnabled }
                        SortOrder.KEYWORDS -> compareByDescending { it.keywords.size }
                    }
                )
            
            items(filteredAndSortedCategories) { category ->
                FilterCategoryItem(
                    category = category,
                    onEnabledChange = { onCategoryEnabled(category.id, it) },
                    onEdit = { showEditDialog = category },
                    onDelete = { showDeleteDialog = category }
                )
            }
        }
    }
    
    // Add Category Dialog
    if (showAddDialog) {
        AddEditCategoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, description, keywords, color, icon ->
                // Handle saving new category
                showAddDialog = false
            }
        )
    }
    
    // Edit Category Dialog
    showEditDialog?.let { category ->
        AddEditCategoryDialog(
            category = category,
            onDismiss = { showEditDialog = null },
            onSave = { name, description, keywords, color, icon ->
                // Handle updating category
                showEditDialog = null
            }
        )
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${category.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(category)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterCategoryItem(
    category: FilterCategory,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(category.color).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForCategory(category.icon),
                            contentDescription = null,
                            tint = Color(category.color),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = category.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Switch(
                        checked = category.isEnabled,
                        onCheckedChange = onEnabledChange
                    )
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
            
            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Keywords Section
                    Text(
                        text = "Keywords",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var currentRow = mutableListOf<String>()
                        var currentWidth = 0
                        val maxWidth = 300 // Approximate max width for a row

                        category.keywords.forEach { keyword ->
                            // If adding this keyword would exceed the row width, create a new row
                            if (currentWidth + (keyword.length * 8) > maxWidth) {
                                // Create a row with the current keywords
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    currentRow.forEach { k ->
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(k) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Tag,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // Reset for next row
                                currentRow.clear()
                                currentWidth = 0
                            }
                            // Add keyword to current row
                            currentRow.add(keyword)
                            currentWidth += keyword.length * 8
                        }

                        // Add the last row if there are any remaining keywords
                        if (currentRow.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                currentRow.forEach { keyword ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(keyword) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Tag,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditCategoryDialog(
    category: FilterCategory? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, keywords: List<String>, color: Int, icon: String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var keywords by remember { mutableStateOf(category?.keywords?.joinToString(",") ?: "") }
    var selectedColor by remember { mutableStateOf(Color(category?.color ?: Color.Blue.toArgb())) }
    var selectedIcon by remember { mutableStateOf(category?.icon ?: "assignment") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Color Picker
                Text("Category Color", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Color.Red,
                        Color.Green,
                        Color.Blue,
                        Color.Yellow,
                        Color.Cyan,
                        Color.Magenta
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color)
                                .clickable { selectedColor = color }
                        )
                    }
                }
                
                // Icon Picker
                Text("Category Icon", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "assignment" to Icons.Default.Assignment,
                        "school" to Icons.Default.School,
                        "event" to Icons.Default.Event,
                        "work" to Icons.Default.Work,
                        "star" to Icons.Default.Star
                    ).forEach { (iconName, icon) ->
                        IconButton(
                            onClick = { selectedIcon = iconName },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selectedIcon == iconName)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                        ) {
                            Icon(
                                icon,
                                contentDescription = iconName,
                                tint = if (selectedIcon == iconName)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        description,
                        keywords.split(",").map { it.trim() },
                        selectedColor.toArgb(),
                        selectedIcon
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}

private enum class SortOrder {
    NAME, ENABLED, KEYWORDS
}

private fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        "assignment" -> Icons.Default.Assignment
        "school" -> Icons.Default.School
        "event" -> Icons.Default.Event
        "work" -> Icons.Default.Work
        "star" -> Icons.Default.Star
        else -> Icons.Default.Assignment
    }
} 