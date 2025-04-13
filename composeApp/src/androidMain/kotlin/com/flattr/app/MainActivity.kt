package com.flattr.app

import android.content.Intent
import android.graphics.Color // Import standard Android Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity // Keep this import if other parts use it, but we are changing the base class
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity // Ensure FragmentActivity is imported
import com.flattr.app.auth.AuthState
import com.flattr.app.auth.AuthTrigger
// Correct Imports for OAuth SDK v3.0.0
import com.truecaller.android.sdk.oAuth.CodeVerifierUtil
import com.truecaller.android.sdk.oAuth.TcOAuthCallback
import com.truecaller.android.sdk.oAuth.TcOAuthData
import com.truecaller.android.sdk.oAuth.TcOAuthError
import com.truecaller.android.sdk.oAuth.TcSdk
import com.truecaller.android.sdk.oAuth.TcSdkOptions
import java.math.BigInteger // For state generation example from docs
import java.security.SecureRandom

// Implement the correct callback and AuthTrigger
// Change base class from ComponentActivity to FragmentActivity
class MainActivity : FragmentActivity(), TcOAuthCallback, AuthTrigger {

    private val TAG = "MainActivityTruecallerOAuth"

    // Temporary storage for OAuth PKCE flow
    private var currentCodeVerifier: String? = null
    private var currentState: String? = null
    private var authState by mutableStateOf<AuthState>(AuthState.Initial)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SDK Initialization is now handled in FlattrApplication.kt
        // Remove the call to initTruecallerSDK()
        // initTruecallerSDK() 

        // Optional: Set this activity as the current callback receiver if needed,
        // although getAuthorizationCode(this) might handle this implicitly.
        // TcSdk.getInstance()?.setOAuthCallback(this)
        Log.d(TAG, "MainActivity onCreate - SDK should be initialized by Application class.")

        setContent {
            App(authTrigger = this, authState = authState)
        }
    }

    // Remove the entire initTruecallerSDK() function
    /*
    private fun initTruecallerSDK() {
        // Check if the SDK instance is available and usable, avoids re-initialization
        if (TcSdk.getInstance()?.isOAuthFlowUsable != true) {
            Log.d(TAG, "Initializing Truecaller OAuth SDK")
            // Use TcSdkOptions.Builder and TcOAuthCallback
            val tcSdkOptions = TcSdkOptions.Builder(this, this) // Pass Activity context and callback listener
                // Customization based on v3.0 docs:
                .loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_GET_STARTED)
                .ctaText(TcSdkOptions.CTA_TEXT_CONTINUE)
                .buttonShapeOptions(TcSdkOptions.BUTTON_SHAPE_ROUNDED)
                .footerType(TcSdkOptions.FOOTER_TYPE_SKIP)
//                .heading(TcSdkOptions.SDK_CONSENT_HEADING_VERIFY_YOUR_NO_WITH) // Use heading() with correct constant
                .sdkOptions(TcSdkOptions.OPTION_VERIFY_ONLY_TC_USERS) // Verify Truecaller users only
                .build()
            TcSdk.init(tcSdkOptions) // Initialize with TcSdk.init
        } else {
            Log.d(TAG, "Truecaller OAuth SDK already initialized and usable")
            // TcSdk.getInstance()?.setOAuthCallback(this) // Consider if needed
        }
    }
    */

    // --- AuthTrigger Implementation ---
    override fun startTruecallerLogin() {
        authState = AuthState.Loading
        val sdkInstance = TcSdk.getInstance()
        if (sdkInstance == null) {
            Log.e(TAG, "Truecaller SDK instance is null. Initialization might have failed in Application class.")
            authState = AuthState.Error("Failed to initialize Truecaller SDK")
            return
        }

        if (sdkInstance.isOAuthFlowUsable) {
            Log.d(TAG, "Starting Truecaller OAuth flow...")
            currentState = generateState()
            sdkInstance.setOAuthState(currentState!!)
            Log.d(TAG, "State generated: $currentState")

            val verifier = CodeVerifierUtil.generateRandomCodeVerifier()
            currentCodeVerifier = verifier

            if (verifier != null) {
                val codeChallenge = CodeVerifierUtil.getCodeChallenge(verifier)
                if (codeChallenge != null) {
                    sdkInstance.setCodeChallenge(codeChallenge)
                    Log.d(TAG, "Code Verifier generated.")
                    Log.d(TAG, "Code Challenge set: $codeChallenge")

                    val scopes = arrayOf("profile", "phone", "email")
                    sdkInstance.setOAuthScopes(scopes)
                    Log.d(TAG, "Scopes set: ${scopes.joinToString()}")

                    sdkInstance.getAuthorizationCode(this)
                } else {
                    Log.e(TAG, "Code challenge generation failed. Cannot proceed.")
                    clearOAuthState()
                    authState = AuthState.Error("Failed to generate code challenge")
                }
            } else {
                Log.e(TAG, "Code verifier generation failed. Cannot proceed.")
                clearOAuthState()
                authState = AuthState.Error("Failed to generate code verifier")
            }
        } else {
            Log.w(TAG, "Truecaller OAuth flow not usable (SDK not init, TC app missing, or other issue).")
            authState = AuthState.Error("Truecaller app is not installed or not ready")
        }
    }

    // --- TcOAuthCallback Implementation ---
    override fun onSuccess(tcOAuthData: TcOAuthData) {
        Log.i(TAG, "OAuth flow Success!")
        Log.d(TAG, "State received: ${tcOAuthData.state}")
        Log.d(TAG, "Scope received: ${tcOAuthData.scopesGranted?.joinToString()}")

        if (currentState == tcOAuthData.state) {
            Log.d(TAG, "State verification successful.")
            val authCode = tcOAuthData.authorizationCode
            Log.d(TAG, "Authorization Code: $authCode")

            if (authCode != null) {
                authState = AuthState.Success(authCode)
                Log.i(TAG, "OAuth flow completed successfully!")
            } else {
                authState = AuthState.Error("Received null authorization code")
                Log.e(TAG, "Auth code was null after successful flow!")
            }
        } else {
            authState = AuthState.Error("State mismatch error - potential security issue")
            Log.e(TAG, "State mismatch error! Potential CSRF attack. Received: ${tcOAuthData.state}, Expected: $currentState")
        }
        clearOAuthState()
    }

    override fun onFailure(tcOAuthError: TcOAuthError) {
        val errorMessage = when (tcOAuthError.errorCode) {
            40306 -> "Test mode error: Phone number not allowed in test mode"
            else -> "OAuth flow failed: ${tcOAuthError.errorMessage}"
        }
        authState = AuthState.Error(errorMessage)
        Log.e(TAG, "OAuth flow Failure: Code: ${tcOAuthError.errorCode}, Msg: ${tcOAuthError.errorMessage}")
        clearOAuthState()
    }

    // Added missing method required by TcOAuthCallback
    override fun onVerificationRequired(tcOAuthError: TcOAuthError?) {
        val errorMessage = "Verification required: ${tcOAuthError?.errorMessage ?: "Unknown error"}"
        authState = AuthState.Error(errorMessage)
        Log.w(TAG, "Verification required (non-TC flow / OTP flow). Error: ${tcOAuthError?.errorMessage}")
        clearOAuthState()
    }

    // --- Required for SDK to get result from Truecaller app activity ---
    @Suppress("DEPRECATION") // Suppress deprecation warning for onActivityResult
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult received: Req=$requestCode, Result=$resultCode")
        // Check if the result belongs to the Truecaller OAuth flow
        // Use the constant from TcSdk correctly
        if (requestCode == TcSdk.SHARE_PROFILE_REQUEST_CODE) {
            // Pass all parameters including requestCode.
            // No need to cast 'this' as it's already a FragmentActivity
            TcSdk.getInstance()?.onActivityResultObtained(this, requestCode, resultCode, data)
            Log.d(TAG, "onActivityResult forwarded to Truecaller SDK for processing.")
        } else {
            Log.d(TAG, "onActivityResult is not for Truecaller SDK (ReqCode: $requestCode)")
        }
    }

    // --- OAuth Helper Functions ---
    // Using example from docs for state generation
    private fun generateState(): String {
        return BigInteger(130, SecureRandom()).toString(32)
    }

    private fun clearOAuthState() {
        // Helper to clear temporary state
        currentState = null
        currentCodeVerifier = null
        Log.d(TAG, "Cleared temporary OAuth state.")
    }

    override fun updateAuthState(state: AuthState) {
        authState = state
    }
}

// Preview
@Preview
@Composable
fun AppAndroidPreview() {
    // Create a NoOpAuthTrigger instance specifically for the preview
    val previewAuthTrigger = object : AuthTrigger {
        override fun startTruecallerLogin() { Log.d("Preview", "startTruecallerLogin called") }
        override fun updateAuthState(state: AuthState) { Log.d("Preview", "updateAuthState called with $state") }
    }
    App(
        authTrigger = previewAuthTrigger,
        authState = AuthState.Initial
    )
}