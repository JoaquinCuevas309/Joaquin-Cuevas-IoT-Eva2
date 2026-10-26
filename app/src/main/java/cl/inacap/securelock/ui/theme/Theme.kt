package cl.inacap.securelock.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B5CAD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E7FA),
    onPrimaryContainer = Color(0xFF062A4D),
    secondary = Color(0xFF3B5B7A),
    secondaryContainer = Color(0xFFD6E7FA),
    onSecondaryContainer = Color(0xFF062A4D),
    surfaceContainer = Color(0xFFFFFFFF),
    background = Color(0xFFF4F7FA),
    onBackground = Color(0xFF14202B),
    surface = Color.White,
    onSurface = Color(0xFF14202B),
    surfaceVariant = Color(0xFFE3EAF1),
    onSurfaceVariant = Color(0xFF4A5A69),
    outline = Color(0xFF8796A5),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFBE3E1),
    onErrorContainer = Color(0xFF5C0F0A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CB7F2),
    onPrimary = Color(0xFF002E55),
    primaryContainer = Color(0xFF123B66),
    onPrimaryContainer = Color(0xFFD6E7FA),
    secondary = Color(0xFFA9C4DE),
    secondaryContainer = Color(0xFF123B66),
    onSecondaryContainer = Color(0xFFD6E7FA),
    surfaceContainer = Color(0xFF1A2531),
    background = Color(0xFF0D141B),
    onBackground = Color(0xFFE4EAF0),
    surface = Color(0xFF141D26),
    onSurface = Color(0xFFE4EAF0),
    surfaceVariant = Color(0xFF223040),
    onSurfaceVariant = Color(0xFFA9B6C3),
    outline = Color(0xFF6C7B8A),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5C1A14),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Immutable
data class StatusColors(val closed: Color, val open: Color, val online: Color, val offline: Color)

val LocalStatusColors = staticCompositionLocalOf { StatusColors(Color(0xFF1B8A5A), Color(0xFFC0392B), Color(0xFF0B5CAD), Color(0xFF9A5B00)) }

@Composable
fun SecureLockTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val status = if (darkTheme) {
        StatusColors(Color(0xFF4CC38A), Color(0xFFFF7A6B), Color(0xFF7CB7F2), Color(0xFFFFB454))
    } else {
        StatusColors(Color(0xFF1B8A5A), Color(0xFFC0392B), Color(0xFF0B5CAD), Color(0xFF9A5B00))
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
    CompositionLocalProvider(LocalStatusColors provides status) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
    }
}
