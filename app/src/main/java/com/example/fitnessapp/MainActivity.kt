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
import com.example.fitnessapp.model.ChoosedSports
import com.example.fitnessapp.pages.signup.AddAgeScreen
import com.example.fitnessapp.pages.signup.AddEmailScreen
import com.example.fitnessapp.pages.signup.ChooseDisciplineScreen
import com.example.fitnessapp.pages.signup.ChooseSportsScreen
import com.example.fitnessapp.pages.signup.CyclingDataInsertScreen
import com.example.fitnessapp.pages.signup.GenderSelectionScreen
import com.example.fitnessapp.pages.home.HomeScreen
import com.example.fitnessapp.pages.home.InfiniteCalendarPage
import com.example.fitnessapp.pages.LoginScreen
import com.example.fitnessapp.pages.signup.PhysicalActivityLevelScreen
import com.example.fitnessapp.pages.signup.RunningDataInsertScreen
import com.example.fitnessapp.pages.home.SeasonScreen
import com.example.fitnessapp.pages.home.WorkoutScreen
import com.example.fitnessapp.pages.more.MoreScreen
import com.example.fitnessapp.pages.signup.PlanLengthScreen
import com.example.fitnessapp.pages.loading.LoadingScreen


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)
        val authViewModel = AuthViewModel(sharedPreferences)

        setContent {
            FitnessAppTheme(darkTheme = false) {
            val navController = rememberNavController()
                val userDetalis = remember { mutableStateOf(UserDetalis(0, 0f, 0f, "", "", "")) }
                val choosedSports = remember { mutableStateOf(ChoosedSports()) }

//                SideEffect {
//                    WindowCompat.setDecorFitsSystemWindows(window, false)
//                    window.statusBarColor = android.graphics.Color.BLACK
//                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
//                }

                AppNavigation(navController, authViewModel, userDetalis, choosedSports)
            }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        FitnessSignInHelper.handleActivityResult(requestCode, resultCode)
//    }

}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(navController: NavHostController, authViewModel: AuthViewModel, userDetalis: MutableState<UserDetalis>, choosedSports: MutableState<ChoosedSports>) {
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
            ChooseSportsScreen(navController, authViewModel, choosedSports)
        }
        composable("choose_discipline") {
            ChooseDisciplineScreen(navController, authViewModel, userDetalis)
        }
        composable("cycling_data_insert") {
            CyclingDataInsertScreen(navController, authViewModel, choosedSports)
        }
        composable("running_data_insert") {
            RunningDataInsertScreen(navController, authViewModel, choosedSports)
        }
        composable("plan_length_screen") {
            PlanLengthScreen(navController, authViewModel, choosedSports)
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
        composable("more") {
            MoreScreen(navController)
        }
        composable("loading_screen") {
            LoadingScreen(navController, authViewModel)
        }

//        composable("swimming_data_insert") {
//            SwimmingDataInsertScreen(navController, authViewModel)
//        }

    }
}



