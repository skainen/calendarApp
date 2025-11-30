package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class TimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val date: LocalDate = LocalDate.now()
) {
    fun toDateTime(): LocalDateTime = LocalDateTime.of(date, startTime)

    fun overlaps(other: TimeSlot): Boolean {
        return startTime < other.endTime && endTime > other.startTime
    }

    companion object {
        // Empty companion object for extension functions
    }
}

data class ScheduledTask(
    val task: TaskData,
    val timeSlot: TimeSlot
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarView(
    suggestedTask: TaskData,
    suggestedTimeSlot: TimeSlot,
    scheduledTasks: List<ScheduledTask> = emptyList(),
    onConfirm: (TimeSlot) -> Unit,
    onCancel: () -> Unit
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }

    // When selectedDate changes, delay showing the time picker
    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            println("DEBUG: selectedDate changed to $selectedDate, delaying time picker...")
            kotlinx.coroutines.delay(300)
            println("DEBUG: Now showing time picker")
            showTimePicker = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (!showTimePicker) "Choose day" else "Choose time"
                    )
                },
                navigationIcon = {
                    if (showTimePicker) {
                        IconButton(onClick = {
                            selectedDate = null
                            showTimePicker = false
                            selectedTimeSlot = null
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            if (selectedTimeSlot != null) {
                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .padding(bottom = 32.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decline")
                        }
                        Button(
                            onClick = { onConfirm(selectedTimeSlot!!) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Task Summary Card
            TaskSummaryCard(
                task = suggestedTask,
                timeSlot = selectedTimeSlot,
                modifier = Modifier.padding(16.dp)
            )

            Divider()

            if (!showTimePicker) {
                // Show day picker
                DayPickerView(
                    suggestedDate = suggestedTimeSlot.date,
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        println("DEBUG: Day selected: $date")
                        selectedDate = date
                    }
                )
            } else {
                // Show time picker
                TimePickerView(
                    selectedDate = selectedDate!!,
                    taskDuration = suggestedTask.estimatedDuration,
                    suggestedTimeSlot = if (suggestedTimeSlot.date == selectedDate) suggestedTimeSlot else null,
                    scheduledTasks = scheduledTasks.filter { it.timeSlot.date == selectedDate },
                    onTimeSelected = { timeSlot ->
                        selectedTimeSlot = timeSlot
                    }
                )
            }
        }
    }
}

@Composable
fun DayPickerView(
    suggestedDate: LocalDate,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val next7Days = remember { (0..6).map { today.plusDays(it.toLong()) } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Pick a day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Debug text
            if (selectedDate != null) {
                Text(
                    "Selected: ${selectedDate.format(DateTimeFormatter.ofPattern("d.M.yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        items(next7Days) { date ->
            DayCard(
                date = date,
                isSuggested = date == suggestedDate,
                isToday = date == today,
                isSelected = date == selectedDate,
                onClick = {
                    println("DEBUG: Day clicked: $date")
                    onDateSelected(date)
                }
            )
        }
    }
}

@Composable
fun DayCard(
    date: LocalDate,
    isSuggested: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayName = date.format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale("fi", "FI")))
    val dateStr = date.format(DateTimeFormatter.ofPattern("d.M.yyyy"))

    // Animate the color change - ONLY show color when selected
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 150),
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dayName.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isToday) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        // color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            "Todasy",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                if (isSuggested) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "Suggested",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerView(
    selectedDate: LocalDate,
    taskDuration: Int,
    suggestedTimeSlot: TimeSlot?,
    scheduledTasks: List<ScheduledTask>,
    onTimeSelected: (TimeSlot) -> Unit
) {
    val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("EEEE d.M", java.util.Locale("fi", "FI")))

    // Generate time slots every 30 minutes from 6:00 to 22:00. TO DO: IMPROVE
    val timeSlots = remember {
        val slots = mutableListOf<LocalTime>()
        var time = LocalTime.of(6, 0)
        while (time.isBefore(LocalTime.of(22, 0))) {
            slots.add(time)
            time = time.plusMinutes(30)
        }
        slots
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "What o clock? (${dateStr.replaceFirstChar { it.uppercase() }})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Task duration: $taskDuration min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Group times by hour
        val groupedTimes = timeSlots.groupBy { it.hour }

        items(groupedTimes.keys.toList()) { hour ->
            val times = groupedTimes[hour] ?: emptyList()

            Column {
                Text(
                    "${hour}:00",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    times.forEach { time ->
                        val slot = TimeSlot(
                            startTime = time,
                            endTime = time.plusMinutes(taskDuration.toLong()),
                            date = selectedDate
                        )

                        val isSuggested = suggestedTimeSlot?.startTime == time
                        val isOccupied = scheduledTasks.any { it.timeSlot.overlaps(slot) }

                        TimeChip(
                            time = time,
                            isSuggested = isSuggested,
                            isOccupied = isOccupied,
                            onClick = {
                                if (!isOccupied) {
                                    onTimeSelected(slot)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeChip(
    time: LocalTime,
    isSuggested: Boolean,
    isOccupied: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isOccupied -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        isSuggested -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isSuggested -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        modifier = modifier
            .height(56.dp)
            .then(
                if (!isOccupied) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .border(
                width = if (isSuggested) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSuggested) FontWeight.Bold else FontWeight.Normal,
                    color = if (isOccupied)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (isOccupied) {
                    Text(
                        "Already scheduled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun TaskSummaryCard(
    task: TaskData,
    timeSlot: TimeSlot?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = task.description,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Duration: ${task.estimatedDuration} min",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Mental load: ${task.mentalLoad}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (timeSlot != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Chosen time:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${timeSlot.date.format(DateTimeFormatter.ofPattern("d.M.yyyy"))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${timeSlot.startTime} - ${timeSlot.endTime}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// Helper function to suggest optimal time slot based on mental load
fun suggestTimeSlot(task: TaskData): TimeSlot {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()

    // Suggest based on mental load. Default settings, to be optimized with actual user data.
    val suggestedHour = when (task.mentalLoad.lowercase()) {
        "high" -> 9  // Morning for high mental load
        "medium" -> 14 // Early afternoon for medium
        "low" -> 17   // Late afternoon/evening for low
        else -> 10
    }

    // If suggested time is in the past today, suggest tomorrow
    val suggestedTime = LocalTime.of(suggestedHour, 0)
    val suggestedDate = if (now.toLocalTime().isAfter(suggestedTime.plusMinutes(task.estimatedDuration.toLong()))) {
        today.plusDays(1)
    } else {
        today
    }

    return TimeSlot(
        startTime = suggestedTime,
        endTime = suggestedTime.plusMinutes(task.estimatedDuration.toLong()),
        date = suggestedDate
    )
}