package com.example.fitnessapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.fitnessapp.model.StravaToken
import com.google.gson.Gson

class AuthManager(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)
    }

    fun getJwtToken(): String? {
        val token = prefs.getString("jwt_token", null)
        Log.d("JWT_DEBUG", "[AuthManager] getJwtToken() called")
        Log.d("JWT_DEBUG", "[AuthManager] Token exists: ${!token.isNullOrEmpty()}")
        Log.d("JWT_DEBUG", "[AuthManager] Token length: ${token?.length}")
        Log.d("JWT_DEBUG", "[AuthManager] Token first 10 chars: ${token?.take(10)}...")
        return token
    }

    fun getUserId(): Int? {
        return prefs.getInt("user_id", -1).takeIf { it != -1 }
    }

    fun getStravaToken(): StravaToken? {
        val tokenJson = prefs.getString("strava_token", null) ?: return null
        return try {
            Gson().fromJson(tokenJson, StravaToken::class.java)
        } catch (e: Exception) {
            Log.e("AuthManager", "Error parsing Strava token", e)
            null
        }
    }

    fun saveStravaToken(token: StravaToken) {
        prefs.edit().putString("strava_token", Gson().toJson(token)).apply()
    }

    fun clearStravaToken() {
        prefs.edit().remove("strava_token").apply()
    }

    fun clearAllStravaData() {
        Log.d("AuthManager", "Clearing all Strava data from SharedPreferences")
        prefs.edit()
            .remove("strava_token")
            .apply()
        Log.d("AuthManager", "All Strava data cleared")
    }

    fun saveJwtToken(token: String) {
        Log.d("JWT_DEBUG", "[AuthManager] saveJwtToken() called")
        Log.d("JWT_DEBUG", "[AuthManager] Token length: ${token.length}")
        Log.d("JWT_DEBUG", "[AuthManager] Token first 10 chars: ${token.take(10)}...")
        prefs.edit().putString("jwt_token", token).apply()
        // Verify the token was saved
        val savedToken = prefs.getString("jwt_token", null)
        Log.d("JWT_DEBUG", "[AuthManager] Token saved successfully: ${savedToken != null}")
        Log.d("JWT_DEBUG", "[AuthManager] Saved token length: ${savedToken?.length}")
    }
} 