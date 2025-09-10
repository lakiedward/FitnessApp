package com.example.fitnessapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
 
import androidx.lifecycle.asLiveData
import com.example.fitnessapp.viewmodel.StravaViewModel
 
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.lifecycleScope

private const val TAG = "STRAVA_FLOW"  // Tag comun pentru toate log-urile legate de Strava

class StravaCallbackActivity : ComponentActivity() {
    private val stravaViewModel: StravaViewModel by lazy {
        StravaViewModel.getInstance(applicationContext)
    }
    
    private var isProcessingCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[Callback] Activity created")
        Log.d(TAG, "[Callback] Intent data: ${intent.data}")
        Log.d(TAG, "[Callback] Intent action: ${intent.action}")
        
        // Handle the intent that started this activity
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)  // Update the activity's intent
        Log.d(TAG, "[Callback] New intent received")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            Log.e(TAG, "[Callback] No intent received")
            finish()
            return
        }

        if (isProcessingCallback) {
            Log.d(TAG, "[Callback] Already processing callback, ignoring")
            return
        }

        Log.d(TAG, "[Callback] Received intent with URI: ${intent.data}")
        Log.d(TAG, "[Callback] Intent action: ${intent.action}")
        Log.d(TAG, "[Callback] Intent categories: ${intent.categories}")
        Log.d(TAG, "[Callback] Intent flags: ${intent.flags}")

        val code = intent.data?.getQueryParameter("code")
        val status = intent.data?.getQueryParameter("status")
        
        Log.d(TAG, "[Callback] Extracted code: ${code?.take(4)}...${code?.takeLast(4)}")
        Log.d(TAG, "[Callback] Extracted status: $status")
        
        val authManager = com.example.fitnessapp.utils.AuthManager(applicationContext)
        val jwt = authManager.getJwtToken()

        if (code == null) {
            Log.e(TAG, "[Callback] No code in callback URI")
            // Even if no code, if status is success, we might be already connected
            if (status == "success") {
                Log.d(TAG, "[Callback] Status is success, setting connected state immediately")
                isProcessingCallback = true
                // Backend OAuth was successful, set connected state immediately
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000) // Brief delay to allow backend to finish processing
                    stravaViewModel.setConnectedAfterOAuth() // Set connected immediately, fetch athlete data in background
                    delay(500) // Brief delay for state to update
                    finish() // Finish quickly to return to main UI
                }
                return
            } else {
                finish()
                return
            }
        }

        if (jwt.isNullOrEmpty()) {
            // Nu ești logat, redirecționează către MainActivity cu codul Strava
            Log.e(TAG, "[Callback] Not logged in, redirecting to MainActivity")
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("strava_code", code)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(mainIntent)
            finish()
            return
        }

        // Ești logat, procesează codul
        Log.d(TAG, "[Callback] User is logged in, processing Strava code")
        isProcessingCallback = true
        stravaViewModel.stravaState.asLiveData().observe(this) { state ->
            Log.d(TAG, "[Callback] Strava state changed: $state")
            if (state is com.example.fitnessapp.viewmodel.StravaState.Connected || state is com.example.fitnessapp.viewmodel.StravaState.Error) {
                Log.d(TAG, "[Callback] Strava connection completed, finishing activity")
                isProcessingCallback = false
                finish()
            }
        }
        
        // Use coroutine to call suspend function
        lifecycleScope.launch {
            try {
                stravaViewModel.handleAuthCode(code)
            } catch (e: Exception) {
                Log.e(TAG, "[Callback] Error handling auth code", e)
                isProcessingCallback = false
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[Callback] Activity resumed")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "[Callback] Activity destroyed")
        isProcessingCallback = false
    }
} 
