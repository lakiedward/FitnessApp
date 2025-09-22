package com.example.fitnessapp.viewmodel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.DetaliiUserCycling
import com.example.fitnessapp.model.RaceModel
import com.example.fitnessapp.model.RacesModelResponse
import com.example.fitnessapp.model.SportsSelectionRequest
import com.example.fitnessapp.model.TrainingDateUpdate
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.model.TrainingPlanCreateRequest
import com.example.fitnessapp.model.User
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.model.UserRaces
import com.example.fitnessapp.model.UserTrainigData
import com.example.fitnessapp.model.UserWeekAvailability
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Response
import com.example.fitnessapp.utils.TokenStore

class AuthViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {

    private val apiService: ApiService = RetrofitClient.retrofit.create(ApiService::class.java)

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

    fun restoreSessionIfPossible() {
        val token = getToken()
        if (!token.isNullOrEmpty()) {
            _authState.value = AuthState.Authenticated(token)
            initUserDataAfterAuth()
        }
    }

    fun signup(email: String, password: String) {
        val user = User(email, password)
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response: Response<Map<String, String>> = apiService.createUser(user)
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "User created successfully. Logging in...")
                    login(email, password)
                } else {
                    _authState.value = AuthState.Error("Signup failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Signup failed: ${e.message}")
            }
        }
    }

    fun login(username: String, password: String) {
        val user = User(username, password)
        // Local validations for clearer, field-related feedback
        if (username.isBlank()) {
            _authState.value = AuthState.Error("Introduceți adresa de email")
            return
        }
        val emailRegex = android.util.Patterns.EMAIL_ADDRESS
        if (!emailRegex.matcher(username).matches()) {
            _authState.value = AuthState.Error("Adresa de email nu este validă")
            return
        }
        if (password.isBlank()) {
            _authState.value = AuthState.Error("Introduceți parola")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Parola trebuie să aibă cel puțin 6 caractere")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response: Response<Map<String, String>> = apiService.login(user)
                if (response.isSuccessful) {
                    val token = response.body()?.get("access_token")
                    if (token != null) {
                        saveToken(token)
                        initUserDataAfterAuth()
                        _authState.value = AuthState.Authenticated(token)
                    } else {
                        _authState.value = AuthState.Error("Serverul nu a returnat token-ul de autentificare")
                    }
                } else {
                    val code = response.code()
                    val raw = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    // Try to extract a message field if backend returns JSON
                    val backendMsg = try {
                        val gson = com.google.gson.Gson()
                        val map = gson.fromJson(raw, Map::class.java) as? Map<*, *>
                        (map?.get("detail") ?: map?.get("message") ?: map?.get("error"))?.toString()
                    } catch (_: Exception) { null }

                    val friendly = when (code) {
                        400, 422 -> backendMsg ?: "Date invalide. Verificați formatul emailului și parola."
                        401 -> "Email sau parolă incorecte."
                        404 -> "Contul nu există sau a fost dezactivat."
                        429 -> "Prea multe încercări. Încercați din nou în câteva minute."
                        in 500..599 -> "Server indisponibil momentan. Încercați mai târziu."
                        else -> backendMsg ?: "Nu am putut autentifica (cod $code)."
                    }
                    _authState.value = AuthState.Error(friendly)
                }
            } catch (e: Exception) {
                val friendly = when (e) {
                    is java.net.UnknownHostException -> "Nu există conexiune la internet."
                    is java.net.SocketTimeoutException -> "Conexiune lentă. Încercați din nou."
                    else -> "Eroare neașteptată: ${e.message}"
                }
                _authState.value = AuthState.Error(friendly)
            }
        }
    }

    fun saveUserSports(sports: SportsSelectionRequest) {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.selectUserSports("Bearer $token", sports)
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "User sports updated successfully")
                    _authState.value = AuthState.Authenticated(token)
                } else {
                    Log.e("AuthViewModel", "Failed to update user sports: ${response.message()}")
                    _authState.value = AuthState.Error("Failed to update user sports: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to update user sports: ${e.message}")
                _authState.value = AuthState.Error("Failed to update user sports: ${e.message}")
            }
        }
    }

    fun updateTrainingPlanDate(planId: Int, trainingDateUpdate: TrainingDateUpdate) {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }

        viewModelScope.launch {
            try {
                val response = apiService.updateTrainingPlanDate(planId, "Bearer $token", trainingDateUpdate)
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Training plan date updated successfully")
                    delay(1000)
                    getTrainingPlans()
                } else {
                    Log.e("AuthViewModel", "Failed to update training plan date: ${response.message()}")
                    _authState.value = AuthState.Error("Failed to update training plan date: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating training plan date: ${e.message}")
                _authState.value = AuthState.Error("Error updating training plan date: ${e.message}")
            }
        }
    }

    suspend fun deleteTrainingPlanEntry(planId: Int): Boolean = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext false
        try {
            val response = apiService.deleteTrainingPlanEntry(planId, "Bearer $token")
            if (response.isSuccessful) {
                getTrainingPlans()
                true
            } else {
                val message = "Failed to delete training plan: ${response.message()}"
                Log.e("AuthViewModel", message)
                false
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error deleting training plan: ${e.message}")
            false
        }
    }
    fun quickCreateTrainingPlan(
        request: TrainingPlanCreateRequest,
        onResult: (Boolean, String?) -> Unit
    ) {
        val token = getToken()
        if (token == null) {
            val message = "Token is missing."
            Log.e("AuthViewModel", message)
            _authState.value = AuthState.Error(message)
            onResult(false, message)
            return
        }

        viewModelScope.launch {
            try {
                val response = apiService.createTrainingPlan("Bearer $token", request)
                if (response.isSuccessful) {
                    getTrainingPlans()
                    onResult(true, null)
                } else {
                    val message = "Failed to create training plan: ${response.message()}"
                    Log.e("AuthViewModel", message)
                    onResult(false, message)
                }
            } catch (e: Exception) {
                val message = "Error creating training plan: ${e.message}"
                Log.e("AuthViewModel", message)
                onResult(false, message)
            }
        }
    }

    fun generateTrainingPlanBySport(raceDate: String) {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing.")
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.generateTrainingPlanBySport("Bearer $token", mapOf("race_date" to raceDate))
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Training plan generated based on selected sports.")
                    delay(3000)
                    getTrainingPlans()
                } else {
                    Log.e("AuthViewModel", "Failed to generate training plan: ${response.message()}")
                    _authState.value = AuthState.Error("Failed to generate training plan: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error generating training plan: ${e.message}")
                _authState.value = AuthState.Error("Error generating training plan: ${e.message}")
            }
        }
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
        viewModelScope.launch {
            try {
                val response = apiService.addUserDetails("Bearer $token", details)
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "User details updated successfully")
                    _authState.value = AuthState.Authenticated(token)
                } else {
                    Log.e("AuthViewModel", "Failed to update user details: ${response.message()}")
                    _authState.value = AuthState.Error("Failed to update user details: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to update user details: ${e.message}")
            }
        }
    }

    fun addWeekAvailability(
        availabilityList: List<UserWeekAvailability>,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val token = getToken()
        if (token == null) {
            onError?.invoke("Token is missing")
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.addWeekAvailability("Bearer $token", availabilityList)
                if (response.isSuccessful) {
                    onSuccess?.invoke()
                } else {
                    onError?.invoke("Failed to save availability: ${response.message()}")
                }
            } catch (e: Exception) {
                onError?.invoke("Error saving availability: ${e.message}")
            }
        }
    }

    fun addOrUpdateTrainingData(ftp: Int) {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }
        val cyclingDetails = DetaliiUserCycling(ftp)
        viewModelScope.launch {
            try {
                val response = apiService.addOrUpdateCyclingData("Bearer $token", cyclingDetails)
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Cycling details updated successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to update cycling details: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to update cycling details: ${e.message}")
            }
        }
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

    fun addRace(race_date: String, race_name: String = "") {
        val token = getToken()
        if (token == null) {
            Log.e("AuthViewModel", "Token is missing. Please log in again.")
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }
        val raceData = UserRaces(race_date, race_name)
        viewModelScope.launch {
            try {
                val response = apiService.addRace("Bearer $token", raceData)
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "race details updated successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to update race details: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to update race details: ${e.message}")
            }
        }
    }

    fun getTrainingPlans(retry: Boolean = true) {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.getTrainingPlan("Bearer $token")
                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()
                    if (data.isEmpty() && retry) {
                        delay(5000)
                        getTrainingPlans(retry = false)
                    } else {
                        _trainingPlan.postValue(data)
                    }
                } else {
                    _authState.value = AuthState.Error("Failed to fetch training plans: ${response.message()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error fetching training plans: ${e.message}")
            }
        }
    }

    fun getRaces() {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }
        viewModelScope.launch {
            try {
                val response: Response<RacesModelResponse> = apiService.getRaces("Bearer $token")
                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    Log.d("AuthViewModel", "Training Plans fetched: $data")
                    _races.postValue(data)
                } else {
                    Log.e("AuthViewModel", "Failed to fetch training plans: ${response.message()}")
                    _authState.value = AuthState.Error("Failed to fetch training plans: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching training plans: ${e.message}")
                _authState.value = AuthState.Error("Error fetching training plans: ${e.message}")
            }
        }
    }

    fun getUserTrainingData() {
        val token = getToken()
        if (token == null) {
            _authState.value = AuthState.Error("Token is missing. Please log in again.")
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.getUserTrainingData("Bearer $token")
                if (response.isSuccessful) {
                    _userTrainingData.value = response.body()
                } else {
                    Log.e("AuthViewModel", "Failed to fetch user training data: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching user training data: ${e.message}")
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun getToken(): String? {
        val token = sharedPreferences.getString("jwt_token", null)
        // Keep TokenStore in sync; do not log token contents
        if (!token.isNullOrEmpty()) {
            TokenStore.setToken(token)
        }
        return token
    }

    private fun saveToken(token: String) {
        sharedPreferences.edit().putString("jwt_token", token).apply()
        TokenStore.setToken(token)
        Log.d("AuthViewModel", "Token saved")
    }

    fun logout() {
        sharedPreferences.edit().remove("jwt_token").apply()
        TokenStore.clear()
        Log.d("AuthViewModel", "Token cleared - user logged out")
        _authState.value = AuthState.Unauthenticated
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val jwtToken: String) : AuthState()
    data class Error(val message: String) : AuthState()
}








