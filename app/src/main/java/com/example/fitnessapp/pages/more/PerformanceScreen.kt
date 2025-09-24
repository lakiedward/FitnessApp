package com.example.fitnessapp.pages.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fitnessapp.navigation.Routes
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.MetricCardState
import com.example.fitnessapp.viewmodel.MetricStatus
import com.example.fitnessapp.viewmodel.MetricType
import com.example.fitnessapp.viewmodel.PerformanceViewModel

@Composable
fun PerformanceScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    performanceViewModel: PerformanceViewModel = viewModel()
) {
    val uiState by performanceViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var metricToRecalculate by remember { mutableStateOf<MetricType?>(null) }

    LaunchedEffect(Unit) {
        performanceViewModel.refreshAll(authViewModel.getToken())
    }

    LaunchedEffect(uiState.globalError) {
        uiState.globalError?.let { message ->
            if (message.isNotBlank()) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    metricToRecalculate?.let { metric ->
        AlertDialog(
            onDismissRequest = { metricToRecalculate = null },
            title = { Text(text = "Confirm\u0103 recalcularea") },
            text = {
                Text(
                    text = "Pornim recalcularea pentru ${metric.displayName}? Aceasta poate dura c\u00E2teva minute."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performanceViewModel.recalc(metric, authViewModel.getToken())
                        metricToRecalculate = null
                    }
                ) {
                    Text(text = "Recalculeaz\u0103")
                }
            },
            dismissButton = {
                TextButton(onClick = { metricToRecalculate = null }) {
                    Text(text = "Renun\u021B\u0103")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Performance") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "\u00CEnapoi"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { performanceViewModel.refreshAll(authViewModel.getToken()) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Re\u00EEmprosp\u0103teaz\u0103"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(MetricType.values(), key = { it.name }) { type ->
                        val metricState = uiState.metrics[type] ?: MetricCardState(type)
                        PerformanceMetricCard(
                            state = metricState,
                            onRefresh = { performanceViewModel.refreshMetric(type, authViewModel.getToken()) },
                            onRecalculate = { metricToRecalculate = type }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(onClick = { navController.navigate(Routes.CHANGE_SPORT_METRICS) }) {
                                Text(text = "Actualizeaz\u0103 manual metricile")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceMetricCard(
    state: MetricCardState,
    onRefresh: () -> Unit,
    onRecalculate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricHeader(state = state)

            AnimatedVisibility(visible = state.isRefreshing || state.isRecalculating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            MetricValueRow(state = state)

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            MetricMetaRow(state = state)

            state.statusMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.error?.takeIf { it.isNotBlank() }?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing && !state.isRecalculating
                ) {
                    Text(text = "Actualizeaz\u0103")
                }

                Button(
                    onClick = onRecalculate,
                    enabled = !state.isRecalculating && state.status != MetricStatus.COOLDOWN,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (state.isRecalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(text = "Recalculeaz\u0103")
                }
            }
        }
    }
}

@Composable
private fun MetricHeader(state: MetricCardState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = metricIcon(state.metric),
                contentDescription = state.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column {
                Text(
                    text = state.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StatusBadge(status = state.status)
    }
}

@Composable
private fun MetricValueRow(state: MetricCardState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = state.value ?: "--",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = state.status.displayText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricMetaRow(state: MetricCardState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetaLine(label = "Ultima actualizare", value = state.lastUpdated ?: "--")
        MetaLine(label = "Surs\u0103", value = state.source ?: "--")
        if (!state.cooldownUntil.isNullOrBlank()) {
            MetaLine(label = "Cooldown", value = state.cooldownUntil)
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusBadge(status: MetricStatus) {
    val (background, content) = when (status) {
        MetricStatus.OK -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        MetricStatus.RECALCULATING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        MetricStatus.COOLDOWN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        MetricStatus.INSUFFICIENT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        MetricStatus.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        MetricStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.displayText,
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}

@Composable
private fun metricIcon(metric: MetricType) = when (metric) {
    MetricType.CYCLING_FTP -> Icons.Filled.DirectionsBike
    MetricType.RUNNING_FTP -> Icons.Filled.DirectionsRun
    MetricType.SWIM_CSS -> Icons.Filled.Pool
}
