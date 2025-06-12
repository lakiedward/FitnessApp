package com.example.fitnessapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.model.ChoosedSports
import com.example.fitnessapp.pages.signup.*
import com.example.fitnessapp.pages.home.*
import com.example.fitnessapp.pages.LoginScreen
import com.example.fitnessapp.pages.loading.LoadingScreen
import com.example.fitnessapp.pages.loading.LoadingTrainingScreen
import com.example.fitnessapp.pages.more.AppIntegrationsScreen
import com.example.fitnessapp.pages.more.MoreScreen
import com.example.fitnessapp.pages.strava.StravaActivitiesScreen
import com.example.fitnessapp.pages.strava.StravaSyncLoadingScreen
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import android.net.Uri
import androidx.compose.runtime.livedata.observeAsState
import com.example.fitnessapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private lateinit var authViewModel: AuthViewModel
    private val stravaViewModel: StravaViewModel by lazy {
        StravaViewModel.getInstance(applicationContext)
    }

    // Variabilă pentru codul Strava primit când userul nu e logat
    private var pendingStravaCode: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)
        authViewModel = AuthViewModel(sharedPreferences)

        // Observer clasic pentru LiveData
        authViewModel.authState.observe(this) { state ->
            if (state is com.example.fitnessapp.viewmodel.AuthState.Authenticated && pendingStravaCode != null) {
                lifecycleScope.launch {
                    try {
                        stravaViewModel.handleAuthCode(pendingStravaCode!!)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error handling Strava auth code", e)
                    }
                }
                pendingStravaCode = null
            }
        }

        // Handle the intent that started this activity
        handleIntent(intent)
        handleStravaCode(intent)

        setContent {
            FitnessAppTheme(darkTheme = false) {
                val navController = rememberNavController()
                val userDetalis = remember { mutableStateOf(UserDetalis(0, 0f, 0f, "", "")) }
                val choosedSports = remember { mutableStateOf(ChoosedSports()) }
                AppNavigation(navController, authViewModel, userDetalis, choosedSports, stravaViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        handleStravaCode(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri?.scheme == "fitnessapp" && uri.host == "strava" && uri.path?.startsWith("/callback") == true) {
                // Extract the authorization code from the URI
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    Log.d("MainActivity", "Received Strava authorization code: $code")
                    // Handle the auth code
                    lifecycleScope.launch {
                        try {
                            stravaViewModel.handleAuthCode(code)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error handling Strava auth code", e)
                        }
                    }
                } else {
                    Log.e("MainActivity", "No authorization code found in callback URI")
                }
            }
        }
    }

    private fun handleStravaCode(intent: Intent?) {
        val code = intent?.getStringExtra("strava_code")
        if (!code.isNullOrEmpty()) {
            // Verific dacă userul e logat (există JWT)
            val jwt = getJwtToken()
            if (!jwt.isNullOrEmpty()) {
                lifecycleScope.launch {
                    try {
                        stravaViewModel.handleAuthCode(code)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error handling Strava auth code", e)
                    }
                }
            } else {
                // Salvez codul pentru a-l procesa după login
                pendingStravaCode = code
            }
        }
    }

    // Helper pentru a verifica dacă există JWT (poți adapta după implementarea ta)
    private fun getJwtToken(): String? {
        val sharedPreferences = getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", null)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userDetalis: MutableState<UserDetalis>,
    choosedSports: MutableState<ChoosedSports>,
    stravaViewModel: StravaViewModel
) {
    NavHost(navController = navController, startDestination = "login_screen") {
        composable("login_screen") {
            LoginScreen(navController, authViewModel)
        }
        composable("gender_selection_screen") {
            GenderSelectionScreen(navController, userDetalis)
        }
        composable("app_integrations") {
            AppIntegrationsScreen(
                onNavigateToStravaActivities = { navController.navigate("strava_activities") },
                authViewModel = authViewModel,
                stravaViewModel = stravaViewModel,
                navController = navController
            )
        }
        composable("add_age_screen") {
            AddAgeScreen(navController, userDetalis)
        }
        composable("strava_auth_screen") {
            StravaAuthScreen(
                onContinue = { navController.navigate("choose_discipline") },
                authViewModel = authViewModel,
                stravaViewModel = stravaViewModel,
                navController = navController
            )
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
            HomeScreen(navController, authViewModel, stravaViewModel)
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
        composable("strava_activities") {
            StravaActivitiesScreen(
                stravaViewModel = stravaViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable("strava_sync_loading") {
            StravaSyncLoadingScreen(
                onNavigateBack = { navController.navigateUp() },
                onSyncComplete = { navController.navigateUp() },
                stravaViewModel = stravaViewModel,
                authViewModel = authViewModel
            )
        }
        composable("training_detail/{trainingId}") { backStackEntry ->
            val trainingId = backStackEntry.arguments?.getString("trainingId")?.toIntOrNull()
            val trainingPlans by authViewModel.trainingPlan.observeAsState(emptyList())
            val training = trainingPlans.find { it.id == trainingId }
            if (training != null) {
                TrainingDetailScreen(
                    training = training,
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }
        composable("loading_training/{trainingId}") { backStackEntry ->
            val trainingId = backStackEntry.arguments?.getString("trainingId")?.toIntOrNull()
            val trainingPlans by authViewModel.trainingPlan.observeAsState(emptyList())
            val training = trainingPlans.find { it.id == trainingId }
            if (training != null) {
                LoadingTrainingScreen(
                    training = training,
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }
    }
}



