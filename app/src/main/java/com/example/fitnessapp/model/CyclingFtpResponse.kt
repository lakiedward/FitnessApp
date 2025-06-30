package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName

data class CyclingFtpResponse(
    @SerializedName("cycling_ftp")
    val cyclingFtp: Int, // ex. 250

    @SerializedName("fthr_cycling")
    val fthrCycling: Int // ex. 160
)