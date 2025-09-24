package com.example.fitnessapp.navigation

object Routes {
    // Core
    const val SPLASH = "splash"
    const val LOGIN = "login_screen"
    const val HOME = "home_screen"

    // Onboarding
    const val ENTER_EMAIL = "enter_add_email"
    const val ADD_AGE = "add_age_screen"
    const val STRAVA_AUTH = "strava_auth_screen"
    const val CHOOSE_DISCIPLINE = "choose_discipline"
    const val DISCIPLINE_LOADING = "discipline_loading_screen"
    const val CHOOSE_SPORTS = "choose_sports"
    const val SETUP_STATUS_LOADING = "setup_status_loading_screen"
    const val ADD_FTP = "add_ftp_screen"
    const val ADD_RUNNING_PACE = "add_running_pace_screen"
    const val ADD_SWIM_PACE = "add_swim_pace_screen"
    const val PLAN_LENGTH = "plan_length_screen"

    // App sections
    const val CALENDAR = "calendar_screen"
    const val SEASON = "season_screen"
    const val WORKOUT = "workout_screen"
    const val MORE = "more"
    const val TRAINING_DASHBOARD = "training_dashboard"
    const val APP_INTEGRATIONS = "app_integrations"
    const val CHANGE_SPORT_METRICS = "change_sport_metrics"
    const val TRAINING_ZONES = "training_zones"
    const val PERFORMANCE = "performance_metrics"

    // Loading
    const val LOADING = "loading_screen"
    const val STRAVA_ACTIVITIES = "strava_activities"
    const val STRAVA_SYNC_LOADING = "strava_sync_loading"

    // Parameterized routes
    const val TRAINING_DETAIL = "training_detail/{trainingId}"
    const val LOADING_TRAINING = "loading_training/{trainingId}"
    const val WORKOUT_EXECUTION = "workout_execution/{trainingId}"
    const val STRAVA_ACTIVITY_DETAIL = "strava_activity_detail/{activityId}"
    const val TRAINING_CREATE = "training_detail/new?date={date}"

    // Builders
    fun trainingDetail(id: Int) = "training_detail/$id"
    fun loadingTraining(id: Int) = "loading_training/$id"
    fun workoutExecution(id: Int) = "workout_execution/$id"
    fun stravaActivityDetail(id: Long) = "strava_activity_detail/$id"
    fun trainingCreate(date: String) = "training_detail/new?date=$date"
}


