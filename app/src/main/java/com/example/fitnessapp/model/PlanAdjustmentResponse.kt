package com.example.fitnessapp.model

data class PlanAdjustmentResponse(
    val success: Boolean,
    val message: String,
    val adjustedWorkouts: List<Int>?,
    val adjustmentType: String?,
    val recoveryDaysAdded: Int?
)