package com.example.fitnessapp

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.pages.AddAgeScreen
import com.example.fitnessapp.pages.AddEmailScreen
import com.example.fitnessapp.pages.ChooseDisciplineScreen
import com.example.fitnessapp.pages.ChooseSportsScreen
import com.example.fitnessapp.pages.CyclingDataInsertScreen
import com.example.fitnessapp.pages.GenderSelectionScreen
import com.example.fitnessapp.pages.HomeScreen
import com.example.fitnessapp.pages.InfiniteCalendarPage
import com.example.fitnessapp.pages.LoginScreen
import com.example.fitnessapp.pages.PhysicalActivityLevelScreen
import com.example.fitnessapp.pages.SeasonScreen
import com.example.fitnessapp.pages.WorkoutScreen


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obține SharedPreferences
        val sharedPreferences = getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)
        // Creează AuthViewModel
        val authViewModel = AuthViewModel(sharedPreferences)
        //authViewModel.getTrainingPlans()
       //authViewModel.getRaces()


        setContent {
            FitnessAppTheme {
                val navController = rememberNavController()
                val userDetalis = remember { mutableStateOf(UserDetalis(0, 0f, 0f, "", "", "")) }
                AppNavigation(navController, authViewModel, userDetalis)
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(navController: NavHostController, authViewModel: AuthViewModel, userDetalis: MutableState<UserDetalis>) {
    NavHost(navController = navController, startDestination = "login_screen") {
        composable("login_screen") {
            LoginScreen(navController, authViewModel)
        }
        composable("gender_selection_screen") {
            GenderSelectionScreen(navController, userDetalis)
        }
        composable("add_age_screen") {
            AddAgeScreen(navController, userDetalis)
        }
        composable("physical_activity_level_screen") {
            PhysicalActivityLevelScreen(navController, userDetalis)
        }
        composable("choose_sports") {
            ChooseSportsScreen(navController, authViewModel)
        }
        composable("choose_discipline") {
            ChooseDisciplineScreen(navController, authViewModel, userDetalis)
        }
        composable("cycling_data_insert") {
            CyclingDataInsertScreen(navController, authViewModel)
        }
        composable("enter_add_email") {
            AddEmailScreen(navController, authViewModel)
        }
        composable("home_screen"){
            HomeScreen(navController, authViewModel)
        }
        composable("calendar_screen"){
            InfiniteCalendarPage(navController, authViewModel)
        }
        composable("season_screen") {
            SeasonScreen(navController, authViewModel)
        }
        composable("workout_screen") {
            WorkoutScreen(navController)
        }

    }
}



