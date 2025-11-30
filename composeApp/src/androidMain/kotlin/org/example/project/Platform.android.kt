package org.example.project

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.example.project.data.DatabaseDriverFactory

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

@Composable
actual fun getPlatformDatabaseFactory(): DatabaseDriverFactory {
    val context = LocalContext.current
    return DatabaseDriverFactory(context)
}
