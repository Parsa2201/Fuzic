package com.androidprj.fuzic.model.ui

import androidx.annotation.StringRes

data class AuthUiState(
    val isSignUp: Boolean = false,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    @StringRes val confirmPasswordErrorRes: Int? = null,
)

data class WelcomeUiState(
    val page: Int = 0,
    val pageCount: Int = 3,
)
