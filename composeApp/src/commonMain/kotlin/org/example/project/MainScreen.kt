package org.example.project

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ArrowForward
import org.example.project.data.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Sealed class for navigation
sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Alotus")
    object Chat : Screen("chat", "Lisää tehtävä")
    object Calendar : Screen("calendar", "Kalenteri")
    object Settings : Screen("settings", "Asetukset")
}

@Composable
fun MainApp(database: AppDatabase) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val taskRepository = remember { TaskRepository(database) }
    val settingsRepository = remember { SettingsRepository(database) }
    val coroutineScope = rememberCoroutineScope()


    // Get data from database
    val dbTasks by taskRepository.getAllTasks().collectAsState(initial = emptyList())
    val settings by settingsRepository.getSettings().collectAsState(initial = null)

    val scheduledTasks = remember(dbTasks) {
        dbTasks.map { task ->
            ScheduledTask(
                task = TaskData(
                    description = task.title,
                    taskType = "Task",
                    estimatedDuration = 30,
                    mentalLoad = when (task.priority) {
                        2L -> "High"
                        1L -> "Medium"
                        else -> "Low"
                    },
                    deadline = null,
                    priority = task.priority.toFloat() / 2f,
                    reasoning = ""
                ),
                timeSlot = TimeSlot.fromTimestamp(task.scheduledTime)
            )
        }
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
                    onAddTaskClick = { currentScreen = Screen.Chat },
                    onTaskComplete = { task ->
                        coroutineScope.launch {
                            dbTasks.find { it.title == task.task.description }?.let {
                                taskRepository.markTaskComplete(it.id)
                            }
                        }
                    },
                    onTaskDelete = { task ->
                        coroutineScope.launch {
                            dbTasks.find { it.title == task.task.description }?.let {
                                taskRepository.deleteTask(it.id)
                            }
                        }
                    }
                )
                Screen.Chat -> ChatScreen(
                    onTaskAnalyzed = { task, slot ->
                        coroutineScope.launch {
                            taskRepository.addTask(
                                title = task.description,
                                description = task.description,
                                scheduledTime = slot.toTimestamp(),
                                priority = when (task.mentalLoad.lowercase()) {
                                    "high" -> 2L
                                    "medium" -> 1L
                                    else -> 0L
                                }
                            )
                            currentScreen = Screen.Home
                        }
                    }
                )
                Screen.Calendar -> CalendarOverviewScreen(
                    scheduledTasks = scheduledTasks,
                    onTaskReschedule = { oldTask, newSlot ->
                        coroutineScope.launch {
                            dbTasks.find { it.title == oldTask.task.description }?.let { dbTask ->
                                taskRepository.updateTask(
                                    id = dbTask.id,
                                    title = dbTask.title,
                                    description = dbTask.description,
                                    scheduledTime = newSlot.toTimestamp(),
                                    isCompleted = dbTask.isCompleted,
                                    priority = dbTask.priority
                                )
                            }
                        }
                    }
                )
                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    onUpdateSettings = { userName, notifications, darkMode, duration ->
                        coroutineScope.launch {
                            settingsRepository.updateSettings(
                                userName = userName,
                                notificationsEnabled = notifications,
                                darkModeEnabled = darkMode,
                                defaultTaskDuration = duration
                            )
                        }
                    }
                )
            }
        }
    }
}

// Extension functions to convert between TimeSlot and timestamp
fun TimeSlot.toTimestamp(): Long {
    val dateTime = LocalDateTime.of(this.date, this.startTime)
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun TimeSlot.Companion.fromTimestamp(timestamp: Long): TimeSlot {
    val dateTime = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )
    return TimeSlot(
        startTime = dateTime.toLocalTime(),
        endTime = dateTime.toLocalTime().plusMinutes(30), // Default 30 min duration
        date = dateTime.toLocalDate()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scheduledTasks: List<ScheduledTask>,
    onAddTaskClick: () -> Unit,
    onTaskComplete: (ScheduledTask) -> Unit = {},
    onTaskDelete: (ScheduledTask) -> Unit = {}
) {
    val today = LocalDate.now()
    val next7Days = (0..6).map { today.plusDays(it.toLong()) }

    // Group tasks by date and only show incomplete tasks
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

    ) { padding ->
        if (scheduledTasks.isEmpty()) {
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
                            DateSection(
                                date = date,
                                tasks = tasksForDate,
                                onTaskComplete = onTaskComplete,
                                onTaskDelete = onTaskDelete
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateSection(
    date: LocalDate,
    tasks: List<ScheduledTask>,
    onTaskComplete: (ScheduledTask) -> Unit = {},
    onTaskDelete: (ScheduledTask) -> Unit = {}
) {
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
            TaskCard(
                scheduledTask = scheduledTask,
                onComplete = { onTaskComplete(scheduledTask) },
                onDelete = { onTaskDelete(scheduledTask) }
            )
        }
    }
}

@Composable
fun TaskCard(
    scheduledTask: ScheduledTask,
    onComplete: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
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

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onComplete) {
                    Icon(Icons.Default.Check, contentDescription = "Complete")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
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
fun CalendarOverviewScreen(
    scheduledTasks: List<ScheduledTask>,
    onTaskReschedule: (ScheduledTask, TimeSlot) -> Unit = { _, _ -> }
) {
    var selectedTask by remember { mutableStateOf<ScheduledTask?>(null) }
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var currentWeekStart by remember { mutableStateOf(LocalDate.now().with(java.time.DayOfWeek.MONDAY)) }

    val weekDays = remember(currentWeekStart) {
        (0..6).map { currentWeekStart.plusDays(it.toLong()) }
    }

    // Show reschedule dialog
    if (showRescheduleDialog && selectedTask != null) {
        CalendarView(
            suggestedTask = selectedTask!!.task,
            suggestedTimeSlot = selectedTask!!.timeSlot,
            scheduledTasks = scheduledTasks,
            onConfirm = { newSlot ->
                onTaskReschedule(selectedTask!!, newSlot)
                showRescheduleDialog = false
                selectedTask = null
            },
            onCancel = {
                showRescheduleDialog = false
                selectedTask = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Calendar")
                        Text(
                            "Week ${currentWeekStart.format(DateTimeFormatter.ofPattern("d.M"))} - ${currentWeekStart.plusDays(6).format(DateTimeFormatter.ofPattern("d.M.yyyy"))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
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
            // Week navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentWeekStart = currentWeekStart.minusWeeks(1)
                }) {
                    Icon(Icons.Default.ArrowBack, "Previous week")
                }

                Button(onClick = {
                    currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
                }) {
                    Text("Today")
                }

                IconButton(onClick = {
                    currentWeekStart = currentWeekStart.plusWeeks(1)
                }) {
                    Icon(Icons.Default.ArrowForward, "Next week")
                }
            }

            Divider()

            // Weekly calendar view
            WeeklyCalendarView(
                weekDays = weekDays,
                scheduledTasks = scheduledTasks,
                onTaskClick = { task ->
                    selectedTask = task
                    showRescheduleDialog = true
                }
            )
        }
    }
}

@Composable
fun WeeklyCalendarView(
    weekDays: List<LocalDate>,
    scheduledTasks: List<ScheduledTask>,
    onTaskClick: (ScheduledTask) -> Unit
) {
    val today = LocalDate.now()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        weekDays.forEach { date ->
            val tasksForDay = scheduledTasks
                .filter { it.timeSlot.date == date }
                .sortedBy { it.timeSlot.startTime }

            item {
                WeekDayCard(
                    date = date,
                    isToday = date == today,
                    tasks = tasksForDay,
                    onTaskClick = onTaskClick
                )
            }
        }
    }
}

@Composable
fun WeekDayCard(
    date: LocalDate,
    isToday: Boolean,
    tasks: List<ScheduledTask>,
    onTaskClick: (ScheduledTask) -> Unit
) {
    val dayName = date.format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale("fi", "FI")))
    val dateStr = date.format(DateTimeFormatter.ofPattern("d.M"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isToday)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dayName.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isToday) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "Today",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No tasks scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                tasks.forEach { task ->
                    WeeklyTaskItem(
                        task = task,
                        onClick = { onTaskClick(task) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WeeklyTaskItem(
    task: ScheduledTask,
    onClick: () -> Unit
) {
    var isClicked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Animate the background color, need to improve this looks buggy
    val backgroundColor by animateColorAsState(
        targetValue = if (isClicked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 150),
        label = "taskItemColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isClicked = true
                coroutineScope.launch {
                    kotlinx.coroutines.delay(300)
                    onClick()
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (task.task.mentalLoad.lowercase()) {
                            "high" -> MaterialTheme.colorScheme.errorContainer
                            "medium" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            task.task.mentalLoad,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "${task.task.estimatedDuration}m",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = task.timeSlot.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = task.timeSlot.endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings? = null,
    onUpdateSettings: (String, Boolean, Boolean, Long) -> Unit = { _, _, _, _ -> }
) {
    var userName by remember(settings) { mutableStateOf(settings?.userName ?: "") }
    var notificationsEnabled by remember(settings) { mutableStateOf(settings?.notificationsEnabled ?: true) }
    var darkModeEnabled by remember(settings) { mutableStateOf(settings?.darkModeEnabled ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = userName,
                onValueChange = {
                    userName = it
                    onUpdateSettings(it, notificationsEnabled, darkModeEnabled, 30L)
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Toggle between dark and light theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = darkModeEnabled,
                    onCheckedChange = {
                        darkModeEnabled = it
                        onUpdateSettings(userName, notificationsEnabled, it, 30L)
                    }
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Get reminders for upcoming tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        onUpdateSettings(userName, it, darkModeEnabled, 30L)
                    }
                )
            }
        }
    }
}