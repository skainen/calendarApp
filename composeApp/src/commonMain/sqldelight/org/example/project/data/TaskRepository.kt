package org.example.project.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(private val database: AppDatabase) {
    private val queries = database.taskQueries
    
    fun getAllTasks(): Flow<List<Task>> {
        return queries.getAllTasks()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
    
    fun getIncompleteTasks(): Flow<List<Task>> {
        return queries.getIncompleteTasks()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
    
    fun getCompletedTasks(): Flow<List<Task>> {
        return queries.getCompletedTasks()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
    
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<Task>> {
        return queries.getTasksForDay(startOfDay, endOfDay)
            .asFlow()
            .mapToList(Dispatchers.Default)
    }
    
    suspend fun getTaskById(id: Long): Task? = withContext(Dispatchers.Default) {
        queries.getTaskById(id).executeAsOneOrNull()
    }
    
    suspend fun addTask(
        title: String,
        description: String,
        scheduledTime: Long,
        priority: Long = 0
    ) = withContext(Dispatchers.Default) {
        queries.insertTask(
            title = title,
            description = description,
            scheduledTime = scheduledTime,
            isCompleted = false,
            priority = priority,
            createdAt = System.currentTimeMillis()
        )
    }
    
    suspend fun updateTask(
        id: Long,
        title: String,
        description: String,
        scheduledTime: Long,
        isCompleted: Boolean,
        priority: Long
    ) = withContext(Dispatchers.Default) {
        queries.updateTask(
            title = title,
            description = description,
            scheduledTime = scheduledTime,
            isCompleted = isCompleted,
            priority = priority,
            id = id
        )
    }
    
    suspend fun markTaskComplete(id: Long) = withContext(Dispatchers.Default) {
        queries.markComplete(id)
    }
    
    suspend fun markTaskIncomplete(id: Long) = withContext(Dispatchers.Default) {
        queries.markIncomplete(id)
    }
    
    suspend fun deleteTask(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteTask(id)
    }
    
    suspend fun deleteAllTasks() = withContext(Dispatchers.Default) {
        queries.deleteAllTasks()
    }
}
