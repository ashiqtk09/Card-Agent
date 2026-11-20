package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Brand Cobalt Palette (matching CardScan design reference)
val CobaltPrimary = Color(0xFF0B3CE0)
val CobaltPrimaryDark = Color(0xFF072AA3)
val CobaltPrimaryLight = Color(0xFF3E6BFF)
val CobaltContainer = Color(0xFFE8EEFE)
val CobaltOnContainer = Color(0xFF082896)

// Neutral Light Surface & Ink Hierarchy
val PureWhite = Color(0xFFFFFFFF)
val SoftCanvas = Color(0xFFF7F8FC)
val SurfaceMuted = Color(0xFFF2F4F8)
val SurfaceContainerHigh = Color(0xFFEAEEF5)
val InkPrimary = Color(0xFF101828)
val InkSecondary = Color(0xFF475467)
val InkTertiary = Color(0xFF667085)
val OutlineSubtle = Color(0xFFD0D5DD)
val DividerHairline = Color(0xFFEAECF0)

// Semantic Status Accents
val MintBadgeBg = Color(0xFFD1FADF)
val MintBadgeText = Color(0xFF067647)
val AmberBadgeBg = Color(0xFFFEF3C7)
val AmberBadgeBorder = Color(0xFFF59E0B)
val AmberBadgeText = Color(0xFFB45309)
val ErrorRed = Color(0xFFD92D20)
val ErrorContainerLight = Color(0xFFFEF3F2)

// Dark Theme Palette (for system dark mode)
val DarkBackground = Color(0xFF0D111A)
val DarkSurface = Color(0xFF141925)
val DarkSurfaceVariant = Color(0xFF1E2638)
val DarkOutline = Color(0xFF344054)
val DarkCobaltPrimary = Color(0xFF6C8FFF)
val DarkCobaltContainer = Color(0xFF18295C)
val DarkInkPrimary = Color(0xFFF2F4F7)
val DarkInkSecondary = Color(0xFF98A2B3)

// Monogram Avatar Pastel Palette (matches design mockup: Mint, Ice Blue, Lavender, Cyan, Sage)
data class AvatarColorPair(val background: Color, val foreground: Color)

val AvatarPalette: List<AvatarColorPair> = listOf(
    AvatarColorPair(background = Color(0xFFDDF5EB), foreground = Color(0xFF0F5132)),
    AvatarColorPair(background = Color(0xFFE0ECFF), foreground = Color(0xFF153B8A)),
    AvatarColorPair(background = Color(0xFFECE6FF), foreground = Color(0xFF3B288C)),
    AvatarColorPair(background = Color(0xFFDEF2FA), foreground = Color(0xFF144D66)),
    AvatarColorPair(background = Color(0xFFE4F4E7), foreground = Color(0xFF1B5E3C)),
)
