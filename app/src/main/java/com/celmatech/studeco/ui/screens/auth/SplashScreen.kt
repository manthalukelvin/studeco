package com.celmatech.studeco.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.celmatech.studeco.ui.navigation.Route
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isLoading: Boolean,
    isAuthenticated: Boolean,
    needsEmailVerification: Boolean,
    onNavigate: (String) -> Unit
) {
    LaunchedEffect(isLoading, isAuthenticated, needsEmailVerification) {
        if (!isLoading) {
            delay(400) // brief brand moment
            when {
                isAuthenticated && needsEmailVerification -> onNavigate(Route.EmailVerification.path)
                isAuthenticated -> onNavigate(Route.Main.path)
                else -> onNavigate(Route.Login.path)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "StudeCo",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 42.sp
                )
            )
            Text(
                text = "Digital Campus",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
