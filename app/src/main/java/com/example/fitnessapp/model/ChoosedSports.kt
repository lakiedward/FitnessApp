package com.example.fitnessapp.model

data class ChoosedSports(
    val cycling: Boolean = false,
    val swimming: Boolean = false,
    val running: Boolean = false
) {
    constructor(selectedSportsList: List<String>) : this(
        cycling = "Cycling" in selectedSportsList,
        swimming = "Swimming" in selectedSportsList,
        running = "Running" in selectedSportsList
    )
}