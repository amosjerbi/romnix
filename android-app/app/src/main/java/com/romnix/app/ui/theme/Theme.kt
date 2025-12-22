package com.romnix.app.ui.theme

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

// Base Colors - Neutral
// LIGHT THEME - Change these to customize light theme colors
val AppWhite = Color(0xFFFFFFFF)        // Light theme: BACKGROUND & SURFACE (change here for light bg!)
val AppDarkText = Color(0xFF1C1B1F)     // Light theme: TEXT on light backgrounds

// DARK THEME - Change these to customize dark theme colors
val AppDarkBackground = Color(0xFF0F0E11)  // Dark theme: ROOT BACKGROUND (darker for depth)
val DarkSurface = Color(0xFF1C1B1F)        // Dark theme: CARD/SURFACE color (lighter than bg)
val DarkSurfaceVariant = Color(0xFF49454F) // Dark theme: SURFACE VARIANT
val LightText = Color(0xFFE6E1E5)          // Dark theme: TEXT on dark backgrounds

// Brand Colors - Blues & Greens
val NavyBlue = Color(0xFF2F325A)      // Primary dark blue for buttons & text
val VibrantBlue = Color(0xFF3B82F6)   // Vibrant blue for primary actions (bright & saturated)
val DarkBlue = Color(0xFF1E3A8A)      // Darker blue for connect buttons (theme-aware base)
val LightBlue = Color(0xFF4AB5FB)     // Light blue accents
val PalestBlue = Color(0xFFEEF8FF)    // Import button backgrounds, action buttons, card borders
val SecondaryGray = Color(0xFF757575) // Light gray for secondary text (Material Design)
val LightGray = Color(0xFFE8E8F0)     // Light gray fill for input fields
val SuccessGreen = Color(0xFF10B981)  // Success states & connected status
val ErrorRed = Color(0xFFEF4444)      // Error states & delete actions

private val LightColors: ColorScheme = lightColorScheme(
    primary = VibrantBlue,               // Vibrant blue for main buttons (bright & saturated)
    onPrimary = AppWhite,
    primaryContainer = AppDarkText,
    onPrimaryContainer = AppDarkText,
    secondary = DarkBlue,                // Darker blue for connect buttons
    onSecondary = AppWhite,              // White text/icon on dark blue buttons
    secondaryContainer = LightBlue,      // Light blue for import button
    onSecondaryContainer = AppWhite,     // White text/icon on light blue buttons
    error = ErrorRed,                    // Red for error states
    onError = AppWhite,
    background = Color(0xFFE5E5E5),      // Darker gray background for contrast
    onBackground = AppDarkText,
    surface = AppWhite,                  // Bright white cards for high contrast
    onSurface = AppDarkText,
    surfaceVariant = AppDarkText.copy(alpha = 0.3f),
    onSurfaceVariant = SecondaryGray     // Light gray for secondary text
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = VibrantBlue,                 // Vibrant blue for primary actions
    onPrimary = AppWhite,
    primaryContainer = VibrantBlue.copy(alpha = 0.3f),
    onPrimaryContainer = LightText,
    secondary = LightBlue,
    onSecondary = AppDarkText,
    secondaryContainer = LightBlue.copy(alpha = 0.4f),  // Increased opacity for better visibility
    onSecondaryContainer = AppWhite,       // White text for better contrast
    background = AppDarkBackground,        // Darkest - for main background
    onBackground = LightText,
    surface = DarkSurface,                 // Lighter than background - for cards
    onSurface = LightText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SecondaryGray
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Material 3 dynamic colors (Material You)
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        // Material 3: Use dynamic colors when available (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) {
                // Dark theme: Use Material You colors with proper background/card separation
                val scheme = dynamicDarkColorScheme(context)
                scheme.copy(
                    background = scheme.surfaceContainerHigh,     // Darker root background for depth
                    surface = scheme.surfaceContainerLow,         // Cards slightly lighter than background
                    surfaceBright = scheme.surfaceContainerHigh   // Modals elevated and bright
                )
            } else {
                // Light theme: Inverted - darker background with brighter cards
                val scheme = dynamicLightColorScheme(context)
                scheme.copy(
                    background = scheme.surfaceContainer,            // Darker background
                    surface = scheme.surfaceBright                   // Brighter cards for high contrast
                )
            }
        }
        // Fallback: Use custom colors for older Android versions
        useDarkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
