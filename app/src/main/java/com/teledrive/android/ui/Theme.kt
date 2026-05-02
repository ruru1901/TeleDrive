package com.teledrive.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6CA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF477B72),
    tertiary = Color(0xFF876D1B),
    surface = Color(0xFFFBFCFE),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    secondary = Color(0xFF82CFC4),
    tertiary = Color(0xFFE6C06A),
    surface = Color(0xFF1A1C1E),
    background = Color(0xFF111316),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
)

@Composable
fun TeleDriveTheme(
    darkMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkMode) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
