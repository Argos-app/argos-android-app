package com.android.argosapp.ui.screen.login

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val rememberMe: Boolean = true,
    val passwordVisible: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
)
