package com.minicut.timer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors =
    lightColorScheme(
        primary = MintPrimary,
        onPrimary = SurfaceCard,
        primaryContainer = MintSoft,
        onPrimaryContainer = MintPrimaryDark,
        secondary = MintSecondary,
        onSecondary = SurfaceCard,
        secondaryContainer = SurfaceMuted,
        onSecondaryContainer = CalmText,
        tertiary = Warning,
        onTertiary = SurfaceCard,
        tertiaryContainer = Color(0xFFF1E1C5),
        onTertiaryContainer = CalmText,
        background = MistBackground,
        onBackground = CalmText,
        surface = SurfaceCard,
        onSurface = CalmText,
        surfaceVariant = SurfaceMuted,
        onSurfaceVariant = CalmTextMuted,
        outline = DividerSoft,
        outlineVariant = DividerSoft,
        surfaceTint = MintPrimary,
        inverseSurface = CalmText,
        inverseOnSurface = SurfaceCard,
        error = Danger,
        onError = SurfaceCard,
        errorContainer = Color(0xFFFFE2DB),
        onErrorContainer = Danger,
    )

private val DarkColors =
    darkColorScheme(
        primary = MintSoft,
        onPrimary = MintPrimaryDark,
        secondary = Color(0xFF9AB9B0),
        onSecondary = Color(0xFF0F1714),
        secondaryContainer = Color(0xFF21322B),
        onSecondaryContainer = Color(0xFFD7E4DD),
        tertiary = Color(0xFFF2C58A),
        onTertiary = Color(0xFF332109),
        tertiaryContainer = Color(0xFF47351C),
        onTertiaryContainer = Color(0xFFFFE7C7),
        background = Color(0xFF0F1513),
        onBackground = Color(0xFFE9F0EB),
        surface = Color(0xFF18201C),
        onSurface = Color(0xFFE9F0EB),
        surfaceVariant = Color(0xFF202A26),
        onSurfaceVariant = Color(0xFFC0CEC6),
        outline = Color(0xFF31413B),
        outlineVariant = Color(0xFF22302B),
        surfaceTint = MintSoft,
        inverseSurface = Color(0xFFE9F0EB),
        inverseOnSurface = Color(0xFF13211D),
        error = Color(0xFFFF8D77),
        onError = Color(0xFF4B150B),
        errorContainer = Color(0xFF6D2D21),
        onErrorContainer = Color(0xFFFFDAD3),
    )

private val AppShapes =
    Shapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(22.dp),
        large = RoundedCornerShape(28.dp),
    )

@Composable
fun MiniCutTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
