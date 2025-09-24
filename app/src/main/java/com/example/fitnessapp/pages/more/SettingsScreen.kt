package com.example.fitnessapp.pages.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    LaunchedEffect(Unit) {
        authViewModel.userTrainingData
    }

    Scaffold(
        topBar = {
            SettingsTopBar(onBack = { navController.navigateUp() })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UnitsSection()
            ThemeSection()
            NotificationSection()
            PrivacySection()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            text = "App settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(0.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun UnitsSection() {
    var useMetric by rememberSaveable { mutableStateOf(true) }
    SettingsCard(
        title = "Units",
        description = "Choose how distances and temperatures are displayed throughout the app."
    ) {
        SettingsToggleRow(
            title = "Metric units",
            subtitle = "Kilometres, metres, Celsius",
            checked = useMetric,
            onToggle = { useMetric = it }
        )
        SettingsToggleRow(
            title = "Imperial units",
            subtitle = "Miles, feet, Fahrenheit",
            checked = !useMetric,
            onToggle = { useMetric = !useMetric }
        )
    }
}

@Composable
private fun ThemeSection() {
    var darkMode by rememberSaveable { mutableStateOf(false) }
    SettingsCard(
        title = "Appearance",
        description = "Switch between light and dark theme to match your environment."
    ) {
        SettingsToggleRow(
            title = "Use dark theme",
            subtitle = "Automatically adjusts colours for low light",
            checked = darkMode,
            onToggle = { darkMode = it }
        )
    }
}

@Composable
private fun NotificationSection() {
    var sessionReminders by rememberSaveable { mutableStateOf(true) }
    var summaryEmail by rememberSaveable { mutableStateOf(false) }
    SettingsCard(
        title = "Notifications",
        description = "Control which reminders and digests you receive."
    ) {
        SettingsToggleRow(
            title = "Session reminders",
            subtitle = "Push notification 30 minutes before a workout",
            checked = sessionReminders,
            onToggle = { sessionReminders = it }
        )
        SettingsToggleRow(
            title = "Weekly email summary",
            subtitle = "A recap of your training every Monday",
            checked = summaryEmail,
            onToggle = { summaryEmail = it }
        )
    }
}

@Composable
private fun PrivacySection() {
    var shareAnalytics by rememberSaveable { mutableStateOf(true) }
    var downloadData by rememberSaveable { mutableStateOf(false) }
    SettingsCard(
        title = "Privacy & data",
        description = "Manage how your data is stored and shared."
    ) {
        SettingsToggleRow(
            title = "Share anonymised analytics",
            subtitle = "Help us improve training recommendations",
            checked = shareAnalytics,
            onToggle = { shareAnalytics = it }
        )
        SettingsToggleRow(
            title = "Automatically export workout files",
            subtitle = "Sync new workouts to your cloud storage",
            checked = downloadData,
            onToggle = { downloadData = it }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock())
    )
}
