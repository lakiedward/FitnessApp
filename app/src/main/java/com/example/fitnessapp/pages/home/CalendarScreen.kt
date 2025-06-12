package com.example.fitnessapp.pages.home

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fitnessapp.model.TrainingPlan
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.components.TrainingChart
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun InfiniteCalendarPage(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val trainingPlans by authViewModel.trainingPlan.observeAsState(emptyList())
    Log.d("CalendarScreen", "Training Plans: $trainingPlans") // Debugging aici
    val today = LocalDate.now()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = Int.MAX_VALUE / 2)
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color.Black)
            )

            TopBar("Calendar", navController)

            // Observăm ziua vizibilă și derivăm luna
            val firstVisibleDay = remember {
                derivedStateOf {
                    today.plusDays((listState.firstVisibleItemIndex - Int.MAX_VALUE / 2).toLong())
                }
            }
            val currentMonthYear by remember {
                derivedStateOf {
                    val day = firstVisibleDay.value
                    day.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercaseChar() } + " " + day.year
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF333333))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentMonthYear,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, color = Color.White)
                )

                TextButton(
                    onClick = {
                        // Scroll la ziua de azi
                        val todayIndex = Int.MAX_VALUE / 2
                        coroutineScope.launch {
                            listState.animateScrollToItem(todayIndex)
                        }
                    }
                ) {
                    Text("Today", color = Color.White)
                }
            }



            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Int.MAX_VALUE) { index ->
                        val day = today.plusDays((index - Int.MAX_VALUE / 2).toLong())
                        val trainingForDay = trainingPlans.find { it.date == day.toString() }
                        CalendarDayItem(
                            day = day,
                            isToday = day == today,
                            training = trainingForDay,
                            navController = navController
                        )
                    }
                }
            }
            BottomNavigationBar(navController)
        }
    }
}





@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarDayItem(
    day: LocalDate,
    isToday: Boolean,
    training: TrainingPlan?,
    navController: NavController
) {
    Log.d("CalendarChart", "steps = ${training?.steps}")

    val cardModifier = if (training != null) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable { navController.navigate("loading_training/${training.id}") }
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // 🟡 Ziua + ziua lunii
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🟩 Interior: descriere + grafic
            if (training != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)), // Gri contrastant
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = training.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = Color.DarkGray)
                        )

                        training.steps?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            TrainingChart(steps = it)
                        }
                    }
                }
            } else {
                Text(
                    text = "No training planned",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color.Gray)
                )
            }
        }
    }
}






