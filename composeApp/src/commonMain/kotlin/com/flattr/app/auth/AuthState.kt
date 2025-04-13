package com.flattr.app.auth

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val authCode: String) : AuthState()
    data class Error(val message: String) : AuthState()
} 