package com.example.fitnessapp.utils

/**
 * Small guard to temporarily suppress global auto-logout (401 handling)
 * during sensitive flows like OAuth where short 401s may legally occur.
 */
object AuthGuard {
    @Volatile
    private var suppressUntilMs: Long = 0L

    fun suppressAutoLogoutFor(ms: Long = 30_000) {
        suppressUntilMs = System.currentTimeMillis() + ms
    }

    fun isSuppressed(): Boolean = System.currentTimeMillis() < suppressUntilMs
}

