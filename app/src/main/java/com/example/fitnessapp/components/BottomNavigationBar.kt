package com.example.fitnessapp.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnessapp.R

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        "home_screen" to "Home",
        "calendar_screen" to "Calendar",
        "season_screen" to "Season",
        "more" to "More"
    )
    val icons = listOf(
        R.drawable.ic_home,
        R.drawable.ic_calendar,
        R.drawable.ic_season,
        R.drawable.ic_more
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        items.forEachIndexed { index, (route, label) ->
            val selected = navController.currentBackStackEntry?.destination?.route == route
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigate(route) },
                icon = {
                    Image(
                        painter = painterResource(id = icons[index]),
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(
                            if (selected) MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        )
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary 
                               else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
} 