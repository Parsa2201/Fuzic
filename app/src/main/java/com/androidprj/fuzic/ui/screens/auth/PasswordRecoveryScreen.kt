package com.androidprj.fuzic.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.androidprj.fuzic.R
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun PasswordRecoveryScreen(
    email: String,
    isSubmitted: Boolean,
    onBackClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(
            title = stringResource(R.string.password_recovery_title),
            onBackClick = onBackClick,
        )
        if (isSubmitted) {
            ScreenMessage(
                icon = Icons.Default.Email,
                title = stringResource(R.string.password_recovery_title),
                message = stringResource(R.string.password_recovery_success),
            )
        } else {
            Column(Modifier.padding(androidx.compose.material3.MaterialTheme.spacing.medium)) {
                Text(stringResource(R.string.password_recovery_message))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_email_label)) },
                )
                Button(
                    onClick = onSubmitClick,
                    enabled = email.contains('@'),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.password_recovery_submit))
                }
            }
        }
    }
}
