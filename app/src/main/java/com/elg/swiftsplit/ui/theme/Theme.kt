package com.elg.swiftsplit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val SpeedrunDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
)

private val SpeedrunLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECDCFF),
    onSecondaryContainer = Color(0xFF24005A),
    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA0F0E4),
    onTertiaryContainer = Color(0xFF00201B),
    error = LightError,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
)

@Composable
fun SpeedrunTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpeedrunDarkColorScheme else SpeedrunLightColorScheme
    val speedrunColors = if (darkTheme) {
        SpeedrunColorScheme()
    } else {
        SpeedrunColorScheme(
            aheadGaining = LightAheadGaining,
            aheadLosing = LightAheadLosing,
            behindLosing = LightBehindLosing,
            behindGaining = LightBehindGaining,
            bestSegment = LightGoldSplit,
            bestSegmentBright = LightGoldSplit,
            timerText = LightTextPrimary,
            timerTextDim = LightTextSecondary,
            cardBackground = LightCard,
            cardBorder = LightBorder,
            elevatedSurface = LightSurface,
            deepBackground = LightBackground,
            success = LightAheadGaining,
            error = LightError,
            warning = WarningOrange,
            info = LightPrimary,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textTertiary = LightTextTertiary,
            textDisabled = LightTextDisabled
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (darkTheme) {
                window.statusBarColor = DeepBlack.toArgb()
                window.navigationBarColor = DeepBlack.toArgb()
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            } else {
                window.statusBarColor = LightBackground.toArgb()
                window.navigationBarColor = LightBackground.toArgb()
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    CompositionLocalProvider(
        LocalSpeedrunColors provides speedrunColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SpeedrunTypography,
            content = content
        )
    }
}


object SpeedrunThemeColors {
    val colors: SpeedrunColorScheme
        @Composable
        get() = LocalSpeedrunColors.current
}