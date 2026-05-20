package com.example.smartattendanceapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Dark scheme — matches the deep navy + electric blue UI ───────────────────
private val DarkColorScheme = darkColorScheme(
    // Primary — buttons, FABs, active nav items
    primary          = ElectricBlue,
    onPrimary        = Color(0xFF003544),
    primaryContainer = NeonPurple,
    onPrimaryContainer = Color.White,

    // Secondary — badges, chips
    secondary        = NeonPurple,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFF1E1535),
    onSecondaryContainer = Color(0xFFD0BCFF),

    // Backgrounds
    background       = DeepNavy,
    onBackground     = TextWhite,
    surface          = CardDark,
    onSurface        = TextWhite,
    surfaceVariant   = CardBorder,
    onSurfaceVariant = TextMuted,

    // Errors
    error            = ErrorRed,
    onError          = Color.White,
    errorContainer   = Color(0xFF3B0A0A),
    onErrorContainer = ErrorRed,

    // Outlines / borders
    outline          = CardBorder,
    outlineVariant   = Color(0xFF2A3A50)
)

// ─── Light scheme — fallback, matches old purple branding ─────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF667eea),
    onPrimary        = Color.White,
    background       = LightBackground,
    onBackground     = TextPrimary,
    surface          = LightSurface,
    onSurface        = TextPrimary,
    error            = ErrorRed,
    onError          = Color.White
)

/**
 * AttendX AI app theme.
 *
 * Uses the dark scheme by default to match the app's deep-navy premium design.
 * Status bar is set to [DeepNavy] with light icons.
 */
@Composable
fun SmartattendanceappTheme(
    darkTheme: Boolean = true,           // default dark — matches app design
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar matches deep navy background
            window.statusBarColor = DeepNavy.toArgb()
            // Navigation bar matches card dark
            window.navigationBarColor = CardDark.toArgb()
            // Light icons on dark background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}