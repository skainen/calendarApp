package org.example.project.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SettingsRepository(private val database: AppDatabase) {
    private val queries = database.userSettingsQueries
    
    fun getSettings(): Flow<UserSettings?> {
        return queries.getSettings()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
    }
    
    suspend fun updateSettings(
        userName: String,
        notificationsEnabled: Boolean,
        darkModeEnabled: Boolean,
        defaultTaskDuration: Long
    ) = withContext(Dispatchers.Default) {
        queries.updateSettings(
            userName = userName,
            notificationsEnabled = notificationsEnabled,
            darkModeEnabled = darkModeEnabled,
            defaultTaskDuration = defaultTaskDuration
        )
    }
    
    suspend fun updateUserName(name: String) = withContext(Dispatchers.Default) {
        queries.updateUserName(name)
    }
    
    suspend fun updateNotifications(enabled: Boolean) = withContext(Dispatchers.Default) {
        queries.updateNotifications(enabled)
    }
    
    suspend fun updateDarkMode(enabled: Boolean) = withContext(Dispatchers.Default) {
        queries.updateDarkMode(enabled)
    }
    
    suspend fun updateDefaultDuration(duration: Long) = withContext(Dispatchers.Default) {
        queries.updateDefaultDuration(duration)
    }
}
