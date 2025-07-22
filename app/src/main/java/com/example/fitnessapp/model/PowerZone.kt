package com.example.fitnessapp.model

/**
 * Enum pentru zonele de putere bazate pe FTP
 */
enum class PowerZone {
    RECOVERY,      // < 55% FTP
    ENDURANCE,     // 55-75% FTP
    TEMPO,         // 75-90% FTP
    THRESHOLD,     // 90-105% FTP
    VO2_MAX,       // 105-120% FTP
    ANAEROBIC      // > 120% FTP
}