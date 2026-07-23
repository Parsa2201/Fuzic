package com.androidprj.fuzic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.androidprj.fuzic.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.ui.theme.FuzicTheme

/** Shows the supplied logo variant that contrasts with the current app background. */
@Composable
fun FuzicLogo(
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val logoAsset = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        DarkLogoAsset
    } else {
        LightLogoAsset
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/$logoAsset")
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

private const val DarkLogoAsset = "fuzic_logo_dark.svg"
private const val LightLogoAsset = "fuzic_logo_light.svg"

@Preview(name = "Fuzic logo - light theme", showBackground = true)
@Composable
private fun FuzicLogoLightPreview() {
    FuzicTheme(darkTheme = false) {
        FuzicLogo(
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(96.dp),
        )
    }
}

@Preview(name = "Fuzic logo - dark theme", showBackground = true)
@Composable
private fun FuzicLogoDarkPreview() {
    FuzicTheme(darkTheme = true) {
        FuzicLogo(
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(96.dp),
        )
    }
}
