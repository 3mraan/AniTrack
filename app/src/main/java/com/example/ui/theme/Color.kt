package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Stitch AniTrack Design System Tokens (Dark Theme)
val SurfaceDark = Color(0xFF0B0B12)
val OnSurfaceDark = Color(0xFFFFFFFF)
val SurfaceContainerLowDark = Color(0xFF12121C)
val SurfaceContainerDark = Color(0xFF191925)
val SurfaceContainerHighDark = Color(0xFF222232)
val OnSurfaceVariantDark = Color(0xFFA1A1AA)
val OutlineDark = Color(0xFF2A2A38)
val OutlineVariantDark = Color(0xFF332E3D)

val PrimaryViolet = Color(0xFF8B5CF6)
val PrimaryTextViolet = Color(0xFFA78BFA)
val OnPrimaryDark = Color(0xFF0B0B12)
val PrimaryContainerDark = Color(0xFF3C0091)
val OnPrimaryContainerDark = Color(0xFFE9DDFF)

val SecondaryBlue = Color(0xFF3B82F6)
val OnSecondaryDark = Color(0xFF0B0B12)
val SecondaryContainerDark = Color(0xFF002E6A)
val OnSecondaryContainerDark = Color(0xFFD8E2FF)

val SuccessGreen = Color(0xFF22C55E)
val OnSuccessDark = Color(0xFF0B0B12)
val WarningAmber = Color(0xFFF59E0B)
val OnWarningDark = Color(0xFF0B0B12)
val ErrorRed = Color(0xFFEF4444)
val OnErrorDark = Color(0xFF0B0B12)

// Light Mode Tokens (for Settings Theme Mode toggle)
val SurfaceLight = Color(0xFFF7F7FC)
val OnSurfaceLight = Color(0xFF11111A)
val SurfaceContainerLowLight = Color(0xFFEEEEF6)
val SurfaceContainerLight = Color(0xFFFFFFFF)
val SurfaceContainerHighLight = Color(0xFFE6E5F0)
val OnSurfaceVariantLight = Color(0xFF525262)
val OutlineLight = Color(0xFFD8D6E4)
val OutlineVariantLight = Color(0xFFC5C2D4)

val PrimaryVioletLight = Color(0xFF7C3AED)
val PrimaryTextVioletLight = Color(0xFF6D28D9)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEDE9FE)
val OnPrimaryContainerLight = Color(0xFF2E1065)

val SecondaryBlueLight = Color(0xFF2563EB)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFDBEAFE)
val OnSecondaryContainerLight = Color(0xFF1E3A8A)

val SuccessGreenLight = Color(0xFF16A34A)
val OnSuccessLight = Color(0xFFFFFFFF)
val WarningAmberLight = Color(0xFFD97706)
val OnWarningLight = Color(0xFFFFFFFF)
val ErrorRedLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)

@Immutable
data class AniTrackExtendedColors(
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val outline: Color,
    val outlineVariant: Color,
    val primaryText: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val isDark: Boolean
)

val DarkExtendedColors = AniTrackExtendedColors(
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    primaryText = PrimaryTextViolet,
    success = SuccessGreen,
    onSuccess = OnSuccessDark,
    warning = WarningAmber,
    onWarning = OnWarningDark,
    isDark = true
)

val LightExtendedColors = AniTrackExtendedColors(
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    primaryText = PrimaryTextVioletLight,
    success = SuccessGreenLight,
    onSuccess = OnSuccessLight,
    warning = WarningAmberLight,
    onWarning = OnWarningLight,
    isDark = false
)

val LocalAniTrackColors = staticCompositionLocalOf { DarkExtendedColors }
