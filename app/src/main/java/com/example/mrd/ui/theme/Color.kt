package com.example.mrd.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Palette: "tomato, fresh teal and saffron on warm cream".
 *   primary   — tomato: appetite, calls to action, favourites
 *   secondary — teal: "open now", fresh/positive states, free delivery
 *   tertiary  — saffron/amber: ratings and gentle warnings (offline banner)
 * Neutrals are warm-tinted so surfaces feel like paper rather than clinical grey.
 * Every foreground/background pair below meets WCAG AA for normal text.
 */

internal val LightColors = lightColorScheme(
    primary = Color(0xFFD83A21),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF410A02),

    secondary = Color(0xFF17705B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC4EEDD),
    onSecondaryContainer = Color(0xFF002118),

    tertiary = Color(0xFF8F5400),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF2B1700),

    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF2A1A15),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF2A1A15),
    surfaceVariant = Color(0xFFF7E4DC),
    onSurfaceVariant = Color(0xFF59423B),
    surfaceTint = Color(0xFFD83A21),

    outline = Color(0xFF8C7168),
    outlineVariant = Color(0xFFDEC1B8),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EA),
    surfaceContainer = Color(0xFFFCEAE2),
    surfaceContainerHigh = Color(0xFFF6E4DB),
    surfaceContainerHighest = Color(0xFFF0DED5),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A5),
    onPrimary = Color(0xFF690B00),
    primaryContainer = Color(0xFF8F2411),
    onPrimaryContainer = Color(0xFFFFDAD3),

    secondary = Color(0xFF86D5BC),
    onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF005142),
    onSecondaryContainer = Color(0xFFC4EEDD),

    tertiary = Color(0xFFFFB95A),
    onTertiary = Color(0xFF492900),
    tertiaryContainer = Color(0xFF6A3C00),
    onTertiaryContainer = Color(0xFFFFDDB0),

    background = Color(0xFF18110E),
    onBackground = Color(0xFFF1DFD9),
    surface = Color(0xFF18110E),
    onSurface = Color(0xFFF1DFD9),
    surfaceVariant = Color(0xFF4D3A34),
    onSurfaceVariant = Color(0xFFDEC1B8),
    surfaceTint = Color(0xFFFFB4A5),

    outline = Color(0xFFA68A81),
    outlineVariant = Color(0xFF59423B),

    surfaceContainerLowest = Color(0xFF130C09),
    surfaceContainerLow = Color(0xFF211815),
    surfaceContainer = Color(0xFF261C18),
    surfaceContainerHigh = Color(0xFF312622),
    surfaceContainerHighest = Color(0xFF3C312C),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)
