package com.androidprj.fuzic.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.ui.theme.FuzicTheme

@Composable
fun SectionHeader(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    onViewAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onViewAllClick != null) {
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = stringResource(R.string.action_view_all),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview(name = "Section header - English", showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    FuzicTheme {
        SectionHeader(
            titleRes = R.string.home_section_most_popular,
            onViewAllClick = { }
        )
    }
}

@Preview(name = "Section header - Persian", locale = "fa", showBackground = true)
@Composable
private fun SectionHeaderPersianPreview() {
    FuzicTheme {
        SectionHeader(
            titleRes = R.string.home_section_most_popular,
            onViewAllClick = { }
        )
    }
}
