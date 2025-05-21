package com.example.fitnessapp.model

data class SyncResult(
    val message: String,
    val activities_synced: Int,
    val ftp_estimate: Any? = null,
    val error: String? = null
)