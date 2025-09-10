Project DB and Migrations Guide

Overview
- Backend API in Fitness_app uses MySQL via env vars.
- All migration files and the runner live separately in db-migrate.

Where Things Live Now
- Backend (FastAPI): C:\Users\lakie\PycharmProjects\Fitness_app
  - Server: app/main.py
  - DB runtime connection: app/database/connection.py
- DB-Migrate (standalone): C:\Users\lakie\PycharmProjects\db-migrate
  - migrations/: SQL files
  - migrate_runner.py: applies all migrations

Running Migrations
- Open: C:\Users\lakie\PycharmProjects\db-migrate
- Install deps then run:
  python -m pip install -r requirements.txt
  python migrate_runner.py

Env Vars
- MYSQLHOST, MYSQLUSER, MYSQLPASSWORD, MYSQLDATABASE, MYSQLPORT (optional)

