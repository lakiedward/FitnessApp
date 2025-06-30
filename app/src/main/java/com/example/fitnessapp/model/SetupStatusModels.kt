package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName

data class SetupStatusResponse(
    val missing: List<String>
)

// Cycling model for /training/ftp/manual
data class DetaliiUserCycling(
    @SerializedName("ftp") val ftp: Int
)

// Running model for /running/pace-prediction/manual
data class PacePredictions(
    val predictions: List<RunningPrediction>
)

data class RunningPrediction(
    @SerializedName("distance_km") val distanceKm: Float,
    val time: String // Format: "4:00"
)

// Swimming model for /swim/best-time-prediction/manual
data class SwimPacePredictions(
    val predictions: List<SwimPrediction>
)

data class SwimPrediction(
    @SerializedName("distance_m") val distanceM: Int,
    val time: String // Format: "1:20"
)

// Running pace prediction model for /running/pace-prediction
data class RunningPacePrediction(
    @SerializedName("distance_km") val distanceKm: Float,
    val time: String, // Format: "MM:SS"
    @SerializedName("pace_min_per_km") val paceMinPerKm: String,
    @SerializedName("avg_hr") val avgHr: Int,
    @SerializedName("adjusted_for_hr") val adjustedForHr: Float
)

// Swimming best time prediction model for /swim/best-time-prediction
data class SwimBestTimePrediction(
    @SerializedName("distance_m") val distanceM: Int,
    val time: String // Format: "MM:SS"
) 