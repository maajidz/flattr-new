package com.flattr.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.flattr.app.auth.AuthState // Import AuthState
import com.flattr.app.auth.AuthTrigger // Import AuthTrigger

// Define a NoOp trigger for Desktop
private object DesktopNoOpAuthTrigger : AuthTrigger {
    override fun startTruecallerLogin() { println("Desktop NoOp: startTruecallerLogin") }
    override fun updateAuthState(state: AuthState) { println("Desktop NoOp: updateAuthState: $state") }
}

fun main() = application {
    // Define portrait window size
    val windowState = rememberWindowState(size = DpSize(400.dp, 700.dp))

    Window(
        onCloseRequest = ::exitApplication,
        title = "Flattr", // Set window title
        state = windowState // Apply the defined state (size, position)
    ) {
        // Call the root App composable with the NoOp trigger and initial state
        App(authTrigger = DesktopNoOpAuthTrigger, authState = AuthState.Initial)
    }
}