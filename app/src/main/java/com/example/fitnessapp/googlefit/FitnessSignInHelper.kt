package com.example.fitnessapp.googlefit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

object FitnessSignInHelper {

    const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001

    val fitnessOptions: FitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_WORKOUT_EXERCISE, FitnessOptions.ACCESS_READ)
        .build()

    fun hasPermissions(context: Context): Boolean {
        val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
        return GoogleSignIn.hasPermissions(account, fitnessOptions)
    }

    fun requestPermissions(activity: Activity) {
        val account = GoogleSignIn.getAccountForExtension(activity, fitnessOptions)
        GoogleSignIn.requestPermissions(
            activity,
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
            account,
            fitnessOptions
        )
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("GoogleFit", "Google Fit permissions granted")
            } else {
                Log.e("GoogleFit", "Google Fit permissions denied")
            }
        }
    }
}


