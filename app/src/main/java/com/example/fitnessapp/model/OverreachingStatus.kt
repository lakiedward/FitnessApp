package com.example.fitnessapp.model

data class OverreachingStatus(
    val isOverreaching: Boolean,
    val overreachingLevel: String,
    val recommendedAction: String,
    val recoveryTimeNeeded: Int,
    val message: String
)