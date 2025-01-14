package com.example.fitnessapp

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.RaceModel
import com.example.fitnessapp.model.RacesModelResponse
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.model.TrainingPlanGenerate
import com.example.fitnessapp.model.TrainingPlanResponse
import com.example.fitnessapp.model.User
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.model.UserRaces
import com.example.fitnessapp.model.UserTrainigData
import com.example.fitnessapp.model.UserWeekAvailability
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {

    private val apiService = RetrofitClient.retrofit.create(ApiService::class.java)
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _trainingPlan = MutableLiveData<List<TrainingPlan>>()
    val trainingPlan: LiveData<List<TrainingPlan>> get() = _trainingPlan

    private val _races = MutableLiveData<List<RaceModel>>()
    val races: LiveData<List<RaceModel>> get() = _races

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
                        saveToken(token) // Salvează token-ul
                        _authState.value = AuthState.Authenticated
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



    fun addUserDetails(varsta: Int, inaltime: Float, greutate: Float, fitnessLevel: String, gender: String, discipline: String) {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        val details = UserDetalis(varsta, inaltime, greutate, fitnessLevel, gender, discipline)
        apiService.addUserDetails("Bearer $token", details).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "User details updated successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to update user details: ${response.message()}")
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

    fun genearteTrainingPlan(duration_weeks: String) {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        val trainingPlanGenerate = TrainingPlanGenerate(duration_weeks)
        apiService.generateTrainingPlan("Bearer $token", trainingPlanGenerate).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Training Plan updated successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to update Training Plan: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("AuthViewModel", "Failed to update rTraining Plan: ${t.message}")
            }
        })
    }

    fun getTrainingPlans() {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        apiService.getTrainingPlan("Bearer $token").enqueue(object : Callback<TrainingPlanResponse> {
            override fun onResponse(call: Call<TrainingPlanResponse>, response: Response<TrainingPlanResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    Log.d("AuthViewModel", "Training Plans fetched: $data")
                    _trainingPlan.postValue(data) // Actualizează datele din LiveData
                } else {
                    Log.e("AuthViewModel", "Failed to fetch training plans: ${response.message()}")
                    _authState.value = AuthState.Error("Failed to fetch training plans: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<TrainingPlanResponse>, t: Throwable) {
                Log.e("AuthViewModel", "Error fetching training plans: ${t.message}")
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
                    _authState.value = AuthState.Error("Failed to fetch training plans: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RacesModelResponse>, t: Throwable) {
                Log.e("AuthViewModel", "Error fetching training plans: ${t.message}")
                _authState.value = AuthState.Error("Error fetching training plans: ${t.message}")
            }
        })
    }




    private fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
        Log.d("AuthViewModel", "Token saved successfully: $token")
    }


    private fun getToken(): String? {
        val token = sharedPreferences.getString("auth_token", null)
        Log.d("AuthViewModel", "Retrieved token: $token")
        return token
    }


//    fun clearToken() {
//        sharedPreferences.edit().remove("auth_token").apply()
//        _authState.value = AuthState.Unauthenticated
//    }
}

// Define AuthState to handle different states of authentication
sealed class AuthState {
    data object Authenticated : AuthState()
//    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
