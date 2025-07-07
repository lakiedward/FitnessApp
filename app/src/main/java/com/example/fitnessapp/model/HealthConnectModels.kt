package com.example.fitnessapp.model

import androidx.health.connect.client.records.HeartRateRecord
import java.time.Instant

// Serializable heart rate data for API communication
data class HeartRateData(
    val time: String, // ISO 8601 format
    val beatsPerMinute: Long
)

data class HealthActivity(
    val id: String,
    val exerciseType: Int,
    val startTime: String, // ISO 8601 format for API compatibility
    val endTime: String,   // ISO 8601 format for API compatibility
    val duration: Long, // în secunde
    val heartRateData: List<HeartRateData>, // Changed to serializable format
    val steps: Long,
    val calories: Double,
    val distance: Double, // în metri
    val title: String? = null,
    val notes: String? = null
)

data class HealthConnectSyncRequest(
    val activities: List<HealthActivity>,
    val syncTimestamp: Long = System.currentTimeMillis()
)

data class HealthConnectSyncResponse(
    val success: Boolean,
    val syncedCount: Int,
    val errors: List<String> = emptyList(),
    val message: String
)

// Response model for last sync endpoint
data class LastSyncResponse(
    val lastSync: String?, // ISO 8601 format, null if no sync exists
    val syncedCount: Int
)

// Response model for activities list endpoint
data class HealthConnectActivity(
    val id: String,
    val exerciseType: Int,
    val exerciseName: String,
    val startTime: String,
    val endTime: String,
    val duration: Long,
    val steps: Long,
    val calories: Double,
    val distance: Double,
    val title: String?,
    val notes: String?,
    val heartRateData: List<HeartRateData>
)

// Response model for stats endpoint
data class HealthConnectStats(
    val totalActivities: Int,
    val totalDistance: Double, // în metri
    val totalCalories: Double,
    val totalSteps: Long,
    val avgWorkoutDuration: Double, // în secunde
    val mostCommonExerciseType: String?, // null dacă nu există activități
    val lastActivity: String? // ISO 8601 format, null dacă nu există activități
)

// Request model for manual sync with date range
data class ManualSyncRequest(
    val start_date: String, // YYYY-MM-DD format
    val end_date: String    // YYYY-MM-DD format
)

// Response model for delete activity
data class DeleteActivityResponse(
    val status: String
)
