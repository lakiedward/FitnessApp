package com.example.fitnessapp.googlefit

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.concurrent.TimeUnit

class FitnessRepository(private val context: Context) {

    fun readSleepData(onResult: (List<String>) -> Unit) {
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(7)

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e("FitnessRepository", "User not signed in")
            onResult(emptyList())
            return
        }

        val request = DataReadRequest.Builder()
            .read(DataType.TYPE_SLEEP_SEGMENT)
            .setTimeRange(start, end, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, account)
            .readData(request)
            .addOnSuccessListener { response ->
                val results = mutableListOf<String>()
                for (dataSet in response.dataSets) {
                    for (dp in dataSet.dataPoints) {
                        val stage = dp.getValue(DataType.TYPE_SLEEP_SEGMENT.fields[0]).asInt()
                        val startTime = dp.getStartTime(TimeUnit.MILLISECONDS)
                        val endTime = dp.getEndTime(TimeUnit.MILLISECONDS)
                        results.add("Stage $stage: ${startTime} - ${endTime}")
                    }
                }
                onResult(results)
            }
            .addOnFailureListener {
                Log.e("FitnessRepository", "Error: ${it.message}")
                onResult(emptyList())
            }
    }
}