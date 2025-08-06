package com.example.fitnessapp.model

data class TrainingStats(
    val plannedWorkouts: Int,
    val completedWorkouts: Int,
    val completionRate: Float,
    val totalPlannedDuration: Int,
    val totalActualDuration: Int,
    val averagePlannedPower: Int?,
    val averageActualPower: Int?,
    val averagePlannedHeartRate: Int?,
    val averageActualHeartRate: Int?,
    val periodStartDate: String,
    val periodEndDate: String
)