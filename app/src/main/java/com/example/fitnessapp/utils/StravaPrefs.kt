package com.example.fitnessapp.utils

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object StravaPrefs {
    private const val PREFS_NAME = "strava_prefs"
    private const val KEY_LAST_SYNC = "last_sync_iso"

    fun setLastSyncNow(context: Context) {
        val iso = Instant.now().toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SYNC, iso)
            .apply()
    }

    fun getLastSyncInstant(context: Context): Instant? {
        val iso = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SYNC, null) ?: return null
        return try {
            Instant.parse(iso)
        } catch (_: Exception) {
            null
        }
    }

    fun getLastSyncFormatted(context: Context): String? {
        val instant = getLastSyncInstant(context) ?: return null
        val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}

