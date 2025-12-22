package com.example.romdownloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Colors from Figma design
val Purple = Color(0xFF797ED2)
val LightPurple = Color(0xFFD8DAFF)
val DarkPurple = Color(0xFF5A5FA8)
val BackgroundLight = Color(0xFFF8F9FF)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1C1B1F)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = LightPurple,
    onPrimaryContainer = DarkPurple,
    secondary = Purple,
    onSecondary = Color.White,
    secondaryContainer = LightPurple,
    onSecondaryContainer = DarkPurple,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = LightPurple.copy(alpha = 0.3f),
    onSurfaceVariant = Purple
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = DarkPurple,
    onPrimaryContainer = LightPurple,
    secondary = LightPurple,
    onSecondary = DarkPurple,
    secondaryContainer = DarkPurple,
    onSecondaryContainer = LightPurple,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF2B2930),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = LightPurple
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to use our custom colors
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
