package com.example.fitnessapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

val Grey50 = Color(0xFFF8F9FA)
val Grey900 = Color(0xFF202124)
val White = Color(0xFFFFFFFF)
val Green500 = Color(0xFF4CAF50)

val GradientStart = Color(0xFF6366F1)
val GradientCenter = Color(0xFF8B5CF6)
val GradientEnd = Color(0xFFA855F7)

val PrimaryPurple = Color(0xFF6366F1)
val ErrorRed = Color(0xFFDC2626)
val ErrorBackground = Color(0xFFFEF2F2)
val SocialButtonBackground = Color(0xFFF8FAFC)

object WorkoutColors {
    @Composable
    fun getBackgroundColor(workoutType: String?): Color {
        val extended = MaterialTheme.extendedColors
        val surface = MaterialTheme.colorScheme.surfaceVariant
        val accent = when (workoutType?.lowercase()) {
            "cycling" -> extended.chartSpeed
            "running" -> MaterialTheme.colorScheme.tertiary
            "swimming" -> extended.warning
            else -> MaterialTheme.colorScheme.primary
        }
        return accent.copy(alpha = 0.08f).compositeOver(surface)
    }

    @Composable
    fun getTextColor(workoutType: String?): Color {
        val extended = MaterialTheme.extendedColors
        return when (workoutType?.lowercase()) {
            "cycling" -> extended.chartSpeed
            "running" -> MaterialTheme.colorScheme.tertiary
            "swimming" -> extended.warning
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    @Composable
    fun secondaryText(): Color = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun error(): Color = MaterialTheme.colorScheme.error
}


