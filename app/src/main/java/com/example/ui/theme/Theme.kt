package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CardScanLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = PureWhite,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = CobaltOnContainer,
    secondary = CobaltPrimaryLight,
    onSecondary = PureWhite,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = InkPrimary,
    tertiary = MintBadgeText,
    onTertiary = PureWhite,
    tertiaryContainer = MintBadgeBg,
    onTertiaryContainer = MintBadgeText,
    background = PureWhite,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SoftCanvas,
    surfaceContainer = SurfaceMuted,
    surfaceContainerHigh = SurfaceContainerHigh,
    outline = OutlineSubtle,
    outlineVariant = DividerHairline,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorRed,
)

private val CardScanDarkColorScheme = darkColorScheme(
    primary = DarkCobaltPrimary,
    onPrimary = Color(0xFF041447),
    primaryContainer = DarkCobaltContainer,
    onPrimaryContainer = Color(0xFFD9E3FF),
    secondary = Color(0xFF9DB5FF),
    onSecondary = Color(0xFF081B54),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkInkPrimary,
    tertiary = Color(0xFF6CE9A6),
    onTertiary = Color(0xFF053321),
    tertiaryContainer = Color(0xFF093B26),
    onTertiaryContainer = Color(0xFFA6F4C5),
    background = DarkBackground,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = Color(0xFF252E42),
    outline = DarkOutline,
    outlineVariant = Color(0xFF273145),
    error = Color(0xFFF97066),
    onError = Color(0xFF55160C),
    errorContainer = Color(0xFF55160C),
    onErrorContainer = Color(0xFFFEE4E2),
)

val CardScanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CardScanTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CardScanDarkColorScheme else CardScanLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CardScanTypography,
        shapes = CardScanShapes,
        content = content,
    )
}
