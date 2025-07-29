# Project Structure Map

This project consists of three main components located in different directories:

- **Backend (FastAPI)**: `/home/laki-edward/PycharmProjects/Fitness_app/`
- **Android UI**: `/home/laki-edward/StudioProjects/FitnessApp/`
- **Web UI (Angular)**: `/home/laki-edward/IdeaProjects/fitsense/`

## Component Details

### Backend
- FastAPI application with endpoints for user management, training plans, and health data
- Located at: `/home/laki-edward/PycharmProjects/Fitness_app/`
- Main file: `app/main.py`
- API routers in: `app/routers/`

### Android UI
- Kotlin-based Android application
- Located at: `/home/laki-edward/StudioProjects/FitnessApp/`
- Uses Retrofit for API communication with the backend
- API service: `app/src/main/java/com/example/fitnessapp/api/ApiService.kt`

### Web UI
- Angular-based web application
- Located at: `/home/laki-edward/IdeaProjects/fitsense/`
- Currently implementing API services to connect to the backend