package com.example.fitnessapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
//import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme


class AddAge : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessAppTheme {
                //GenderSelectionScreen()
            }
        }
    }
}


@Composable
fun AddAgeScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize()
    ) {
        IconButton(
            onClick = {
                navController.navigateUp()
            },
            modifier = Modifier
                //.padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            // Text(text = "Back", color = Color.Black)
            Image(
                painter = painterResource(id = R.drawable.back),
                contentDescription = "Back",
            )

        }
    }

}