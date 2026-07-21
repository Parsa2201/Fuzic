package com.androidprj.fuzic.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.AuthUiState
import com.androidprj.fuzic.model.WelcomeUiState
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun WelcomeRoute(
    uiState: WelcomeUiState,
    onPageChanged: (Int) -> Unit,
    onSkipClick: () -> Unit,
    onNextClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WelcomeScreen(uiState, onPageChanged, onSkipClick, onNextClick, onStartClick, modifier)
}

@Composable
fun WelcomeScreen(
    uiState: WelcomeUiState,
    onPageChanged: (Int) -> Unit,
    onSkipClick: () -> Unit,
    onNextClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.page.coerceIn(0, uiState.pageCount - 1),
        pageCount = { uiState.pageCount },
    )
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(
            onClick = onSkipClick,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.auth_welcome_skip))
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            WelcomePage(page = page)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            repeat(uiState.pageCount) { page ->
                val color = if (page == pagerState.currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                Spacer(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, MaterialTheme.shapes.small),
                )
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        Button(
            onClick = if (pagerState.currentPage == uiState.pageCount - 1) onStartClick else onNextClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (pagerState.currentPage == uiState.pageCount - 1) {
                        R.string.auth_welcome_start
                    } else {
                        R.string.auth_welcome_next
                    },
                ),
            )
        }
    }
}

@Composable
private fun WelcomePage(
    page: Int,
) {
    val content = when (page) {
        0 -> R.string.auth_welcome_page_one_title to R.string.auth_welcome_page_one_message
        1 -> R.string.auth_welcome_page_two_title to R.string.auth_welcome_page_two_message
        else -> R.string.auth_welcome_page_three_title to R.string.auth_welcome_page_three_message
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(112.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.large))
        Text(
            text = stringResource(content.first),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = stringResource(content.second),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AuthRoute(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityClick: () -> Unit,
    onConfirmPasswordVisibilityClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSwitchModeClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreen(
        uiState,
        onEmailChange,
        onPasswordChange,
        onConfirmPasswordChange,
        onPasswordVisibilityClick,
        onConfirmPasswordVisibilityClick,
        onSubmitClick,
        onForgotPasswordClick,
        onSwitchModeClick,
        onRetryClick,
        modifier,
    )
}

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityClick: () -> Unit,
    onConfirmPasswordVisibilityClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSwitchModeClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = stringResource(if (uiState.isSignUp) R.string.auth_sign_up_title else R.string.auth_sign_in_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.large))
        if (uiState.errorMessage != null) {
            ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.auth_error_title),
                message = uiState.errorMessage,
                fillMaxSize = false,
                action = {
                    TextButton(onClick = onRetryClick) {
                        Text(stringResource(R.string.action_retry))
                    }
                },
            )
        } else {
            AuthForm(
                uiState = uiState,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange,
                onPasswordVisibilityClick = onPasswordVisibilityClick,
                onConfirmPasswordVisibilityClick = onConfirmPasswordVisibilityClick,
                onSubmitClick = onSubmitClick,
                onForgotPasswordClick = onForgotPasswordClick,
                onSwitchModeClick = onSwitchModeClick,
            )
        }
        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.medium),
            )
        }
    }
}

@Composable
private fun AuthForm(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityClick: () -> Unit,
    onConfirmPasswordVisibilityClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSwitchModeClick: () -> Unit,
) {
    OutlinedTextField(
        value = uiState.email,
        onValueChange = onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.auth_email_label)) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        isError = uiState.emailError != null,
        supportingText = uiState.emailError?.let { { Text(it) } },
        singleLine = true,
    )
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    PasswordField(
        value = uiState.password,
        onValueChange = onPasswordChange,
        label = stringResource(R.string.auth_password_label),
        isVisible = uiState.isPasswordVisible,
        errorMessage = uiState.passwordError,
        onVisibilityClick = onPasswordVisibilityClick,
    )
    if (uiState.isSignUp) {
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        PasswordField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.auth_confirm_password_label),
            isVisible = uiState.isConfirmPasswordVisible,
            errorMessage = uiState.confirmPasswordError,
            onVisibilityClick = onConfirmPasswordVisibilityClick,
        )
    } else {
        TextButton(
            onClick = onForgotPasswordClick,
        ) {
            Text(stringResource(R.string.auth_forgot_password))
        }
    }
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    Button(onClick = onSubmitClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(if (uiState.isSignUp) R.string.auth_sign_up else R.string.auth_sign_in))
    }
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    TextButton(onClick = onSwitchModeClick) {
        Text(stringResource(if (uiState.isSignUp) R.string.auth_switch_to_sign_in else R.string.auth_switch_to_sign_up))
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    errorMessage: String?,
    onVisibilityClick: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onVisibilityClick) {
                Icon(
                    if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (isVisible) R.string.auth_password_hide else R.string.auth_password_show,
                    ),
                )
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it) } },
        singleLine = true,
    )
}

@Preview(name = "Welcome - English", showBackground = true)
@Composable
private fun WelcomePreview() {
    FuzicTheme {
        WelcomeScreen(WelcomeUiState(page = 0), {}, {}, {}, {})
    }
}

@Preview(name = "Welcome Persian final page", locale = "fa", showBackground = true)
@Composable
private fun WelcomePersianPreview() {
    FuzicTheme {
        WelcomeScreen(WelcomeUiState(page = 2), {}, {}, {}, {})
    }
}

@Preview(name = "Sign in", showBackground = true)
@Composable
private fun SignInPreview() {
    FuzicTheme {
        AuthScreen(
            uiState = AuthUiState(email = "parsa@example.com"),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPasswordVisibilityClick = {},
            onConfirmPasswordVisibilityClick = {},
            onSubmitClick = {},
            onForgotPasswordClick = {},
            onSwitchModeClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Sign up Persian validation", locale = "fa", showBackground = true)
@Composable
private fun SignUpValidationPreview() {
    FuzicTheme {
        AuthScreen(
            AuthUiState(
                isSignUp = true,
                email = "bad",
                password = "123",
                confirmPassword = "456",
                emailError = stringResource(R.string.auth_email_error),
                passwordError = stringResource(R.string.auth_password_error),
                confirmPasswordError = stringResource(R.string.auth_confirm_password_error),
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPasswordVisibilityClick = {},
            onConfirmPasswordVisibilityClick = {},
            onSubmitClick = {},
            onForgotPasswordClick = {},
            onSwitchModeClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Sign in loading Persian", locale = "fa", showBackground = true)
@Composable
private fun SignInLoadingPreview() {
    FuzicTheme {
        AuthScreen(
            uiState = AuthUiState(isLoading = true),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPasswordVisibilityClick = {},
            onConfirmPasswordVisibilityClick = {},
            onSubmitClick = {},
            onForgotPasswordClick = {},
            onSwitchModeClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Sign in error Persian", locale = "fa", showBackground = true)
@Composable
private fun SignInErrorPreview() {
    FuzicTheme {
        AuthScreen(
            AuthUiState(errorMessage = stringResource(R.string.auth_error_message)),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPasswordVisibilityClick = {},
            onConfirmPasswordVisibilityClick = {},
            onSubmitClick = {},
            onForgotPasswordClick = {},
            onSwitchModeClick = {},
            onRetryClick = {},
        )
    }
}
