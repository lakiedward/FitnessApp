package com.example.fitnessapp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.api.User
import com.example.fitnessapp.api.UpdateGender
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthViewModel : ViewModel() {

    private val apiService = RetrofitClient.retrofit.create(ApiService::class.java)
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun signup(username: String, password: String) {
        val user = User(username, password)
        _authState.value = AuthState.Loading

        apiService.createUser(user).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    _authState.value = AuthState.Authenticated
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
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("Login failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                _authState.value = AuthState.Error("Login failed: ${t.message}")
            }
        })
    }

    fun updateGender(username: String, gender: String) {
        val updateGender = UpdateGender(username, gender)

        apiService.updateGender(updateGender).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Gender updated successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to update gender: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("AuthViewModel", "Failed to update gender: ${t.message}")
            }
        })
    }
}

// Define AuthState to handle different states of authentication
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
