package com.androidprj.fuzic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun previewArtworkUri(resourceId: Int): String {
    val packageName = LocalContext.current.packageName
    return "android.resource://$packageName/$resourceId"
}
