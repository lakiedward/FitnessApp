package com.example.fitnessapp.pages.more

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.R
import com.example.fitnessapp.navigation.Routes
import com.example.fitnessapp.pages.home.ModernBottomNavigation
import com.example.fitnessapp.ui.theme.extendedColors
import com.example.fitnessapp.viewmodel.AuthViewModel

@Composable
fun MoreScreen(navController: NavController, authViewModel: AuthViewModel? = null) {
    val context = LocalContext.current

    val colorScheme = MaterialTheme.colorScheme
    val gradientContentColor = if (isSystemInDarkTheme()) {
        colorScheme.onSurface
    } else {
        colorScheme.onPrimary
    }
    val gradientColors = listOf(
        MaterialTheme.extendedColors.gradientPrimary,
        MaterialTheme.extendedColors.gradientSecondary,
        MaterialTheme.extendedColors.gradientAccent
    )

    val scrollState = rememberScrollState()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            ModernBottomNavigation(navController = navController)
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(colors = gradientColors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = gradientContentColor
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface.copy(alpha = 0.98f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.iclogo),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )
                            Text(
                                text = "Lucas Scott",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "@lucasscott3",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ModernSection(title = "Account") {
                            ModernSectionItem("My Account") {}
                            ModernSectionItem("Settings") {}
                            ModernSectionItem("Training Dashboard") {
                                navController.navigate(Routes.TRAINING_DASHBOARD)
                            }
                            ModernSectionItem("App Integrations") {
                                navController.navigate("app_integrations")
                            }
                            ModernSectionItem("Change Sport Metrics") {
                                navController.navigate("change_sport_metrics")
                            }
                            ModernSectionItem("Performance") {
                                navController.navigate(Routes.PERFORMANCE)
                            }
                            ModernSectionItem("Training Zones") {
                                navController.navigate("training_zones")
                            }
                        }

                        ModernSection(title = "Support") {
                            ModernSectionItem("Contact us") {}
                            ModernSectionItem("Help & Support") {}
                        }

                        ModernSection(title = "Actions") {
                            ModernSectionItem("Log out") {
                                authViewModel?.logout()
                                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                                navController.navigate("login_screen") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content)
        }
    }
}

@Composable
fun ModernSectionItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MoreScreenPreview() {
    MoreScreen(navController = rememberNavController(), authViewModel = null)
}
