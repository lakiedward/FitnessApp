/**
 * @project FitSense
 * @component Android UI
 * 
 * @related_components
 * - Backend API: /home/laki-edward/PycharmProjects/Fitness_app/
 * - Web UI: /home/laki-edward/IdeaProjects/fitsense/
 */

package com.example.fitnessapp

import android.content.Context
import android.content.Intent
 
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.ChoosedSports
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.pages.LoginScreen
import com.example.fitnessapp.pages.home.HomeScreen
import com.example.fitnessapp.pages.home.InfiniteCalendarPage
import com.example.fitnessapp.pages.home.SeasonScreen
import com.example.fitnessapp.pages.home.StravaActivityDetailScreen
import com.example.fitnessapp.pages.home.TrainingDetailScreen
import com.example.fitnessapp.pages.home.WorkoutScreen
import com.example.fitnessapp.pages.home.QuickCreateTrainingPlanScreen
import com.example.fitnessapp.pages.loading.LoadingScreen
import com.example.fitnessapp.pages.loading.LoadingTrainingScreen
import com.example.fitnessapp.pages.more.AppIntegrationsScreen
import com.example.fitnessapp.pages.more.ChangeSportMetricsScreen
import com.example.fitnessapp.pages.more.MoreScreen
import com.example.fitnessapp.pages.more.PerformanceScreen
import com.example.fitnessapp.pages.more.TrainingZonesScreen
import com.example.fitnessapp.pages.signup.AddAgeScreen
import com.example.fitnessapp.pages.signup.AddEmailScreen
import com.example.fitnessapp.pages.signup.AddFtpScreen
import com.example.fitnessapp.pages.signup.AddRunningPaceScreen
import com.example.fitnessapp.pages.signup.AddSwimPaceScreen
import com.example.fitnessapp.pages.signup.ChooseDisciplineScreen
import com.example.fitnessapp.pages.signup.ChooseSportsScreen
import com.example.fitnessapp.pages.signup.DisciplineLoadingScreen
import com.example.fitnessapp.pages.signup.PlanLengthScreen
import com.example.fitnessapp.pages.signup.SetupStatusLoadingScreen
import com.example.fitnessapp.pages.signup.StravaAuthScreen
import com.example.fitnessapp.pages.strava.StravaActivitiesScreen
import com.example.fitnessapp.pages.strava.StravaSyncLoadingScreen
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.HealthConnectViewModel
import com.example.fitnessapp.viewmodel.StravaViewModel
import kotlinx.coroutines.launch
import com.example.fitnessapp.navigation.Routes

import java.time.LocalDate

import com.example.fitnessapp.utils.AuthEvent
import com.example.fitnessapp.utils.AuthEventBus

class MainActivity : ComponentActivity() {
    private lateinit var authViewModel: AuthViewModel
    private val stravaViewModel: StravaViewModel by lazy {
        StravaViewModel.getInstance(applicationContext)
    }
    private val healthConnectViewModel: HealthConnectViewModel by lazy {
        HealthConnectViewModel.getInstance(applicationContext)
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

        // Handle Strava code if forwarded from callback activity
        handleStravaCode(intent)

        setContent {
            FitnessAppTheme {
                val navController = rememberNavController()
                val userDetalis = remember { mutableStateOf(UserDetalis(0, 0f, 0f, "", "")) }
                val choosedSports = remember { mutableStateOf(ChoosedSports()) }
                AppNavigation(
                    navController,
                    authViewModel,
                    userDetalis,
                    choosedSports,
                    stravaViewModel,
                    healthConnectViewModel
                )
            }
        }

        // Global listener: when a 401 happens anywhere, logout
        lifecycleScope.launch {
            AuthEventBus.events.collect { event ->
                if (event is AuthEvent.TokenInvalid) {
                    authViewModel.logout()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStravaCode(intent)
    }

    // Other deep links can be handled here if needed (Strava handled by StravaCallbackActivity)

    private fun handleStravaCode(intent: Intent?) {
        val code = intent?.getStringExtra("strava_code")
        if (!code.isNullOrEmpty()) {
            // Verific dacă există JWT (poți adapta după implementarea ta)
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
    stravaViewModel: StravaViewModel,
    healthConnectViewModel: HealthConnectViewModel
) {
    // Observe auth state for logout handling
    val authState by authViewModel.authState.observeAsState()

    // Navigate to login when user is logged out, using LaunchedEffect to avoid constant recomposition
    LaunchedEffect(authState) {
        if (authState is com.example.fitnessapp.viewmodel.AuthState.Unauthenticated &&
            !com.example.fitnessapp.utils.AuthGuard.isSuppressed()) {
            navController.navigate("login_screen") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            // Decide where to go based on stored token
            LaunchedEffect(Unit) {
                val token = authViewModel.getToken()
                if (!token.isNullOrEmpty()) {
                    authViewModel.restoreSessionIfPossible()
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.LOGIN) {
            LoginScreen(navController, authViewModel)
        }
        composable(Routes.APP_INTEGRATIONS) {
            AppIntegrationsScreen(
                authViewModel = authViewModel,
                stravaViewModel = stravaViewModel,
                healthConnectViewModel = healthConnectViewModel,
                apiService = RetrofitClient.retrofit.create(ApiService::class.java),
                onOpenStravaSync = { navController.navigate(Routes.STRAVA_SYNC_LOADING) },
                onNavigateBack = { navController.navigateUp() },
                onRequireLogin = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.ADD_AGE) {
            AddAgeScreen(navController, userDetalis)
        }
        composable(Routes.STRAVA_AUTH) {
            StravaAuthScreen(
                onContinue = { navController.navigate(Routes.CHOOSE_DISCIPLINE) },
                authViewModel = authViewModel,
                stravaViewModel = stravaViewModel,
                navController = navController
            )
        }
        composable(Routes.CHOOSE_SPORTS) {
            ChooseSportsScreen(navController, authViewModel, choosedSports, userDetalis)
        }
        composable(Routes.SETUP_STATUS_LOADING) {
            SetupStatusLoadingScreen(navController, authViewModel)
        }
        composable(Routes.CHOOSE_DISCIPLINE) {
            ChooseDisciplineScreen(navController, authViewModel, userDetalis)
        }
        composable(Routes.DISCIPLINE_LOADING) {
            DisciplineLoadingScreen(navController, authViewModel, userDetalis.value)
        }
        composable(Routes.ADD_FTP) {
            AddFtpScreen(navController, authViewModel)
        }
        composable(Routes.ADD_RUNNING_PACE) {
            AddRunningPaceScreen(navController, authViewModel)
        }
        composable(Routes.ADD_SWIM_PACE) {
            AddSwimPaceScreen(navController, authViewModel)
        }
        composable(Routes.PLAN_LENGTH) {
            PlanLengthScreen(navController, authViewModel, choosedSports)
        }
        composable(Routes.ENTER_EMAIL) {
            AddEmailScreen(navController, authViewModel)
        }
        composable(Routes.HOME){
            HomeScreen(navController, authViewModel, stravaViewModel, healthConnectViewModel)
        }
        composable(Routes.CALENDAR){
            InfiniteCalendarPage(navController, authViewModel)
        }
        composable(
            route = Routes.TRAINING_CREATE,
            arguments = listOf(navArgument("date") { nullable = true })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            QuickCreateTrainingPlanScreen(
                navController = navController,
                authViewModel = authViewModel,
                defaultDate = date ?: LocalDate.now().toString()
            )
        }
        composable(Routes.SEASON) {
            SeasonScreen(navController, authViewModel)
        }
        composable(Routes.WORKOUT) {
            WorkoutScreen(navController)
        }
        composable(Routes.MORE) {
            MoreScreen(navController, authViewModel)
        }
        composable(Routes.PERFORMANCE) {
            PerformanceScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable(Routes.CHANGE_SPORT_METRICS) {
            ChangeSportMetricsScreen(navController, authViewModel)
        }
        composable(Routes.TRAINING_ZONES) {
            TrainingZonesScreen(navController, authViewModel)
        }
        composable(Routes.LOADING) {
            LoadingScreen(navController, authViewModel)
        }
        composable(Routes.STRAVA_ACTIVITIES) {
            StravaActivitiesScreen(
                stravaViewModel = stravaViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Routes.STRAVA_SYNC_LOADING) {
            StravaSyncLoadingScreen(
                onNavigateBack = { navController.navigateUp() },
                onSyncComplete = { navController.navigateUp() },
                onViewActivities = { navController.navigate(Routes.STRAVA_ACTIVITIES) },
                stravaViewModel = stravaViewModel,
                authViewModel = authViewModel
            )
        }
        composable(Routes.TRAINING_DETAIL) { backStackEntry ->
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
        composable(Routes.LOADING_TRAINING) { backStackEntry ->
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
        composable(Routes.WORKOUT_EXECUTION) { backStackEntry ->
            val trainingId = backStackEntry.arguments?.getString("trainingId")?.toIntOrNull()
            val trainingPlans by authViewModel.trainingPlan.observeAsState(emptyList())
            val training = trainingPlans.find { it.id == trainingId }
            if (training != null) {
                com.example.fitnessapp.pages.workout.WorkoutExecutionScreen(
                    trainingId = trainingId ?: 0,
                    training = training,
                    navController = navController
                )
            }
        }
        composable(Routes.STRAVA_ACTIVITY_DETAIL) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")?.toLongOrNull() ?: 0L
            StravaActivityDetailScreen(
                navController = navController,
                activityId = activityId
            )
        }
        composable(Routes.TRAINING_DASHBOARD) {
            com.example.fitnessapp.pages.workout.TrainingDashboardScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }
}

