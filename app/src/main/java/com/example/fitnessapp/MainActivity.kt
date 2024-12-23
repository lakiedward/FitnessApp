package com.example.fitnessapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.api.ApiConfig
import com.example.fitnessapp.AuthViewModel
import com.example.fitnessapp.pages.AddAgeScreen
import com.example.fitnessapp.pages.AddEmailScreen
import com.example.fitnessapp.pages.CheckYourEmailScreen
import com.example.fitnessapp.pages.ChooseSportsScreen
import com.example.fitnessapp.pages.CyclingDataInsertScreen
import com.example.fitnessapp.pages.ForgotPasswordScreen
import com.example.fitnessapp.pages.GenderSelectionScreen
import com.example.fitnessapp.pages.LoginScreen
import com.example.fitnessapp.pages.PhysicalActivityLevelScreen
import com.example.fitnessapp.pages.SetNewPasswordScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authViewModel : AuthViewModel by viewModels()
        setContent {
            //ApiConfig.initialize(applicationContext)

            // Testare cheie API
            //Log.d("API_KEY", "Cheia API este: ${ApiConfig.apiKey}")
            FitnessAppTheme {
                val navController = rememberNavController()
                AppNavigation(navController, authViewModel)
                //LoginScreen()
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, authViewModel: AuthViewModel = AuthViewModel()) {
    NavHost(navController = navController, startDestination = "login_screen") {
        composable("login_screen") {
            LoginScreen(navController, authViewModel)
        }
        composable("gender_selection_screen") {
            GenderSelectionScreen(navController, authViewModel)
        }
        composable("add_age_screen") {
            AddAgeScreen(navController,authViewModel)
        }
        composable("physical_activity_level_screen") {
            PhysicalActivityLevelScreen(navController, authViewModel)
        }
        composable("choose_sports") {
            ChooseSportsScreen(navController, authViewModel)
        }
        composable("cycling_data_insert") {
            CyclingDataInsertScreen(navController, authViewModel)
        }
        composable("enter_add_email") {
            AddEmailScreen(navController, authViewModel)
        }
        composable("enter_forgot_password") {
            ForgotPasswordScreen(navController)
        }
        composable("enter_check_your_email") {
            CheckYourEmailScreen(navController)
        }
        composable("enter_set_new_password") {
            SetNewPasswordScreen(navController)
        }
    }
}


