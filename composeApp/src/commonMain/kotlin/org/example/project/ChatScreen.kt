package org.example.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class Message(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onTaskAnalyzed: (TaskData, TimeSlot) -> Unit
) {
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    MessageBubble(
                        message = Message(
                            content = "Let us schedule",
                            isUser = false
                        )
                    )
                }

                items(messages) { message ->
                    MessageBubble(message = message)
                }

                if (isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }

            // Input area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Input here") },
                        enabled = !isLoading,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val userMessage = inputText
                                messages = messages + Message(userMessage, true)
                                inputText = ""

                                // Scroll to bottom
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size)
                                }

                                // Process task and schedule
                                isLoading = true
                                processTask(
                                    taskDescription = userMessage,
                                    onResult = { response, task, slot ->
                                        messages = messages + Message(response, false)
                                        messages = messages + Message(
                                            "✅ Task scheduled for ${slot.date.format(java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy"))} at ${slot.startTime}",
                                            false
                                        )
                                        isLoading = false

                                        // Auto-schedule the task
                                        onTaskAnalyzed(task, slot)

                                        coroutineScope.launch {
                                            listState.animateScrollToItem(messages.size)
                                        }
                                    },
                                    onError = { error ->
                                        messages = messages + Message(error, false)
                                        isLoading = false
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(messages.size)
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isLoading && inputText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScheduleTaskButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Pick a time",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing task...")
            }
        }
    }
}

// Process task
fun processTask(
    taskDescription: String,
    onResult: (String, TaskData, TimeSlot) -> Unit,
    onError: (String) -> Unit
) {
    kotlinx.coroutines.GlobalScope.launch {
        try {
            val apiService = TaskApiService()
            val taskData = apiService.analyzeTask(taskDescription)
            val summary = apiService.formatTaskSummary(taskData)
            val suggestedSlot = suggestTimeSlot(taskData)

            onResult(summary, taskData, suggestedSlot)
        } catch (e: Exception) {
            onError("Error: ${e.message}\n\nTry again or rephrase task.")
        }
    }
}