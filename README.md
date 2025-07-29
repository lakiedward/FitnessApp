# FitSense Android Application

This is the Android mobile application for FitSense, a comprehensive fitness tracking and training platform.

## Features

- User authentication and profile management
- Training plan generation and tracking
- Workout execution and recording
- Health data synchronization with Health Connect
- Activity history and statistics
- Strava integration

## Technology Stack

- Kotlin
- Jetpack Compose for UI
- Retrofit for API communication
- MVVM architecture
- Health Connect API integration

## Related Projects
- **Backend API**: Located at `/home/laki-edward/PycharmProjects/Fitness_app/`
- **Web App**: Located at `/home/laki-edward/IdeaProjects/fitsense/`

## Development Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device

## API Integration

The app communicates with the backend API using Retrofit. The API service interface is defined in:
`app/src/main/java/com/example/fitnessapp/api/ApiService.kt`