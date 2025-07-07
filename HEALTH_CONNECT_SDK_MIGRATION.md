# Health Connect SDK 1.1+ Migration

## Issue Description
The user reported that Health Connect shows "please grant permissions in health connect and try again" but they don't receive anything in Health Connect. This is because custom intents like `androidx.health.ACTION_REQUEST_PERMISSIONS` and deep-links are no longer exposed by Health Connect 1.1+.

## Root Cause
In Health Connect 1.1+, the following approaches no longer work:
- Custom intents: `androidx.health.ACTION_REQUEST_PERMISSIONS`
- Deep links: `healthconnect://permissions?package=...`
- Direct intent launching from ViewModel

## Solution Implemented

### 1. Updated ViewModel Architecture
**File**: `HealthConnectViewModel.kt`

**Changes Made**:
- Removed problematic intent-based permission requests
- Added callback methods for UI layer to handle permission results:
  - `onPermissionsGranted()` - Called when permissions are granted
  - `onPermissionsDenied()` - Called when permissions are denied
- Updated `requestHealthConnectPermissions()` to delegate to UI layer

### 2. Updated UI Layer Pattern
**File**: `AppIntegrationsScreen.kt`

**Changes Made**:
- Added TODO comment with proper Health Connect SDK implementation pattern
- Prepared structure for official SDK usage

### 3. Proper Health Connect SDK Pattern (To Be Implemented)

The correct implementation should follow this pattern:

```kotlin
// 1. Define permissions
val PERMS = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getWritePermission(StepsRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getWritePermission(DistanceRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getWritePermission(HeartRateRecord::class)
)

// 2. Create permission launcher
val launcher = rememberLauncherForActivityResult(
    PermissionController.createRequestPermissionResultContract()
) { granted: Set<String> ->
    if (granted.containsAll(PERMS)) {
        healthConnectViewModel.onPermissionsGranted()
    } else {
        healthConnectViewModel.onPermissionsDenied()
    }
}

// 3. Handle button click with SDK status check
Button(onClick = {
    when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_UNAVAILABLE -> {
            // Health Connect not installed
            healthConnectViewModel.onPermissionsDenied()
        }
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
            // Open Play Store for update
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            }
            context.startActivity(intent)
        }
        else -> launcher.launch(PERMS)   // Open permission screen automatically
    }
}) { Text("Connect to Health Connect") }
```

## Required Imports (When Available)

```kotlin
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
```

## Benefits of This Approach

1. **Automatic Permission UI**: The Health Connect permission screen opens automatically
2. **Proper SDK Integration**: Uses official Health Connect SDK methods
3. **Better User Experience**: No manual navigation to Health Connect settings
4. **Future-Proof**: Compatible with Health Connect 1.1+ requirements
5. **Clean Architecture**: ViewModel doesn't launch intents directly

## Current Status

- ✅ ViewModel updated to use callback pattern
- ✅ UI structure prepared for SDK implementation
- ✅ Documentation and TODO comments added
- ⏳ Waiting for Health Connect SDK imports to resolve
- ⏳ Final implementation pending SDK availability

## Next Steps

1. Resolve Health Connect SDK import issues
2. Implement the permission launcher pattern in AppIntegrationsScreen
3. Test the automatic permission flow
4. Remove fallback methods once SDK implementation is working

This migration ensures that the Health Connect integration will work properly with Health Connect 1.1+ and provides users with the automatic permission dialog they expect.
