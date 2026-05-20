package com.example.smartattendanceapp.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary brand colors ──────────────────────────────────────────────────────
val ElectricBlue   = Color(0xFF00D4FF)   // primary accent — buttons, highlights
val NeonPurple     = Color(0xFF7B5EFF)   // secondary accent — gradients, badges
val GradientStart  = NeonPurple          // gradient left/top
val GradientEnd    = ElectricBlue        // gradient right/bottom

// ─── Background & surface ──────────────────────────────────────────────────────
val DeepNavy       = Color(0xFF0A0E1A)   // main screen background
val CardDark       = Color(0xFF111827)   // card / bottom-sheet background
val CardBorder     = Color(0xFF1E2D40)   // card border, dividers

// ─── Semantic colors ───────────────────────────────────────────────────────────
val SuccessGreen   = Color(0xFF00E676)   // present indicator, success states
val WarnAmber      = Color(0xFFFFAB40)   // warnings, teacher badge
val ErrorRed       = Color(0xFFFF5252)   // errors, logout, destructive actions
val PresentGreen   = Color(0xFF00C853)   // attendance "Present" badge

// ─── Text ─────────────────────────────────────────────────────────────────────
val TextWhite      = Color(0xFFE8EAED)   // primary text on dark backgrounds
val TextMuted      = Color(0xFF6B7A99)   // secondary / hint text

// ─── Legacy Material names (kept so existing references don't break) ──────────
val Purple80       = Color(0xFFD0BCFF)
val PurpleGrey80   = Color(0xFFCCC2DC)
val Pink80         = Color(0xFFEFB8C8)
val Purple40       = NeonPurple
val PurpleGrey40   = Color(0xFF625b71)
val Pink40         = Color(0xFF7D5260)

// ─── Kept for any remaining light-mode references ──────────────────────────────
val Purple500      = Color(0xFF667eea)
val Purple200      = Color(0xFF764ba2)
val LightBackground = Color(0xFFF5F5F7)
val LightSurface   = Color(0xFFFFFFFF)
val LightCard      = Color(0xFFFFFFFF)
val TextPrimary    = Color(0xFF1A1A1A)
val TextSecondary  = Color(0xFF757575)
val TextHint       = Color(0xFF9E9E9E)
val BorderColor    = Color(0xFFE0E0E0)
val InputBackground = Color(0xFFF5F5F5)