package com.example.smartattendanceapp

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// AppTheme — the single source of truth for every colour in the app.
// All screens read from LocalAppTheme.current instead of using hardcoded values.
// When the user toggles Light Mode in Settings, MainScreen recomposes with the
// opposite palette and every screen updates automatically.
// ─────────────────────────────────────────────────────────────────────────────

data class AppColors(
    val background:  Color,   // screen background
    val card:        Color,   // card / surface
    val cardBorder:  Color,   // card stroke
    val accent:      Color,   // primary accent (Electric Blue)
    val accent2:     Color,   // secondary accent (Neon Purple)
    val success:     Color,
    val warning:     Color,
    val error:       Color,
    val textPrimary: Color,
    val textMuted:   Color,
    val isLight:     Boolean  // true = light theme active
)

val DarkColors = AppColors(
    background  = Color(0xFF0A0E1A),
    card        = Color(0xFF111827),
    cardBorder  = Color(0xFF1E2D40),
    accent      = Color(0xFF00D4FF),
    accent2     = Color(0xFF7B5EFF),
    success     = Color(0xFF00E676),
    warning     = Color(0xFFFFAB40),
    error       = Color(0xFFFF5252),
    textPrimary = Color(0xFFE8EAED),
    textMuted   = Color(0xFF6B7A99),
    isLight     = false
)

val LightColors = AppColors(
    background  = Color(0xFFF5F7FA),
    card        = Color(0xFFFFFFFF),
    cardBorder  = Color(0xFFE2E8F0),
    accent      = Color(0xFF0099CC),   // slightly darker blue for light bg contrast
    accent2     = Color(0xFF6B46C1),   // darker purple for contrast
    success     = Color(0xFF16A34A),
    warning     = Color(0xFFD97706),
    error       = Color(0xFFDC2626),
    textPrimary = Color(0xFF0F172A),
    textMuted   = Color(0xFF64748B),
    isLight     = true
)

// CompositionLocal so any composable can call LocalAppTheme.current
val LocalAppTheme = compositionLocalOf { DarkColors }