package com.celmatech.studeco.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.celmatech.studeco.ui.screens.auth.EmailVerificationScreen
import com.celmatech.studeco.ui.screens.auth.LoginScreen
import com.celmatech.studeco.ui.screens.auth.ResetPasswordScreen
import com.celmatech.studeco.ui.screens.auth.SignupScreen
import com.celmatech.studeco.ui.screens.auth.SplashScreen
import com.celmatech.studeco.ui.screens.main.MainScreen
import com.celmatech.studeco.ui.screens.auth.AuthViewModel

@Composable
fun StudeCoNavGraph() {
    val navController = rememberNavController()
    val authVm: AuthViewModel = hiltViewModel()
    val authState by authVm.authState.collectAsState()

    LaunchedEffect(Unit) {
        authVm.initialize()
    }

    NavHost(
        navController = navController,
        startDestination = Route.Splash.path
    ) {
        composable(Route.Splash.path) {
            SplashScreen(
                isLoading = authState.isLoading,
                isAuthenticated = authState.isAuthenticated,
                needsEmailVerification = authState.needsEmailVerification,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Login.path) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Route.Signup.path) },
                onNavigateToReset = { navController.navigate(Route.ResetPassword.path) },
                viewModel = authVm
            )
        }

        composable(Route.Signup.path) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Signup.path) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
                viewModel = authVm
            )
        }

        composable(Route.ResetPassword.path) {
            ResetPasswordScreen(
                onDone = { navController.popBackStack() },
                viewModel = authVm
            )
        }

        composable(Route.EmailVerification.path) {
            EmailVerificationScreen(
                onVerified = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSignOut = {
                    authVm.signOut()
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Main.path) {
            MainScreen(
                onSignOut = {
                    authVm.signOut()
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
