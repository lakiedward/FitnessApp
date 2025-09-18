package com.example.fitnessapp.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnessapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    titlu: String,
    navController: NavController,
    showBackButton: Boolean = false,
    showProfileIcon: Boolean = true,
    showNotificationIcon: Boolean = true
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = titlu,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (showProfileIcon) {
                IconButton(onClick = { navController.navigate("workout_screen") }) {
                    Image(
                        painter = painterResource(id = R.drawable.iclogo),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                }
            }
        },
        actions = {
            if (showNotificationIcon) {
                IconButton(onClick = { navController.navigate("workout_screen") }) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_record_white_no_text),
                        contentDescription = "Notifications",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
} 