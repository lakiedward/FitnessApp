package com.example.fitnessapp.api

import com.example.fitnessapp.BuildConfig

@Deprecated("Use BuildConfig.BASE_URL directly")
object ApiConfig {
    // Backward compatibility: delegate to BuildConfig
    val BASE_URL: String = BuildConfig.BASE_URL
}
