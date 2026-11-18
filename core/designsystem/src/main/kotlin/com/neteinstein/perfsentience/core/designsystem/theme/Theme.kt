package com.neteinstein.perfsentience.core.designsystem.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.neteinstein.perfsentience.core.model.Severity
import com.neteinstein.perfsentience.core.model.ThemeMode
import com.neteinstein.perfsentience.core.model.Verdict

private val LightColors = lightColorScheme(
    primary = Violet40,
    onPrimary = Color.White,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    tertiary = Coral40,
    onTertiary = Color.White,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Coral10,
    error = Red40,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Ink98,
    onBackground = Ink10,
    surface = Ink98,
    onSurface = Ink10,
    surfaceVariant = Ink90,
    onSurfaceVariant = Ink30,
    surfaceContainerLowest = Ink99,
    surfaceContainerLow = Ink95,
    surfaceContainer = Color(0xFFEFEAFB),
    surfaceContainerHigh = Color(0xFFE9E4F6),
    surfaceContainerHighest = Ink90,
    outline = Ink50,
    outlineVariant = Ink80,
)

private val DarkColors = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet20,
    primaryContainer = Violet30,
    onPrimaryContainer = Violet90,
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    tertiary = Coral80,
    onTertiary = Coral20,
    tertiaryContainer = Coral30,
    onTertiaryContainer = Coral90,
    error = Red80,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Red90,
    background = Ink5,
    onBackground = Ink90,
    surface = Ink5,
    onSurface = Ink90,
    surfaceVariant = Ink30,
    onSurfaceVariant = Ink80,
    surfaceContainerLowest = Color(0xFF07060E),
    surfaceContainerLow = Ink10,
    surfaceContainer = Ink15,
    surfaceContainerHigh = Ink20,
    surfaceContainerHighest = Ink30,
    outline = Ink60,
    outlineVariant = Ink30,
)

/** Semantic colours not covered by Material's [ColorScheme]. */
@Immutable
data class StatusColors(
    val good: Color,
    val fair: Color,
    val poor: Color,
) {
    fun forVerdict(verdict: Verdict): Color = when (verdict) {
        Verdict.GOOD -> good
        Verdict.FAIR -> fair
        Verdict.POOR -> poor
    }

    fun forSeverity(severity: Severity): Color = when (severity) {
        Severity.LOW -> good
        Severity.MEDIUM -> fair
        Severity.HIGH -> poor
    }
}

private val LightStatus = StatusColors(good = GoodLight, fair = FairLight, poor = PoorLight)
private val DarkStatus = StatusColors(good = GoodDark, fair = FairDark, poor = PoorDark)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

object PerfsentienceTheme {
    val status: StatusColors
        @Composable get() = LocalStatusColors.current

    val isDark: Boolean
        @Composable get() = LocalIsDarkTheme.current
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun PerfsentienceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val target = when {
        dynamicColor ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors

        else -> LightColors
    }
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus,
        LocalIsDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = target.animated(),
            typography = PerfsentienceTypography,
            shapes = PerfsentienceShapes,
            content = content,
        )
    }
}

/** Cross-fades the key colours so switching light/dark feels smooth instead of flashing. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(durationMillis = 450)
    val primary by animateColorAsState(primary, spec, label = "primary")
    val background by animateColorAsState(background, spec, label = "background")
    val surface by animateColorAsState(surface, spec, label = "surface")
    val onSurface by animateColorAsState(onSurface, spec, label = "onSurface")
    val onBackground by animateColorAsState(onBackground, spec, label = "onBackground")
    val surfaceContainer by animateColorAsState(surfaceContainer, spec, label = "surfaceContainer")
    val surfaceContainerHigh by animateColorAsState(surfaceContainerHigh, spec, label = "surfaceContainerHigh")
    val surfaceContainerLow by animateColorAsState(surfaceContainerLow, spec, label = "surfaceContainerLow")
    return copy(
        primary = primary,
        background = background,
        surface = surface,
        onSurface = onSurface,
        onBackground = onBackground,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerLow = surfaceContainerLow,
    )
}
