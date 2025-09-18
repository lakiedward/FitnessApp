package com.example.fitnessapp.pages.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.model.RaceModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.components.TopBar
import com.example.fitnessapp.components.BottomNavigationBar
import com.example.fitnessapp.ui.theme.FitnessAppTheme

@Composable
fun SeasonScreen(navController: NavController, authViewModel: AuthViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val races = authViewModel.races.observeAsState(emptyList())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(colorScheme.surface) // was MaterialTheme.colorScheme.onSurface
            )
            TopBar("Season", navController)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                CurrentTrainingPhase()

                Spacer(modifier = Modifier.height(24.dp))

                FitnessScoreGraph()

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(races.value) { race ->
                        RaceRow(race)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* Handle Add Race */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Add Race",
                        style = typography.bodyLarge.copy(color = colorScheme.onPrimary),
                        textAlign = TextAlign.Center
                    )
                }
            }

            BottomNavigationBar(navController)
        }
    }
}

@Composable
fun CurrentTrainingPhase() {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Current Training Phase",
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Build Phase",
            style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ends on Dec 13, 2024",
            style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(
            progress = 0.8f,
            modifier = Modifier.size(100.dp),
            color = colorScheme.primary,
            strokeWidth = 8.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "80%",
            style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun FitnessScoreGraph() {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface) // was light violet
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Fitness Score",
                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            GraphPlaceholder()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "30% Your Fitness Score is 30% worse compared to last month",
                style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GraphPlaceholder() {
    val colorScheme = MaterialTheme.colorScheme
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)) {
        drawLine(
            color = colorScheme.primary,
            start = center.copy(x = 0f),
            end = center.copy(x = size.width),
            strokeWidth = 4f
        )
    }
}

@Composable
fun RaceRow(race: RaceModel) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface) // was light violet
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = race.race_date,
                    style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SeasonScreenPreview() {
    FitnessAppTheme {
        SeasonScreen(
            navController = rememberNavController(),
            authViewModel = AuthViewModel(SharedPreferencesMock())
        )
    }
}


