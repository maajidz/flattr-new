package com.flattr.app

import com.flattr.app.auth.AuthState
import com.flattr.app.auth.AuthTrigger
// kotlinx.browser.window // Not used directly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.JsAny

// --- Configuration Constants ---
const val TRUECALLER_WEB_APP_KEY = "pkPJN4ffeb3d90a4a40ad80adf32217eafc83"
const val TRUECALLER_WEB_APP_NAME = "Flattr" // Or your desired display name
const val TRUECALLER_PRIVACY_POLICY_URL = "https://flattr.io/privacy" // Replace with actual URL
const val TRUECALLER_TERMS_URL = "https://flattr.io/tnc" // Replace with actual URL
// Added Callback URL constant - used in logging
const val TRUECALLER_CALLBACK_URL = "https://flattr.io/auth/true-sdk"

/**
 * JS Interop definitions for Truecaller Web SDK.
 */

@JsName("Object")
external interface TruecallerCallbackData {
    val status: String?
    val message: String?
    val accessToken: String?
    val requestNonce: String?
}

// Removed JsAuthTrigger interface as WebAuthTrigger now implements AuthTrigger directly
// public interface JsAuthTrigger : AuthTrigger {
//    fun setRequestNonce(nonce: String?)
// }

object TruecallerJsCallbackHandler {
    // Changed type from JsAuthTrigger? to AuthTrigger?
    private var authTrigger: AuthTrigger? = null
    private var currentRequestNonce: String? = null

    // Changed parameter type from JsAuthTrigger to AuthTrigger
    public fun initialize(trigger: AuthTrigger) {
        this.authTrigger = trigger
        // Removed trigger.setRequestNonce(null) as AuthTrigger doesn't have this method
        println("TruecallerJsCallbackHandler initialized.")
    }

    // This method is still needed to track the nonce for verification
    fun setCurrentNonce(nonce: String) {
        currentRequestNonce = nonce
        // No need to call setRequestNonce on trigger anymore
        println("Handler nonce set: $nonce")
    }

    fun handleTruecallerCallback(data: TruecallerCallbackData) {
        println("Kotlin handleTruecallerCallback received: status=${data.status}, nonce=${data.requestNonce}")
        val trigger = authTrigger ?: run {
            println("Error: AuthTrigger not initialized in TruecallerJsCallbackHandler")
            return
        }

        if (currentRequestNonce == null || currentRequestNonce != data.requestNonce) {
            println("Error: Nonce mismatch! Expected $currentRequestNonce, Received ${data.requestNonce}. Potential CSRF attack.")
            CoroutineScope(Dispatchers.Main).launch {
                trigger.updateAuthState(AuthState.Error("Security check failed (Nonce mismatch)."))
            }
            clearNonce()
            return
        }
        println("Nonce verification successful.")

        CoroutineScope(Dispatchers.Main).launch {
            when (data.status) {
                "success" -> {
                    val token = data.accessToken
                    if (token != null) {
                        println("Success! Access Token received client-side (needs backend validation): $token")
                        println("Your backend at $TRUECALLER_CALLBACK_URL should have received this token via POST.")
                        trigger.updateAuthState(AuthState.Success(token))
                    } else {
                         trigger.updateAuthState(AuthState.Error("Success callback missing access token."))
                    }
                }
                "failure" -> {
                    val errorMsg = data.message ?: "Unknown Truecaller Web SDK error"
                    trigger.updateAuthState(AuthState.Error(errorMsg))
                }
                "user_cancelled" -> {
                    trigger.updateAuthState(AuthState.Error("User cancelled Truecaller login."))
                }
                else -> {
                    trigger.updateAuthState(AuthState.Error("Unknown callback status: ${data.status}"))
                }
            }
        }
        clearNonce()
    }

    fun handleTruecallerScriptError() {
         println("Kotlin handleTruecallerScriptError called.")
         val trigger = authTrigger ?: return
         CoroutineScope(Dispatchers.Main).launch {
             trigger.updateAuthState(AuthState.Error("Failed to load Truecaller SDK script."))
         }
    }

    private fun clearNonce() {
        currentRequestNonce = null
        // No need to call setRequestNonce on trigger anymore
        println("Handler nonce cleared.")
    }
}

// --- Top-level Exported Functions ---

/**
 * This function is exported to JavaScript and will be called by the Truecaller SDK callback.
 * It delegates the call to the TruecallerJsCallbackHandler object.
 */
@JsExport
fun handleTruecallerCallbackExported(data: TruecallerCallbackData) {
    TruecallerJsCallbackHandler.handleTruecallerCallback(data)
}

/**
 * This function is exported to JavaScript and can be called if the Truecaller SDK script fails to load.
 * It delegates the call to the TruecallerJsCallbackHandler object.
 */
@JsExport
fun handleTruecallerScriptErrorExported() {
    TruecallerJsCallbackHandler.handleTruecallerScriptError()
} 