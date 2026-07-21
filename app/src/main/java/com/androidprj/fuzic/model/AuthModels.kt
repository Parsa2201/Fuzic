package com.androidprj.fuzic.model

data class AuthUiState(
    val isSignUp: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
)

data class WelcomeUiState(
    val page: Int = 0,
    val pageCount: Int = 3,
)
