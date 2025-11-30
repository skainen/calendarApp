package org.example.project

import androidx.compose.runtime.Composable
import org.example.project.data.DatabaseDriverFactory

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun getPlatformDatabaseFactory(): DatabaseDriverFactory
