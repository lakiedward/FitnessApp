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

### Environment configuration (dev / prod)

- The app now uses product flavors `dev` and `prod` with `BuildConfig.BASE_URL`.
- Optionally set these in `local.properties`:

  DEV_BASE_URL=http://192.168.60.108:8000/
  PROD_BASE_URL=https://fitnessapp-production-60ee.up.railway.app/

- Select the build variant in Android Studio (e.g., `devDebug`, `prodRelease`).
- Debug builds allow cleartext (HTTP) to the dev host; release builds disable cleartext.

## API Integration

The app communicates with the backend API using Retrofit. The API service interface is defined in:
`app/src/main/java/com/example/fitnessapp/api/ApiService.kt`

Authorization headers are now added automatically by an OkHttp interceptor when a token is available; the header is redacted in HTTP logs.

## Database Migrations\n\nMigrations are now maintained in a standalone repo:\n- Path: C:\\Users\\lakie\\PycharmProjects\\db-migrate\n- Run all: `python migrate_runner.py` with MYSQL env vars set.\n\nThis Android repo no longer runs migrations; the previous runner is deprecated.

