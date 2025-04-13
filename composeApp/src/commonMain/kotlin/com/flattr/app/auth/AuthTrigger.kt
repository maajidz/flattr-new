package com.flattr.app.auth

/**
 * Interface to trigger platform-specific authentication flows from common code.
 */
interface AuthTrigger {
    fun startTruecallerLogin()
    fun updateAuthState(state: AuthState)
} 