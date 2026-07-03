package com.nenah.cinetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CinemaGold,
    secondary = CinemaMint,
    tertiary = CinemaCoral,
    background = CinemaInk,
    surface = CinemaSurface,
    onPrimary = CinemaInk,
    onSecondary = CinemaInk,
    onTertiary = CinemaInk,
    onBackground = CinemaText,
    onSurface = CinemaText,
    surfaceVariant = Color(0xFF1D232D),
    onSurfaceVariant = CinemaMuted
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8A5A00),
    secondary = Color(0xFF006B57),
    tertiary = Color(0xFF9B3F2F),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF17120B),
    onSurface = Color(0xFF17120B),
    surfaceVariant = Color(0xFFF0E7DC),
    onSurfaceVariant = Color(0xFF6C6258)
)

@Composable
fun CineTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is intentionally disabled: it overrides the curated
    // CineTracker palettes with wallpaper-derived colors on Android 12+.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
