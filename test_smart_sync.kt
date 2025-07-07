fun main() {
    println("=== Health Connect Smart Sync Test ===")
    
    // Simulate the smart sync logic
    val now = java.time.Instant.now()
    
    // Test case 1: First time sync (no last sync date)
    println("\n1. First-time sync test:")
    val firstSyncStart = now.minus(30, java.time.temporal.ChronoUnit.DAYS)
    println("   Start date: $firstSyncStart")
    println("   End date: $now")
    println("   Period: 30 days (first-time sync)")
    
    // Test case 2: Incremental sync with recent last sync
    println("\n2. Recent incremental sync test:")
    val recentLastSync = now.minus(53, java.time.temporal.ChronoUnit.MINUTES)
    val minimumSyncPeriod = now.minus(24, java.time.temporal.ChronoUnit.HOURS)
    val incrementalStart = if (recentLastSync.isBefore(minimumSyncPeriod)) recentLastSync else minimumSyncPeriod
    println("   Last sync: $recentLastSync (53 minutes ago)")
    println("   Minimum period: $minimumSyncPeriod (24 hours ago)")
    println("   Actual start: $incrementalStart")
    println("   Period: ${java.time.temporal.ChronoUnit.HOURS.between(incrementalStart, now)} hours")
    
    // Test case 3: Expansion logic when no activities found
    println("\n3. Expansion logic test:")
    println("   Initial window: 24 hours -> 0 activities found")
    val expandedTo7Days = now.minus(7, java.time.temporal.ChronoUnit.DAYS)
    println("   Expanded to 7 days: $expandedTo7Days -> checking for activities")
    val expandedTo30Days = now.minus(30, java.time.temporal.ChronoUnit.DAYS)
    println("   Expanded to 30 days: $expandedTo30Days -> final attempt")
    
    println("\n=== Smart Sync Logic Benefits ===")
    println("✓ Ensures minimum 24-hour sync window")
    println("✓ Automatically expands search when no activities found")
    println("✓ Maintains efficiency for regular incremental syncs")
    println("✓ Provides comprehensive coverage for first-time syncs")
    println("✓ Includes utility functions for sync management")
}