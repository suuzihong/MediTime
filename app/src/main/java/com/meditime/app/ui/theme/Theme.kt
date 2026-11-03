package com.meditime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 药品可选的 6 种标识色 */
val MedicinePalette = listOf(
    Color(0xFF0E9F8E),
    Color(0xFFE07A5F),
    Color(0xFF5B7FA6),
    Color(0xFFC08A3E),
    Color(0xFF8E6BA8),
    Color(0xFF4E8C4A)
)

fun medicineColor(index: Int): Color = MedicinePalette[index.coerceIn(0, MedicinePalette.lastIndex)]

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B8578),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5EFE6),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFD4704F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCC),
    onSecondaryContainer = Color(0xFF351000),
    tertiary = Color(0xFF4C6478),
    onTertiary = Color.White,
    background = Color(0xFFF5F9F7),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFE1EAE6),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBEC9C5),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FBF8),
    surfaceContainer = Color(0xFFF0F5F2),
    surfaceContainerHigh = Color(0xFFEAF0ED),
    surfaceContainerHighest = Color(0xFFE4EBE7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD8C9),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFFB5EFE6),
    secondary = Color(0xFFFFB59D),
    onSecondary = Color(0xFF55200D),
    secondaryContainer = Color(0xFF73341F),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFB4C9DF),
    onTertiary = Color(0xFF1E3245),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF151B19),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF889390),
    outlineVariant = Color(0xFF3F4946),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0B100E),
    surfaceContainerLow = Color(0xFF171E1B),
    surfaceContainer = Color(0xFF1B221F),
    surfaceContainerHigh = Color(0xFF252C29),
    surfaceContainerHighest = Color(0xFF303835)
)

@Composable
fun MediTimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
