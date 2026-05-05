package com.minicut.timer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DataCoachLightColors =
    lightColorScheme(
        primary = Color(0xFF0F6B43),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8FFE7),
        onPrimaryContainer = Color(0xFF052111),
        secondary = Color(0xFF335F9F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE8FF),
        onSecondaryContainer = Color(0xFF071D3A),
        tertiary = Color(0xFF9A5A14),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFE3C0),
        onTertiaryContainer = Color(0xFF2D1600),
        background = Color(0xFFF4F6F8),
        onBackground = Color(0xFF101318),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF101318),
        surfaceVariant = Color(0xFFE9EEF5),
        onSurfaceVariant = Color(0xFF4F5B6B),
        outline = Color(0xFFC8D0DA),
        outlineVariant = Color(0xFFDDE3EA),
        surfaceTint = Color(0xFF0F6B43),
        inverseSurface = DataSurface,
        inverseOnSurface = DataText,
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )

private val DataCoachDarkColors =
    darkColorScheme(
        primary = DataMint,
        onPrimary = Color(0xFF032113),
        primaryContainer = DataMintContainer,
        onPrimaryContainer = Color(0xFFDFFFEA),
        secondary = DataBlue,
        onSecondary = Color(0xFF061229),
        secondaryContainer = DataBlueContainer,
        onSecondaryContainer = Color(0xFFD9E7FF),
        tertiary = DataAmber,
        onTertiary = Color(0xFF2F1700),
        tertiaryContainer = DataAmberContainer,
        onTertiaryContainer = Color(0xFFFFE3C0),
        background = DataBlack,
        onBackground = DataText,
        surface = DataSurface,
        onSurface = DataText,
        surfaceVariant = DataSurfaceMuted,
        onSurfaceVariant = DataTextMuted,
        outline = DataOutline,
        outlineVariant = DataOutlineMuted,
        surfaceTint = DataMint,
        inverseSurface = Color(0xFFE9EEF5),
        inverseOnSurface = Color(0xFF101318),
        error = DataDanger,
        onError = Color(0xFF330808),
        errorContainer = DataDangerContainer,
        onErrorContainer = Color(0xFFFFDADA),
        scrim = Color(0xCC000000),
    )

private val AppShapes =
    Shapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(22.dp),
        large = RoundedCornerShape(28.dp),
    )

@Composable
fun MiniCutTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DataCoachDarkColors else DataCoachLightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
