package app.nearwave.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightScheme = lightColorScheme(
    primary = Palette.Teal600,
    onPrimary = Palette.White,
    primaryContainer = Palette.Teal100,
    onPrimaryContainer = Palette.Teal900,
    secondary = Palette.Ink700,
    onSecondary = Palette.White,
    secondaryContainer = Palette.Haze200,
    onSecondaryContainer = Palette.Ink900,
    tertiary = Palette.Amber500,
    onTertiary = Palette.Amber950,
    tertiaryContainer = Palette.Amber100,
    onTertiaryContainer = Palette.Amber950,
    error = Palette.Rose600,
    onError = Palette.White,
    errorContainer = Palette.Rose100,
    onErrorContainer = Palette.Rose900,
    background = Palette.Haze50,
    onBackground = Palette.Ink900,
    surface = Palette.Haze50,
    onSurface = Palette.Ink900,
    surfaceVariant = Palette.Haze200,
    onSurfaceVariant = Palette.Ink600,
    surfaceContainerLowest = Palette.White,
    surfaceContainerLow = Palette.White,
    surfaceContainer = Palette.Haze100,
    surfaceContainerHigh = Palette.Haze200,
    surfaceContainerHighest = Palette.Haze300,
    outline = Palette.Ink300,
    outlineVariant = Palette.Haze300,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.Teal300,
    onPrimary = Palette.Teal800,
    primaryContainer = Palette.Teal700,
    onPrimaryContainer = Palette.Teal100,
    secondary = Palette.Mist300,
    onSecondary = Palette.Night900,
    secondaryContainer = Palette.Night600,
    onSecondaryContainer = Palette.Mist100,
    tertiary = Palette.Amber400,
    onTertiary = Palette.Amber950,
    tertiaryContainer = Palette.Amber700,
    onTertiaryContainer = Palette.Amber950,
    error = Palette.Rose300,
    onError = Palette.Rose900,
    errorContainer = Palette.Rose900,
    onErrorContainer = Palette.Rose100,
    background = Palette.Night900,
    onBackground = Palette.Mist100,
    surface = Palette.Night900,
    onSurface = Palette.Mist100,
    surfaceVariant = Palette.Night700,
    onSurfaceVariant = Palette.Mist300,
    surfaceContainerLowest = Palette.Night950,
    surfaceContainerLow = Palette.Night800,
    surfaceContainer = Palette.Night800,
    surfaceContainerHigh = Palette.Night700,
    surfaceContainerHighest = Palette.Night600,
    outline = Palette.Night500,
    outlineVariant = Palette.Night600,
)

@Composable
fun NearwaveTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNwColors provides if (darkTheme) DarkNwColors else LightNwColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = NwTypography,
            shapes = NwShapes,
            content = content,
        )
    }
}

object NearwaveTheme {
    val colors: NwColors
        @Composable @ReadOnlyComposable get() = LocalNwColors.current
}
