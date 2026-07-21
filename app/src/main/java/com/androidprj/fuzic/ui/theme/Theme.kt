package com.androidprj.fuzic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = MidnightVioletDark,
    onPrimary = OnDarkAccent,
    primaryContainer = MidnightViolet,
    onPrimaryContainer = OnDarkAccent,
    secondary = ElectricTealDark,
    onSecondary = OnDarkAccent,
    secondaryContainer = ElectricTeal,
    onSecondaryContainer = OnDarkAccent,
    tertiary = PremiumGoldDark,
    onTertiary = OnGold,
    tertiaryContainer = PremiumGold,
    onTertiaryContainer = OnGold,
    error = MidnightErrorDark,
    onError = OnDarkAccent,
    background = MidnightBackgroundDark,
    onBackground = MidnightOnDark,
    surface = MidnightSurfaceDark,
    onSurface = MidnightOnDark,
    surfaceVariant = MidnightSurfaceVariantDark,
    onSurfaceVariant = OnMidnightSurfaceVariantDark,
    outline = MidnightOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = MidnightVioletLight,
    onPrimary = OnDarkAccent,
    primaryContainer = MidnightVioletContainerLight,
    onPrimaryContainer = OnMidnightVioletContainerLight,
    secondary = ElectricTealLight,
    onSecondary = OnDarkAccent,
    secondaryContainer = ElectricTealContainerLight,
    onSecondaryContainer = OnElectricTealContainerLight,
    tertiary = PremiumGoldLight,
    onTertiary = OnDarkAccent,
    tertiaryContainer = PremiumGoldContainerLight,
    onTertiaryContainer = OnPremiumGoldContainerLight,
    error = MidnightErrorLight,
    onError = OnDarkAccent,
    background = MidnightBackgroundLight,
    onBackground = MidnightOnLight,
    surface = MidnightSurfaceLight,
    onSurface = MidnightOnLight,
    surfaceVariant = MidnightSurfaceVariantLight,
    onSurfaceVariant = OnMidnightSurfaceVariantLight,
    outline = MidnightOutlineLight
)

@Composable
fun FuzicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalFuzicSpacing provides FuzicSpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                PersianTypography
            } else {
                Typography
            },
            shapes = FuzicShapes,
            content = content
        )
    }
}
