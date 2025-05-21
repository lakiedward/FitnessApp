package com.example.fitnessapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.asLiveData
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "STRAVA_FLOW"  // Tag comun pentru toate log-urile legate de Strava

class StravaCallbackActivity : ComponentActivity() {
    private val stravaViewModel: StravaViewModel by viewModels {
        StravaViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[Callback] Activity created")
        
        val code = intent.data?.getQueryParameter("code")
        val authManager = com.example.fitnessapp.utils.AuthManager(applicationContext)
        val jwt = authManager.getJwtToken()

        if (code == null) {
            Log.e(TAG, "[Callback] No code in callback URI")
            finish()
            return
        }

        if (jwt.isNullOrEmpty()) {
            // Nu ești logat, redirecționează către MainActivity cu codul Strava
            Log.e(TAG, "[Callback] Not logged in, redirecting to MainActivity")
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("strava_code", code)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
            return
        }

        // Ești logat, procesează codul
        stravaViewModel.stravaState.asLiveData().observe(this) { state ->
            if (state is com.example.fitnessapp.viewmodel.StravaState.Connected || state is com.example.fitnessapp.viewmodel.StravaState.Error) {
                finish()
            }
        }
        stravaViewModel.handleAuthCode(code)
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

        Log.d(TAG, "[Callback] Received intent with URI: ${intent.data}")
        Log.d(TAG, "[Callback] Intent action: ${intent.action}")
        Log.d(TAG, "[Callback] Intent categories: ${intent.categories}")
        Log.d(TAG, "[Callback] Intent flags: ${intent.flags}")

        val code = intent.data?.getQueryParameter("code")
        if (code != null) {
            Log.d(TAG, "[Callback] Extracted authorization code: ${code.take(4)}...${code.takeLast(4)}")
            Log.d(TAG, "[Callback] Starting Strava connection process")
            CoroutineScope(Dispatchers.Main).launch {
                stravaViewModel.handleAuthCode(code)
                finish()
            }
        } else {
            Log.e(TAG, "[Callback] No authorization code received")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[Callback] Activity resumed")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "[Callback] Activity destroyed")
    }
} 