package com.flattr.app

import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.flattr.app.auth.AuthState
import com.flattr.app.auth.AuthTrigger
import com.flattr.app.ui.screens.LoginScreen
import com.flattr.app.ui.screens.SplashScreen

// Define possible screens
private enum class Screen {
    Splash,
    Login
}

// Dummy implementation for non-Android platforms or previews
private object NoOpAuthTrigger : AuthTrigger {
    override fun startTruecallerLogin() { /* Do nothing */ }
    override fun updateAuthState(state: AuthState) { /* Do nothing */ }
}

@Composable
fun App(authTrigger: AuthTrigger, authState: AuthState) {
    var currentScreen by remember { mutableStateOf(Screen.Splash) }

    MaterialTheme { // Ensure MaterialTheme is applied at the root
        // Crossfade provides a smooth transition between screens
        Crossfade(targetState = currentScreen) { screen ->
            when (screen) {
                Screen.Splash -> SplashScreen(onTimeout = { currentScreen = Screen.Login })
                // Pass the authTrigger down to LoginScreen
                Screen.Login -> LoginScreen(authTrigger = authTrigger, authState = authState)
            }
        }
    }
}