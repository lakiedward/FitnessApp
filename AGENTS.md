# AGENTS (Android – modul FitnessApp)

Acest fișier acoperă doar modulul Android din acest repo. Ghidul comun pentru întregul monorepo se află la rădăcina proiectului în `AGENTS.md`.

## Domeniu
- Kotlin + Jetpack Compose, arhitectură MVVM, integrare Health Connect, REST API către Backend.

## Locație și directoare cheie
- Rădăcina modulului: `FitnessApp/`
- Surse: `app/src/main/java/com/example/fitnessapp/`
- Resurse: `app/src/main/res/`

## Build și comenzi utile
- Build debug (Unix): `./gradlew assembleDevDebug`
- Build debug (Windows): `gradlew.bat assembleDevDebug`
- Teste unitare: `./gradlew testDevDebugUnitTest`
- Lint: `./gradlew lintDevDebug`

Note:
- Flavors: `dev` și `prod` (vezi `app/build.gradle.kts`).
- `BuildConfig.BASE_URL` este setat prin flavors și valori din `local.properties` (`DEV_BASE_URL`, `PROD_BASE_URL`).

## Sarcini tipice
- Implementare ecrane Compose și refactor MVVM/ViewModel.
- Configurare Retrofit/OkHttp, mapări modele API și DTO-uri.
- Integrare Health Connect și senzori (după caz).

## Semnale de handoff (către alte module)
- Orice modificare de contract API sau câmpuri noi/renumite în modele → notificați Backend (`../Fitness_app/`) și Migrații DB (`../db-migrate-Fitness_App/`).
- Schimbări de URL/medii (`BuildConfig.BASE_URL`) → aliniere cu Backend și configurările CORS.
- Cerințe noi de date pentru UI → deschideți discuție cu Backend pentru a ajusta endpoint-urile.

## Resurse
- `PROJECT_MAP.md` – vedere de ansamblu a componentelor Android.
- `.ai-assistant-guide.md` – bune practici pentru asistenții AI în modulul Android.
- `README.md`, `README_DB.md` – context operațional și de integrare.

