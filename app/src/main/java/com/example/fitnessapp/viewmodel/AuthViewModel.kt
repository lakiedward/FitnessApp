package com.example.fitnessapp.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.RaceModel
import com.example.fitnessapp.model.RacesModelResponse
import com.example.fitnessapp.model.SportsSelectionRequest
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.model.User
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.model.UserRaces
import com.example.fitnessapp.model.UserTrainigData
import com.example.fitnessapp.model.UserWeekAvailability
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

class AuthViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {


    private val apiService = RetrofitClient.retrofit.create(ApiService::class.java)
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _trainingPlan = MutableLiveData<List<TrainingPlan>>()
    val trainingPlan: LiveData<List<TrainingPlan>> get() = _trainingPlan

    private val _races = MutableLiveData<List<RaceModel>>()
    val races: LiveData<List<RaceModel>> get() = _races

    private val _userTrainingData = MutableLiveData<UserTrainigData?>()
    val userTrainingData: LiveData<UserTrainigData?> get() = _userTrainingData

    fun initUserDataAfterAuth() {
        val token = getToken()
        if (token != null) {
            getTrainingPlans()
            getRaces()
            getUserTrainingData()
        } else {
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
        }
    }

    fun signup(email: String, password: String) {
        val user = User(email, password)
        _authState.value = AuthState.Loading

        apiService.createUser(user).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "User created successfully. Logging in...")
                    // Autentificare automată după înregistrare
                    login(email, password)
                } else {
                    _authState.value = AuthState.Error("Signup failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                _authState.value = AuthState.Error("Signup failed: ${t.message}")
            }
        })
    }


    fun login(username: String, password: String) {
        val user = User(username, password)
        _authState.value = AuthState.Loading

        apiService.login(user).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val token = response.body()?.get("access_token")
                    if (token != null) {
                        saveToken(token)
                        initUserDataAfterAuth() // 🔁 Apelează aici
                        _authState.value = AuthState.Authenticated(token)
                    } else {
                        _authState.value = AuthState.Error("Login failed: Token missing")
                    }
                } else {
                    _authState.value = AuthState.Error("Login failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                _authState.value = AuthState.Error("Login failed: ${t.message}")
            }
        })
    }

    fun generateTrainingPlanBySport(raceDate: String) {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing.")
            return
        }

        apiService.generateTrainingPlanBySport("Bearer $token", mapOf("race_date" to raceDate))
            .enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        Log.d("AuthViewModel", "Training plan generated based on selected sports.")

                        // ✅ Adaugă aici reîncărcarea planurilor după un scurt delay
                        viewModelScope.launch {
                            delay(3000) // Așteaptă puțin ca planul să fie salvat în DB
                            getTrainingPlans()
                        }

                    } else {
                        Log.e("AuthViewModel", "Failed to generate training plan: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Log.e("AuthViewModel", "Error generating training plan: ${t.message}")
                }
            })
    }


    fun addUserDetails(varsta: Int, inaltime: Float, greutate: Float, gender: String, discipline: String) {
        val token = getToken()
        _authState.value = AuthState.Loading

        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        val details = UserDetalis(varsta, inaltime, greutate, gender, discipline)
        apiService.addUserDetails("Bearer $token", details).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "User details updated successfully")
                    _authState.value = AuthState.Authenticated(token) // ✅ asta era esențial
                } else {
                    Log.e("AuthViewModel", "Failed to update user details: ${response.message()}")
                    _authState.value =
                        AuthState.Error("Failed to update user details: ${response.message()}")

                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("AuthViewModel", "Failed to update user details: ${t.message}")
            }
        })
    }

    fun addWeekAvailability(availabilityList: List<UserWeekAvailability>) {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        apiService.addWeekAvailability("Bearer $token", availabilityList).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Week availability updated successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to update week availability: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("AuthViewModel", "Failed to update week availability: ${t.message}")
            }
        })
    }

    fun addOrUpdateTrainingData(ftp: Int, max_bpm: Int) {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        val cyclingDetails = UserTrainigData(ftp, max_bpm)
        apiService.addOrUpdateTrainingData("Bearer $token", cyclingDetails).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Cycling details updated successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to update cycling details: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("AuthViewModel", "Failed to update cycling details: ${t.message}")
            }
        })

    }

    fun saveRunningData(best5km: Int?, best10km: Int?) {
        viewModelScope.launch {
            try {
                val token = getToken() ?: return@launch
                val request = mapOf(
                    "best_5km" to best5km,
                    "best_10km" to best10km
                )
                val response = apiService.saveRunningData("Bearer $token", request)
                if (!response.isSuccessful) {
                    Log.e("AuthViewModel", "Failed to save running data: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception saving running data: ${e.localizedMessage}")
            }
        }
    }

    fun addRace(race_date: String,  race_name: String = "") {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        val raceData = UserRaces(race_date, race_name)
        apiService.addRace("Bearer $token", raceData).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "race details updated successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to update race details: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("AuthViewModel", "Failed to update race details: ${t.message}")
            }
        })
    }

//    fun genearteCyclinngPlan(duration_weeks: String) {
//        val token = getToken()
//        if (token == null) {
//            Log.e("AuthViewModel", "Token is missing. Please log in again.")
//            _authState.value = AuthState.Error("Token is missing. Please log in again.")
//            return
//        }
//
//        val trainingPlanGenerate = TrainingPlanGenerate(duration_weeks)
//        apiService.generateCyclingPlan("Bearer $token", trainingPlanGenerate).enqueue(object : Callback<Map<String, String>> {
//            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
//                if (response.isSuccessful) {
//                    Log.d("AuthViewModel", "Training Plan updated successfully")
//                } else {
//                    Log.e("AuthViewModel", "Failed to update Training Plan: ${response.message()}")
//                }
//            }
//
//            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
//                Log.e("AuthViewModel", "Failed to update rTraining Plan: ${t.message}")
//            }
//        })
//    }

    fun saveUserSports(sports: List<String>) {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing")
            return
        }

        val body = SportsSelectionRequest(sports)
        Log.d("AuthViewModel", "Trimitem sporturile: $body")

        apiService.selectUserSports("Bearer $token", body)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("AuthViewModel", "User sports saved successfully.")
                        _authState.value = AuthState.Authenticated(token)
                    } else {
                        Log.e("AuthViewModel", "Failed to save sports: ${response.code()} ${response.message()}")
                        _authState.value =
                            AuthState.Error("Failed to save sports: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("AuthViewModel", "Error saving sports: ${t.message}")
                    _authState.value = AuthState.Error("Exception: ${t.message}")
                }
            })
    }




    fun getTrainingPlans(retry: Boolean = true) {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        apiService.getTrainingPlan("Bearer $token").enqueue(object : Callback<List<TrainingPlan>> {
            override fun onResponse(call: Call<List<TrainingPlan>>, response: Response<List<TrainingPlan>>) {
                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()
                    if (data.isEmpty() && retry) {
                        // Încearcă din nou după 5 secunde
                        viewModelScope.launch {
                            delay(5000)
                            getTrainingPlans(retry = false) // doar o singură reîncercare
                        }
                    } else {
                        _trainingPlan.postValue(data)
                    }
                } else {
                    _authState.value =
                        AuthState.Error("Failed to fetch training plans: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<TrainingPlan>>, t: Throwable) {
                _authState.value = AuthState.Error("Error fetching training plans: ${t.message}")
            }
        })
    }



    fun getRaces() {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        apiService.getRaces("Bearer $token").enqueue(object : Callback<RacesModelResponse> {
            override fun onResponse(call: Call<RacesModelResponse>, response: Response<RacesModelResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    Log.d("AuthViewModel", "Training Plans fetched: $data")
                    _races.postValue(data) // Actualizează datele din LiveData
                } else {
                    Log.e("AuthViewModel", "Failed to fetch training plans: ${response.message()}")
                    _authState.value =
                        AuthState.Error("Failed to fetch training plans: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RacesModelResponse>, t: Throwable) {
                Log.e("AuthViewModel", "Error fetching training plans: ${t.message}")
                _authState.value = AuthState.Error("Error fetching training plans: ${t.message}")
            }
        })
    }

    fun getUserTrainingData() {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        apiService.getUserTrainingData("Bearer $token").enqueue(object : Callback<UserTrainigData> {
            override fun onResponse(call: Call<UserTrainigData>, response: Response<UserTrainigData>) {
                if (response.isSuccessful) {
                    _userTrainingData.value = response.body()
                } else {
                    Log.e("AuthViewModel", "Failed to fetch user training data: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserTrainigData>, t: Throwable) {
                Log.e("AuthViewModel", "Error fetching user training data: ${t.message}")
            }
        })
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun getToken(): String? {
        val token = sharedPreferences.getString("jwt_token", null)
        Log.d("AuthViewModel", "Retrieved token: $token")
        return token
    }

    private fun saveToken(token: String) {
        sharedPreferences.edit().putString("jwt_token", token).apply()
        Log.d("AuthViewModel", "Token saved successfully: $token")
    }

}



// Define AuthState to handle different states of authentication
sealed class AuthState {
    object Idle : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val jwtToken: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
