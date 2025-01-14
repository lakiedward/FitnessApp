package com.example.fitnessapp.model

data class TrainingPlanResponse(
    val message: String,
    val data: List<TrainingPlan>
)


data class TrainingPlan(
    val id: Int,
    val user_id: Int,
    val date: String,
    val workout_name: String,
    val duration: String,
    val intensity: String,
    val description: String
)
