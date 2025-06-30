package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName

data class RunningFtpResponse(
    @SerializedName("running_ftp")
    val runningFtp: String, // ex. "5:00" (pace in min:sec/km format)

    @SerializedName("fthr_running")
    val fthrRunning: Int // ex. 170
)
