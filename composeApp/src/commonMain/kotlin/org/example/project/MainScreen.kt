package org.example.project

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Sealed class for navigation destinations
sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Alotus")
    object Chat : Screen("chat", "Lisää tehtävä")
    object Calendar : Screen("calendar", "Kalenteri")
    object Settings : Screen("settings", "Asetukset")
}

@Composable
fun MainApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var scheduledTasks by remember { mutableStateOf(listOf<ScheduledTask>()) }
    var showCalendarView by remember { mutableStateOf(false) }
    var pendingTask by remember { mutableStateOf<TaskData?>(null) }
    var pendingSuggestedSlot by remember { mutableStateOf<TimeSlot?>(null) }

    // Handle calendar view overlay
    if (showCalendarView && pendingTask != null && pendingSuggestedSlot != null) {
        CalendarView(
            suggestedTask = pendingTask!!,
            suggestedTimeSlot = pendingSuggestedSlot!!,
            scheduledTasks = scheduledTasks,
            onConfirm = { selectedSlot ->
                scheduledTasks = scheduledTasks + ScheduledTask(pendingTask!!, selectedSlot)
                showCalendarView = false
                pendingTask = null
                pendingSuggestedSlot = null
                currentScreen = Screen.Home // Navigate to home after confirming
            },
            onCancel = {
                showCalendarView = false
                pendingTask = null
                pendingSuggestedSlot = null
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentScreen == Screen.Home,
                    onClick = { currentScreen = Screen.Home }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Add Task") },
                    label = { Text("Add Task") },
                    selected = currentScreen == Screen.Chat,
                    onClick = { currentScreen = Screen.Chat }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    selected = currentScreen == Screen.Calendar,
                    onClick = { currentScreen = Screen.Calendar }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentScreen == Screen.Settings,
                    onClick = { currentScreen = Screen.Settings }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    scheduledTasks = scheduledTasks,
                    onAddTaskClick = { currentScreen = Screen.Chat }
                )
                Screen.Chat -> ChatScreen(
                    onTaskAnalyzed = { task, slot ->
                        pendingTask = task
                        pendingSuggestedSlot = slot
                        showCalendarView = true
                    }
                )
                Screen.Calendar -> CalendarOverviewScreen(scheduledTasks = scheduledTasks)
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scheduledTasks: List<ScheduledTask>,
    onAddTaskClick: () -> Unit
) {
    val today = LocalDate.now()

    val next7Days = (0..6).map { today.plusDays(it.toLong()) }

    // Group tasks by date
    val tasksByDate = scheduledTasks
        .filter { it.timeSlot.date in next7Days }
        .sortedBy { it.timeSlot.toDateTime() }
        .groupBy { it.timeSlot.date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coming tasks") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTaskClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        if (scheduledTasks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "No tasks",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "+ to add task",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Next 7 Days",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${tasksByDate.values.sumOf { it.size }} tasks scheduled",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                next7Days.forEach { date ->
                    val tasksForDate = tasksByDate[date] ?: emptyList()
                    if (tasksForDate.isNotEmpty()) {
                        item {
                            DateSection(date = date, tasks = tasksForDate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateSection(date: LocalDate, tasks: List<ScheduledTask>) {
    val isToday = date == LocalDate.now()
    val dayName = date.format(DateTimeFormatter.ofPattern("EEEE"))
    val dateStr = date.format(DateTimeFormatter.ofPattern("MMM dd"))

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$dayName, $dateStr",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isToday) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "Today",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        tasks.forEach { scheduledTask ->
            TaskCard(scheduledTask = scheduledTask)
        }
    }
}

@Composable
fun TaskCard(scheduledTask: ScheduledTask) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = scheduledTask.task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${scheduledTask.timeSlot.startTime} - ${scheduledTask.timeSlot.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip(label = scheduledTask.task.taskType)
                    Chip(label = "${scheduledTask.task.estimatedDuration}m")
                    Chip(
                        label = scheduledTask.task.mentalLoad,
                        color = when (scheduledTask.task.mentalLoad.lowercase()) {
                            "high" -> MaterialTheme.colorScheme.errorContainer
                            "medium" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Chip(label: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarOverviewScreen(scheduledTasks: List<ScheduledTask>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "All tasks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val tasksByDate = scheduledTasks
                .sortedBy { it.timeSlot.toDateTime() }
                .groupBy { it.timeSlot.date }

            tasksByDate.forEach { (date, tasks) ->
                item {
                    DateSection(date = date, tasks = tasks)
                }
            }

            if (scheduledTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No upcoming tasks",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    "User settings here",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Age, life situation...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}