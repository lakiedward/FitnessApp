package com.example.fitnessapp.model

data class EnhancedWorkoutRequest(
    val plannedWorkoutId: Int,
    val isPlanned: Boolean,
    val workoutName: String,
    val startTime: String,
    val endTime: String,
    val duration: Int,
    val averagePower: Int,
    val maxPower: Int,
    val averageHeartRate: Int?,
    val maxHeartRate: Int?,
    val distance: Float?,
    val caloriesBurned: Int?,
    val workoutType: String,
    val completed: Boolean,
    val notes: String?
)