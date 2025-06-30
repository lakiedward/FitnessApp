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

    data class Ramp(
        val type: String = "Ramp",
        val duration: Int,
        val start_power: Float,
        val end_power: Float
    ) : WorkoutStep()

    data class FreeRide(
        val type: String = "FreeRide",
        val duration: Int,
        val power_low: Float,
        val power_high: Float
    ) : WorkoutStep()

    data class IntervalsP(
        val type: String = "IntervalsP",
        val repeat: Int,
        val on_duration: Int,
        val on_power: Float,
        val off_duration: Int,
        val off_power: Float,
        val cadence: Int? = null
    ) : WorkoutStep()

    data class Pyramid(
        val type: String = "Pyramid",
        val repeat: Int,
        val step_duration: Int,
        val start_power: Float,
        val peak_power: Float,
        val end_power: Float
    ) : WorkoutStep()
}
