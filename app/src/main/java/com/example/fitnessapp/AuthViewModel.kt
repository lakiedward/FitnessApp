package com.example.fitnessapp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fitnessapp.pages.convertMillisToDate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    // Check if the user is authenticated
    fun checkAuthStatus() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    // Log in the user with email and password
    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    // Sign up the user and store name and age in Firestore
    fun signup(email: String, password: String, name: String) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            _authState.value = AuthState.Error("Email, password, or name can't be empty")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the user ID and store additional information in Firestore
                    val user = auth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener
                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "gender" to null,
                        "age" to null,
                        "height" to null,
                        "weight" to null,
                        "fitnessLevel" to null,
                        "selectedSports" to emptyList<String>(),
                        "schedule" to emptyList<Map<String, Any>>()
                    )

                    // Store the user data in Firestore
                    firestore.collection("users")
                        .document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            _authState.value = AuthState.Authenticated
                        }
                        .addOnFailureListener { e ->
                            _authState.value = AuthState.Error("Failed to save user details: ${e.message}")
                        }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun updateGender(gender: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .update("gender", gender)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Gender updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Failed to update gender: ${e.message}")
            }
    }

    fun updateFitnessLevel(fitnessLevel: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .update("fitnessLevel", fitnessLevel)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Fitness Level updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Failed to update fitness level: ${e.message}")
            }
    }

    fun saveSportsAndSchedule(selectedSports: List<String>, schedule: List<Map<String, Any>>) {
        val userId = auth.currentUser?.uid ?: return
        val data = mapOf(
            "selectedSports" to selectedSports,
            "schedule" to schedule
        )

        firestore.collection("users").document(userId)
            .update(data)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Sports and schedule saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Failed to save data: ${e.message}")
            }
    }


    fun updatePhysicalAttributes(age: Int, weight: Double, height: Double) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "age" to age,
            "weight" to weight,
            "height" to height
        )
        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Physical attributes updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Failed to update physical attributes: ${e.message}")
            }
    }

    fun saveCyclingData(cyclingFtp: String, maxBpm: String, selectedRaceType: String, raceDate: String) {
        val userId = auth.currentUser?.uid ?: return
        val data = mapOf(
            "cyclingFtp" to cyclingFtp,
            "maxBpm" to maxBpm,
            "selectedRaceType" to selectedRaceType,
            "raceDate" to raceDate
        )

        firestore.collection("users").document(userId)
            .update(data)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Cycling data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Failed to save cycling data: ${e.message}")
            }
    }


    // Sign out the user
    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}

// Sealed class for different auth states
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
