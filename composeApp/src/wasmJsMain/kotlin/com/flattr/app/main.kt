package com.flattr.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.flattr.app.auth.AuthState
import com.flattr.app.auth.AuthTrigger
import kotlinx.browser.document
// kotlinx.browser.window // Not used directly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// kotlinx.coroutines.suspendCancellableCoroutine // Not needed if startTruecallerLogin is not suspend
// kotlin.coroutines.resume // Not needed
// kotlin.coroutines.resumeWithException // Not needed
import kotlin.random.Random
// kotlin.js.Promise // Not used directly
import kotlin.js.JsAny
// Removed JsFunction import
import kotlinx.js.jso // Added import for jso builder

// External JavaScript functions for Truecaller SDK
@JsName("triggerTruecallerLogin")
external fun triggerTruecallerLoginExternal(options: JsAny)

@JsName("isTruecallerScriptLoaded")
external fun isTruecallerScriptLoadedExternal(): Boolean

// Removed jsConsoleLog helper function

// Refactored to use jso builder
fun buildTruecallerOptionsJsObject(nonce: String): JsAny {
    // Constants needed
    val partnerKey = TRUECALLER_WEB_APP_KEY
    val partnerName = TRUECALLER_WEB_APP_NAME
    val privacyUrl = TRUECALLER_PRIVACY_POLICY_URL
    val termsUrl = TRUECALLER_TERMS_URL
    val customDomain = "https://flattr.io"

    // Use jso builder to create the object
    return jso {
        this.requestNonce = nonce
        this.callback = ::handleTruecallerCallbackExported // Assign function reference directly
        this.partnerKey = partnerKey
        this.partnerName = partnerName
        this.lang = "en"
        this.privacyUrl = privacyUrl
        this.termsUrl = termsUrl
        this.loginHint = "" // Empty string
        this.consentTitle = "Login with Truecaller"
        this.ctaText = "continue"
        this.buttonColor = "#4285F4"
        this.buttonTextColor = "#FFFFFF"
        // Create nested object for sdkOptions
        this.sdkOptions = jso {
            this.customDomain = customDomain
        }
    }
}

// Define the WebAuthTrigger class
class WebAuthTrigger : AuthTrigger {
    // Removed 'override' as AuthTrigger likely doesn't define 'state'
    var state by mutableStateOf<AuthState>(AuthState.Initial)
        private set

    private var currentRequestNonce: String? = null

    // Keep override assuming AuthTrigger defines this method
    override fun updateAuthState(newState: AuthState) {
        state = newState
        if (newState is AuthState.Success || newState is AuthState.Error) {
            currentRequestNonce = null
        }
        println("WebAuthTrigger state updated: $newState")
    }

    fun setRequestNonce(nonce: String?) {
        this.currentRequestNonce = nonce
        if (nonce != null) {
             TruecallerJsCallbackHandler.setCurrentNonce(nonce)
        }
        println("Nonce set/cleared: $nonce")
    }

    // Keep override assuming AuthTrigger defines this method
    override fun startTruecallerLogin() {
        CoroutineScope(Dispatchers.Main).launch {
            updateAuthState(AuthState.Loading)
            println("WebAuthTrigger: startTruecallerLogin called (non-suspend)")

            if (!isTruecallerScriptLoadedExternal()) {
                println("Error: Truecaller script not loaded.")
                updateAuthState(AuthState.Error("Truecaller SDK not available."))
                return@launch
            }

            val nonce = generateNonce()
            setRequestNonce(nonce)
            println("Generated nonce: $nonce")

            // Call the refactored top-level builder function
            val options = buildTruecallerOptionsJsObject(nonce)

            // Use standard println for logging the options object
            // Note: Output might be less detailed than browser console.log
            println("Calling triggerTruecallerLoginExternal with options: $options")

            try {
                triggerTruecallerLoginExternal(options)
                println("triggerTruecallerLoginExternal invoked.")
            } catch (e: Throwable) {
                // Catch potential exceptions during the external call
                println("Error calling triggerTruecallerLoginExternal: ${e.message}")
                // Use dynamic cast for JS exceptions if needed, otherwise Throwable is fine
                val errorMessage = (e as? JsAny)?.toString() ?: e.message ?: "Unknown error"
                updateAuthState(AuthState.Error("Failed to initiate Truecaller login: $errorMessage"))
                setRequestNonce(null) // Clear nonce on failure
            }
        }
    }

    private fun generateNonce(length: Int = 16): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val authTrigger = WebAuthTrigger()
    TruecallerJsCallbackHandler.initialize(authTrigger)

    // Use CanvasBasedWindow instead of ComposeViewport
    CanvasBasedWindow(canvasElementId = "ComposeTarget") { // Provide an ID for the canvas
        App(authTrigger = authTrigger, authState = authTrigger.state)
    }
}