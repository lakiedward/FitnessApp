package com.example.fitnessapp.pages

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.fitnessapp.AuthViewModel

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar("Calendar", navController)

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
                            training = trainingForDay
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
fun CalendarDayItem(day: LocalDate, isToday: Boolean, training: TrainingPlan?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
            )
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
        }
        Column(modifier = Modifier.weight(2f)) {
            if (training != null) {
                Text(
                    text = training.workout_name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                )
                Text(
                    text = training.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color.Gray),
                    maxLines = 2
                )
            } else {
                Text(
                    text = "No training planned",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color.Gray)
                )
            }
        }
    }
}




