package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName

data class SwimmingPaceResponse(
    @SerializedName("pace_100m")
    val pace100m: String, // ex. "1:20"

    @SerializedName("fthr_swimming")
    val fthrSwimming: Int // ex. 150
)