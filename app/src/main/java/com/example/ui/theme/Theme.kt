package com.example.ui.theme

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
    primary = VkBlueLight,
    onPrimary = Color.White,
    primaryContainer = VkBlueDark,
    onPrimaryContainer = Color.White,
    secondary = VkAccentCyan,
    onSecondary = Color.Black,
    background = VkDarkBackground,
    onBackground = VkTextPrimaryDark,
    surface = VkDarkSurface,
    onSurface = VkTextPrimaryDark,
    surfaceVariant = VkDarkSurfaceVariant,
    onSurfaceVariant = VkTextSecondaryDark,
    outline = VkDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = VkBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E7FF),
    onPrimaryContainer = VkBlueDark,
    secondary = VkBlueLight,
    onSecondary = Color.White,
    background = VkLightBackground,
    onBackground = VkTextPrimaryLight,
    surface = VkLightSurface,
    onSurface = VkTextPrimaryLight,
    surfaceVariant = VkLightSurfaceVariant,
    onSurfaceVariant = VkTextSecondaryLight,
    outline = VkLightOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode favored by music players
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
