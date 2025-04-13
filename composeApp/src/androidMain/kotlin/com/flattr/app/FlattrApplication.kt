package com.flattr.app

import android.app.Application
import android.util.Log
// Remove incorrect imports
// import com.truecaller.android.sdk.ITrueCallback
// import com.truecaller.android.sdk.TrueError
// import com.truecaller.android.sdk.TrueProfile
// Correct imports for OAuth v3.0.0
import com.truecaller.android.sdk.oAuth.TcOAuthCallback
import com.truecaller.android.sdk.oAuth.TcOAuthData
import com.truecaller.android.sdk.oAuth.TcOAuthError
import com.truecaller.android.sdk.oAuth.TcSdk
import com.truecaller.android.sdk.oAuth.TcSdkOptions

class FlattrApplication : Application() {

    private val TAG = "FlattrApplication"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FlattrApplication onCreate")

        // Define SDK options using TcOAuthCallback
        val sdkOptions = TcSdkOptions.Builder(this, object : TcOAuthCallback { // Implement correct callback
            override fun onSuccess(data: TcOAuthData) {
                // Minimal handling in Application class.
                // Logic should primarily be in the Activity that initiates the flow.
                Log.i(TAG, "[AppCallback] OAuth Success. State: ${data.state}, AuthCode: ${data.authorizationCode}")
            }

            override fun onFailure(error: TcOAuthError) {
                // Minimal handling in Application class.
                Log.e(TAG, "[AppCallback] OAuth Failure: ${error.errorCode} - ${error.errorMessage}")
            }

            override fun onVerificationRequired(error: TcOAuthError?) {
                // Minimal handling in Application class.
                Log.w(TAG, "[AppCallback] Verification Required: ${error?.errorMessage}")
            }

        })
            // The clientId is read from AndroidManifest.xml meta-data, not set here.
            // .clientId(getString(R.string.clientID)) 
            .build()

        // Initialize the SDK
        try {
            TcSdk.init(sdkOptions)
            Log.i(TAG, "Truecaller SDK Initialized successfully in Application")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Truecaller SDK: ${e.message}")
            // Log the full stack trace for detailed debugging
            e.printStackTrace()
        }
    }
} 