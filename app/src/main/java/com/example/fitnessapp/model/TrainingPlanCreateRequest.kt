package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName

data class TrainingPlanCreateRequest(
    @SerializedName("date") val date: String,
    @SerializedName("workout_name") val workoutName: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("intensity") val intensity: String,
    @SerializedName("description") val description: String,
    @SerializedName("workout_type") val workoutType: String? = null,
    @SerializedName("zwo_path") val zwoPath: String? = null,
    @SerializedName("steps_json") val stepsJson: List<Map<String, Any>>? = null
)
