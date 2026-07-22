package com.androidprj.fuzic.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.ui.theme.FuzicTheme

@Composable
fun Modifier.fuzicShimmer(shape: Shape = MaterialTheme.shapes.medium): Modifier {
    val transition = rememberInfiniteTransition(label = "fuzicShimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -320f,
        targetValue = 960f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "fuzicShimmerOffset"
    )
    val colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    )

    return clip(shape).background(
        brush = Brush.linearGradient(
            colors = colors,
        start = Offset(shimmerOffset - 320f, 0f),
        end = Offset(shimmerOffset, 0f)
        )
    )
}

@Preview(name = "Shimmer block", showBackground = true)
@Composable
private fun FuzicShimmerPreview() {
    FuzicTheme {
        Box(
            modifier = Modifier
                .size(width = 180.dp, height = 96.dp)
                .fuzicShimmer()
        )
    }
}
