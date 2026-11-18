package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val NatureAuraDarkScheme = darkColorScheme(
    primary = CeladonAccent,
    onPrimary = SanctuaryVoid,
    primaryContainer = CeladonDeep,
    onPrimaryContainer = IvoryPrimary,
    secondary = WarmAmber,
    onSecondary = SanctuaryVoid,
    secondaryContainer = AmberDeep,
    onSecondaryContainer = IvoryPrimary,
    tertiary = ChakraCrown,
    onTertiary = SanctuaryVoid,
    background = SanctuaryVoid,
    onBackground = IvoryPrimary,
    surface = SanctuaryForest,
    onSurface = IvoryPrimary,
    surfaceVariant = SanctuarySurface,
    onSurfaceVariant = SageMuted,
    surfaceContainerLow = SanctuaryForest,
    surfaceContainer = SanctuarySurface,
    surfaceContainerHigh = SanctuarySurfaceHigh,
    outline = SanctuaryBorder,
    outlineVariant = SanctuaryBorder,
    error = CoralAlert,
    onError = SanctuaryVoid,
)

@Composable
fun NatureAuraTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(LocalContext.current)
        } else {
            NatureAuraDarkScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NatureAuraTypography,
        content = content,
    )
}
