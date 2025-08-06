package com.example.fitnessapp.model

data class EnhancedWorkoutResponse(
    val message: String,
    val workoutId: Int,
    val workout: SavedWorkout?
)