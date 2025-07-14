package com.example.fitnessapp.model

// Modelele pentru dispozitive trainer
data class TrainerDevice(
    val id: String,
    val name: String,
    val type: TrainerType,
    val isConnected: Boolean = false,
    val batteryLevel: Int? = null,
    val signalStrength: Int? = null
)

enum class TrainerType {
    SMART_TRAINER,
    POWER_METER,
    HEART_RATE_MONITOR,
    CADENCE_SENSOR,
    SPEED_SENSOR
}

// Model pentru sesiunea de antrenament
data class WorkoutSession(
    val trainingPlan: TrainingPlan,
    val startTime: Long,
    val currentStep: Int = 0,
    val elapsedTime: Int = 0,
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val totalSteps: Int = 0
)

// Date în timp real de la trainer
data class RealTimeData(
    val power: Int = 0,
    val heartRate: Int = 0,
    val cadence: Int = 0,
    val speed: Float = 0f,
    val distance: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}