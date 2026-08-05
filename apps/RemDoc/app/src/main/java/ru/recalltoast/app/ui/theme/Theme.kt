package ru.recalltoast.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ru.recalltoast.app.domain.model.ThemeMode

// Brand colors from RemDoc icon
val RemDocCyan = Color(0xFF00E5FF)
val RemDocMagenta = Color(0xFFD500F9)
val RemDocNavy = Color(0xFF0A0E21)
val RemDocNavySoft = Color(0xFF141B33)
val RemDocSurfaceLight = Color(0xFFF4F6FB)
val RemDocOnLight = Color(0xFF12162A)

val RemDocGradient = Brush.linearGradient(
    colors = listOf(RemDocCyan, RemDocMagenta)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B2BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E0FF),
    onPrimaryContainer = Color(0xFF1A0050),
    secondary = Color(0xFF00A8C0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F7FF),
    onSecondaryContainer = Color(0xFF003540),
    tertiary = RemDocMagenta,
    background = RemDocSurfaceLight,
    onBackground = RemDocOnLight,
    surface = Color.White,
    onSurface = RemDocOnLight,
    surfaceVariant = Color(0xFFE6EAF5),
    onSurfaceVariant = Color(0xFF444B63),
    outline = Color(0xFF747B93)
)

private val DarkColors = darkColorScheme(
    primary = RemDocCyan,
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF1A2750),
    onPrimaryContainer = Color(0xFFC8F7FF),
    secondary = RemDocMagenta,
    onSecondary = Color(0xFF3A0050),
    secondaryContainer = Color(0xFF4A1470),
    onSecondaryContainer = Color(0xFFF3D0FF),
    tertiary = RemDocCyan,
    background = RemDocNavy,
    onBackground = Color(0xFFE8ECFF),
    surface = RemDocNavySoft,
    onSurface = Color(0xFFE8ECFF),
    surfaceVariant = Color(0xFF222A45),
    onSurfaceVariant = Color(0xFFB8C0DC),
    outline = Color(0xFF848CAb)
)

@Composable
fun RecallToastTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Brand palette always — no Dynamic Color, keeps RemDoc identity.
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
