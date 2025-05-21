package com.example.fitnessapp.model

sealed class WorkoutStep {
    data class SteadyState(
        val type: String = "SteadyState",
        val duration: Int,
        val power: Float
    ) : WorkoutStep()

    data class IntervalsT(
        val type: String = "IntervalsT",
        val repeat: Int,
        val on_duration: Int,
        val on_power: Float,
        val off_duration: Int,
        val off_power: Float
    ) : WorkoutStep()
}
