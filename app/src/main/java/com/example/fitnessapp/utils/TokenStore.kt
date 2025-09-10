package com.example.fitnessapp.utils

/**
 * In-memory holder for the JWT used by the OkHttp auth interceptor.
 * Keep it synchronized with SharedPreferences in AuthViewModel.
 */
object TokenStore {
    @Volatile
    private var token: String? = null

    fun setToken(value: String?) {
        token = value
    }

    fun getToken(): String? = token

    fun clear() {
        token = null
    }
}

