package com.example.fitnessapp.api

import com.example.fitnessapp.BuildConfig
import com.example.fitnessapp.utils.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        // Redact Authorization header from logs
        redactHeader("Authorization")
    }

    // Interceptor to attach Authorization header automatically if not present
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        // If the request already specifies Authorization, keep it
        if (original.header("Authorization") != null) {
            return@Interceptor chain.proceed(original)
        }

        val token = TokenStore.getToken()
        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else original

        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(300, TimeUnit.SECONDS)  // 5 minutes - increased for batch processing
        .readTimeout(300, TimeUnit.SECONDS)     // 5 minutes - increased for batch processing
        .writeTimeout(300, TimeUnit.SECONDS)    // 5 minutes - increased for batch processing
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
