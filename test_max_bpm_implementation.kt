package com.example.fitnessapp.test

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import kotlinx.coroutines.runBlocking

/**
 * Test script to verify the max BPM implementation
 * This tests:
 * 1. The new GET /strava/max-bpm endpoint
 * 2. The StravaViewModel.getMaxBpm() method
 * 3. The updated UI components that use max BPM instead of FTHR
 */

fun testMaxBpmEndpoint(context: Context) {
    println("[DEBUG_LOG] Testing max BPM implementation...")
    
    val stravaViewModel = StravaViewModel(context)
    
    runBlocking {
        try {
            println("[DEBUG_LOG] Calling getMaxBpm() method...")
            val maxBpmData = stravaViewModel.getMaxBpm()
            
            if (maxBpmData.isNotEmpty()) {
                val maxBpm = maxBpmData["max_bpm"]
                val source = maxBpmData["source"]
                val retrievedAt = maxBpmData["retrieved_at"]
                
                println("[DEBUG_LOG] Max BPM API call successful!")
                println("[DEBUG_LOG] Max BPM: $maxBpm")
                println("[DEBUG_LOG] Source: $source")
                println("[DEBUG_LOG] Retrieved at: $retrievedAt")
                
                // Verify response format matches expected structure
                if (maxBpm != null && source != null && retrievedAt != null) {
                    println("[DEBUG_LOG] ✓ Response format is correct")
                } else {
                    println("[DEBUG_LOG] ✗ Response format is incorrect")
                }
            } else {
                println("[DEBUG_LOG] ✗ Max BPM API call returned empty data")
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] ✗ Max BPM API call failed: ${e.message}")
        }
    }
}

@Composable
fun TestMaxBpmInUI(context: Context) {
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))
    var maxBpm by remember { mutableStateOf<Float?>(null) }
    var testResult by remember { mutableStateOf("Testing...") }
    
    LaunchedEffect(Unit) {
        try {
            println("[DEBUG_LOG] Testing max BPM in UI component...")
            val maxBpmData = stravaViewModel.getMaxBpm()
            val maxBpmValue = maxBpmData["max_bpm"] as? Number
            maxBpm = maxBpmValue?.toFloat()
            
            if (maxBpm != null) {
                testResult = "✓ Max BPM loaded successfully: ${maxBpm} bpm"
                println("[DEBUG_LOG] $testResult")
            } else {
                testResult = "✗ Failed to load max BPM"
                println("[DEBUG_LOG] $testResult")
            }
        } catch (e: Exception) {
            testResult = "✗ Error loading max BPM: ${e.message}"
            println("[DEBUG_LOG] $testResult")
        }
    }
}

/**
 * Expected API Response Format:
 * {
 *   "max_bpm": 185,
 *   "source": "stored_value",
 *   "retrieved_at": "2024-01-15T10:30:00Z"
 * }
 * 
 * Expected Error Responses:
 * - 404: No max BPM data found for user
 * - 401: Unauthorized (invalid/missing token)
 * - 500: Server error
 */