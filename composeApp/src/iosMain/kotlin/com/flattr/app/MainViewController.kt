package com.flattr.app

import androidx.compose.ui.window.ComposeUIViewController
import com.flattr.app.auth.AuthState

// Get access to the NoOpAuthTrigger defined in commonMain
// Note: This assumes NoOpAuthTrigger is public or internal in commonMain/App.kt
// If it's private, we need to make it internal or create a new one here.
// For now, let's assume it's accessible or redefine it if needed.

// Re-define NoOpAuthTrigger if it's private in commonMain/App.kt
private object IosNoOpAuthTrigger : com.flattr.app.auth.AuthTrigger {
    override fun startTruecallerLogin() { println("iOS NoOp: startTruecallerLogin") }
    override fun updateAuthState(state: AuthState) { println("iOS NoOp: updateAuthState: $state") }
}

fun MainViewController() = ComposeUIViewController { 
    // Provide the NoOp trigger and initial state
    App(authTrigger = IosNoOpAuthTrigger, authState = AuthState.Initial)
}