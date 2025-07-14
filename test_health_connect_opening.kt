// Test script to debug Health Connect opening issue
// This test simulates what happens when the "Open Health Connect" button is clicked

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

fun main() {
    println("=== Health Connect Opening Debug Test ===")
    
    // Simulate the package detection logic
    println("\n1. Testing package detection:")
    val possiblePackages = listOf(
        "com.google.android.healthconnect.controller", // Newer Health Connect
        "com.google.android.apps.healthdata", // Older Health Connect
        "com.android.healthconnect" // Alternative package name
    )
    
    println("   Checking possible Health Connect packages:")
    for (packageName in possiblePackages) {
        println("   - $packageName")
    }
    
    // Test intent construction
    println("\n2. Testing intent construction:")
    val testPackageName = "com.google.android.healthconnect.controller"
    
    // Strategy 1: Direct app launch
    println("   Strategy 1: Direct app launch intent")
    println("   - Package: $testPackageName")
    println("   - Action: Launch intent for package")
    println("   - Flags: FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP")
    
    // Strategy 2: Permission request intent
    println("   Strategy 2: Permission request intent")
    println("   - Action: androidx.health.ACTION_REQUEST_PERMISSIONS")
    println("   - Package: $testPackageName")
    println("   - Flags: FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP")
    
    // Strategy 3: Settings intent
    println("   Strategy 3: Settings intent")
    println("   - Action: androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
    println("   - Flags: FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP")
    
    println("\n3. Common issues that could prevent Health Connect from opening:")
    println("   ✓ Wrong package name - Fixed with dynamic detection")
    println("   ✓ Intent flags - Using NEW_TASK and CLEAR_TOP")
    println("   ? Security exceptions - Could be blocking intent")
    println("   ? App not installed - Should be detected")
    println("   ? Intent action not supported - Multiple fallbacks provided")
    
    println("\n4. Potential solutions to test:")
    println("   1. Add more detailed error logging")
    println("   2. Try simpler intent construction")
    println("   3. Add intent validation before launching")
    println("   4. Test with different intent flags")
    println("   5. Add user feedback when intents fail")
    
    println("\n=== Recommended Fix ===")
    println("Add more robust error handling and user feedback to the openHealthConnectSettings() method")
    println("to help identify exactly why Health Connect is not opening.")
}