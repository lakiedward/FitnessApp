package com.example.fitnessapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Definirea culorilor brandului
val BrandPurple = Color(0xFF6D28D9)
val BrandPurpleLight = Color(0xFFF3E8FF)
val BrandSecondary = Color(0xFF03DAC6)

// Scheme de culori statice
private val lightColorScheme = lightColorScheme(
    primary = BrandPurple,
    secondary = BrandSecondary,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceVariant = BrandPurpleLight
)

private val darkColorScheme = darkColorScheme(
    primary = BrandPurple,
    secondary = BrandSecondary,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = BrandPurpleLight
)

@Composable
fun FitnessAppTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColor) {
        if (isSystemInDarkTheme()) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        if (isSystemInDarkTheme()) {
            darkColorScheme
        } else {
            lightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}