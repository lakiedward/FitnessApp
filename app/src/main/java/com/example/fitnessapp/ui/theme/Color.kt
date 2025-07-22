package com.example.fitnessapp.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

//grey
val Grey50 = Color(0xFFF8F9FA)
//black
val Grey900 = Color(0xFF202124)
//white
val White = Color(0xFFFFFFFF)
//green
val Green500 = Color(0xFF4CAF50)

// Workout Colors Object for centralized sport-specific theming
object WorkoutColors {
    // Cycling colors
    val cyclingBg = Color(0xFFF0F9FF)
    val cyclingText = Color(0xFF2563EB)

    // Running colors
    val runningBg = Color(0xFFF0FDF4)
    val runningText = Color(0xFF059669)

    // Swimming colors
    val swimmingBg = Color(0xFFFFF7ED)
    val swimmingText = Color(0xFFD97706)

    // Default colors
    val defaultBg = Color(0xFFF8FAFC)
    val defaultText = Color(0xFF1F2937)

    // Common colors
    val primaryPurple = Color(0xFF6366F1)
    val textSecondary = Color(0xFF6B7280)
    val errorRed = Color(0xFFEF4444)

    fun getBackgroundColor(workoutType: String?): Color {
        return when (workoutType?.lowercase()) {
            "cycling" -> cyclingBg
            "running" -> runningBg
            "swimming" -> swimmingBg
            else -> defaultBg
        }
    }

    fun getTextColor(workoutType: String?): Color {
        return when (workoutType?.lowercase()) {
            "cycling" -> cyclingText
            "running" -> runningText
            "swimming" -> swimmingText
            else -> defaultText
        }
    }
}
