package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName

data class SavedWorkout(
    @SerializedName("training_plan_id") val trainingPlanId: Int? = null,
    @SerializedName("workout_name") val workoutName: String,
    @SerializedName("start_time") val startTime: String, // Format ISO: "2025-07-15T10:15:37.646Z"
    @SerializedName("end_time") val endTime: String,     // Format ISO: "2025-07-15T10:15:37.646Z"
    @SerializedName("duration") val duration: Int, // Total workout duration in seconds
    @SerializedName("total_power") val totalPower: Int? = null,
    @SerializedName("average_power") val averagePower: Int? = null,
    @SerializedName("max_power") val maxPower: Int? = null,
    @SerializedName("average_heart_rate") val averageHeartRate: Int? = null,
    @SerializedName("max_heart_rate") val maxHeartRate: Int? = null,
    @SerializedName("distance") val distance: Float? = null, // Distance in meters
    @SerializedName("calories_burned") val caloriesBurned: Int? = null,
    @SerializedName("workout_steps") val workoutSteps: List<WorkoutStepData>? = null, // Changed from workout_data
    @SerializedName("performance_metrics") val performanceMetrics: Map<String, Any>? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("workout_type") val workoutType: String = "cycling", // 'cycling', 'running', 'swimming', 'strength', 'other'
    @SerializedName("completed") val completed: Boolean = true // Whether the workout was completed or stopped early
)

data class WorkoutStepData(
    @SerializedName("step_number") val stepNumber: Int,
    @SerializedName("duration") val duration: Int,
    @SerializedName("target_power") val targetPower: Int? = null,
    @SerializedName("target_heart_rate") val targetHeartRate: Int? = null,
    @SerializedName("target_cadence") val targetCadence: Int? = null,
    @SerializedName("actual_power") val actualPower: Int? = null,
    @SerializedName("actual_heart_rate") val actualHeartRate: Int? = null,
    @SerializedName("actual_cadence") val actualCadence: Int? = null,
    @SerializedName("step_type") val stepType: String, // "interval", etc.
    @SerializedName("description") val description: String? = null
)

data class SavedWorkoutResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("workout_id") val workoutId: Int,
    val workout: WorkoutDetails? = null
)

data class WorkoutDetails(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("training_plan_id") val trainingPlanId: Int? = null,
    @SerializedName("workout_name") val workoutName: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    val duration: Int,
    @SerializedName("total_power") val totalPower: Int? = null,
    @SerializedName("average_power") val averagePower: Int? = null,
    @SerializedName("max_power") val maxPower: Int? = null,
    @SerializedName("average_heart_rate") val averageHeartRate: Int? = null,
    @SerializedName("max_heart_rate") val maxHeartRate: Int? = null,
    val distance: Float? = null,
    @SerializedName("calories_burned") val caloriesBurned: Int? = null,
    @SerializedName("workout_data") val workoutData: Any? = null,
    @SerializedName("performance_metrics") val performanceMetrics: Any? = null,
    val notes: String? = null,
    @SerializedName("workout_type") val workoutType: String,
    val completed: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
