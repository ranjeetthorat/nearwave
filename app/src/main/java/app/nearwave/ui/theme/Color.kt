package app.nearwave.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Nearwave palette. Inspired by handheld two-way radios: a calm teal "channel" colour for
 * everything connected, and a high-visibility signal amber reserved for one thing only,
 * the talk button. Rose is reserved for buzzes. Backgrounds are a cool haze, never cream.
 */
internal object Palette {
    // Ink (text) and Haze (light surfaces)
    val Ink900 = Color(0xFF12302B)
    val Ink700 = Color(0xFF2C4A45)
    val Ink600 = Color(0xFF47625D)
    val Ink300 = Color(0xFF8FA39E)
    val Haze50 = Color(0xFFF1F4F1)
    val Haze100 = Color(0xFFE7ECE8)
    val Haze200 = Color(0xFFDCE4DF)
    val Haze300 = Color(0xFFC7D3CD)
    val White = Color(0xFFFFFFFF)

    // Night (dark surfaces) and Mist (dark-theme text)
    val Night950 = Color(0xFF0B1816)
    val Night900 = Color(0xFF0E1E1C)
    val Night800 = Color(0xFF132624)
    val Night700 = Color(0xFF1B302D)
    val Night600 = Color(0xFF24403C)
    val Night500 = Color(0xFF34524D)
    val Mist100 = Color(0xFFE2ECE8)
    val Mist300 = Color(0xFFA7BAB4)

    // Channel teal
    val Teal900 = Color(0xFF00201B)
    val Teal600 = Color(0xFF0D6B5F)
    val Teal300 = Color(0xFF63C9B8)
    val Teal100 = Color(0xFFC5EDE5)
    val Teal800 = Color(0xFF00382F)
    val Teal700 = Color(0xFF0B4F46)

    // Signal amber (talk)
    val Amber950 = Color(0xFF2B2000)
    val Amber700 = Color(0xFFC78F00)
    val Amber500 = Color(0xFFF4B400)
    val Amber400 = Color(0xFFFFC52E)
    val Amber100 = Color(0xFFFFE9A8)

    // Buzz rose
    val Rose900 = Color(0xFF3F0011)
    val Rose600 = Color(0xFFC2304A)
    val Rose300 = Color(0xFFFF8A9B)
    val Rose100 = Color(0xFFFFD9DE)

    /** Avatar fills. Mid-tones that hold white initials in both themes. */
    val Avatars = listOf(
        Color(0xFF2F7D6D), // teal
        Color(0xFF6D55A3), // violet
        Color(0xFF3D6FB6), // blue
        Color(0xFF8A6D10), // olive gold
        Color(0xFFA83A62), // berry
        Color(0xFF3F7F3A), // green
    )
}

/** Colours Material 3 has no slot for. Read with `NearwaveTheme.colors`. */
@Immutable
data class NwColors(
    val talk: Color,
    val talkPressed: Color,
    val onTalk: Color,
    val talkDisabled: Color,
    val onTalkDisabled: Color,
    val buzz: Color,
    val onBuzz: Color,
    val buzzContainer: Color,
    val onBuzzContainer: Color,
    val radarSurface: Color,
    val radarRing: Color,
    val radarSweep: Color,
    val connected: Color,
    val offline: Color,
)

internal val LightNwColors = NwColors(
    talk = Palette.Amber500,
    talkPressed = Palette.Amber700,
    onTalk = Palette.Amber950,
    talkDisabled = Palette.Haze200,
    onTalkDisabled = Palette.Ink300,
    buzz = Palette.Rose600,
    onBuzz = Palette.White,
    buzzContainer = Palette.Rose100,
    onBuzzContainer = Palette.Rose900,
    radarSurface = Palette.White,
    radarRing = Palette.Haze300,
    radarSweep = Palette.Teal600,
    connected = Palette.Teal600,
    offline = Palette.Ink300,
)

internal val DarkNwColors = NwColors(
    talk = Palette.Amber400,
    talkPressed = Palette.Amber500,
    onTalk = Palette.Amber950,
    talkDisabled = Palette.Night600,
    onTalkDisabled = Palette.Mist300,
    buzz = Palette.Rose300,
    onBuzz = Palette.Rose900,
    buzzContainer = Palette.Rose900,
    onBuzzContainer = Palette.Rose100,
    radarSurface = Palette.Night800,
    radarRing = Palette.Night500,
    radarSweep = Palette.Teal300,
    connected = Palette.Teal300,
    offline = Palette.Mist300,
)

internal val LocalNwColors = staticCompositionLocalOf { LightNwColors }
