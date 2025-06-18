package com.example.whatsappfilter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whatsappfilter.data.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    onDismiss: () -> Unit,
    onAccept: (List<Message>) -> Unit,
    messages: List<Message>
) {
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }
    var selectedContacts by remember { mutableStateOf(setOf<String>()) }
    var filteredMessages by remember { mutableStateOf(messages) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Messages") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Filter Categories
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(listOf("Assignments", "Exams", "Schedules", "Important")) { filter ->
                        FilterChip(
                            selected = filter in selectedFilters,
                            onClick = {
                                selectedFilters = if (filter in selectedFilters) {
                                    selectedFilters - filter
                                } else {
                                    selectedFilters + filter
                                }
                                updateFilteredMessages(messages, selectedFilters, selectedContacts)
                            },
                            label = { Text(filter) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Selection
                Text(
                    text = "Filter by Contact",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedButton(
                    onClick = { /* Show contact picker */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Select Contacts")
                }

                if (selectedContacts.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedContacts.toList()) { contact ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    selectedContacts = selectedContacts - contact
                                    updateFilteredMessages(messages, selectedFilters, selectedContacts)
                                },
                                label = { Text(contact) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, "Remove contact")
                                }
                            )
                        }
                    }
                }

                // Preview Section
                if (filteredMessages.isNotEmpty()) {
                    Text(
                        text = "Preview (${filteredMessages.size} messages)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                    ) {
                        items(filteredMessages) { message ->
                            MessageItem(
                                message = message,
                                onDelete = {},
                                onMarkAsRead = {},
                                onSetReminder = {}
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onAccept(filteredMessages)
                        onDismiss()
                    },
                    enabled = filteredMessages.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Accept")
                }
            }
        }
    )
}

private fun updateFilteredMessages(
    messages: List<Message>,
    selectedFilters: Set<String>,
    selectedContacts: Set<String>
): List<Message> {
    return messages.filter { message ->
        val matchesFilter = if (selectedFilters.isEmpty()) {
            true
        } else {
            selectedFilters.any { filter ->
                when (filter) {
                    "Assignments" -> isAssignmentRelated(message.content)
                    "Exams" -> isExamRelated(message.content)
                    "Schedules" -> isScheduleRelated(message.content)
                    "Important" -> isImportantMessage(message.content)
                    else -> false
                }
            }
        }

        val matchesContact = if (selectedContacts.isEmpty()) {
            true
        } else {
            message.sender in selectedContacts
        }

        matchesFilter && matchesContact
    }
}

private fun isAssignmentRelated(content: String): Boolean {
    val keywords = listOf("assignment", "homework", "project", "submission")
    return keywords.any { it in content.lowercase() }
}

private fun isExamRelated(content: String): Boolean {
    val keywords = listOf("exam", "test", "quiz", "assessment")
    return keywords.any { it in content.lowercase() }
}

private fun isScheduleRelated(content: String): Boolean {
    val keywords = listOf("schedule", "timetable", "class", "lecture")
    return keywords.any { it in content.lowercase() }
}

private fun isImportantMessage(content: String): Boolean {
    val keywords = listOf("important", "urgent", "asap", "deadline")
    return keywords.any { it in content.lowercase() }
} 