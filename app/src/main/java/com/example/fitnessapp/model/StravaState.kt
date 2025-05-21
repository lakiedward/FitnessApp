package com.example.fitnessapp.model

sealed class StravaState {
    object Disconnected : StravaState()
    object Connected : StravaState()
    object Connecting : StravaState()
    data class Error(val message: String) : StravaState()
} 