package au.edu.jcu.assessment.utilityapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RadarLime = Color(0xFFD7F76A)
val RadarInk = Color(0xFF20251F)
private val Light = lightColorScheme(
    primary = RadarInk, onPrimary = Color.White,
    primaryContainer = RadarLime, onPrimaryContainer = RadarInk,
    secondary = Color(0xFF5B6942), secondaryContainer = Color(0xFFEAF0D9), onSecondaryContainer = RadarInk,
    background = Color(0xFFF7F8F2), onBackground = RadarInk,
    surface = Color.White, onSurface = RadarInk,
    surfaceVariant = Color(0xFFF0F2E9), onSurfaceVariant = Color(0xFF65705F),
    outline = Color(0xFF76816F), outlineVariant = Color(0xFFE1E5DA))
private val Dark = darkColorScheme(
    primary = RadarLime, onPrimary = RadarInk,
    primaryContainer = Color(0xFF394629), onPrimaryContainer = RadarLime,
    secondary = Color(0xFFBCD0A0), secondaryContainer = Color(0xFF343E2B), onSecondaryContainer = Color(0xFFE4F0D3),
    background = Color(0xFF131711), onBackground = Color(0xFFF0F3E9),
    surface = Color(0xFF1F251D), onSurface = Color(0xFFF0F3E9),
    surfaceVariant = Color(0xFF2A3125), onSurfaceVariant = Color(0xFFBBC4B2), outlineVariant = Color(0xFF3A4433))
@Composable
fun RadarTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = RadarTypography, content = content)
}
