package com.example.smartattendanceapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * AttendX AI typography.
 *
 * Scale:
 *  displayLarge   → 32sp ExtraBold  — big hero numbers (stats)
 *  headlineLarge  → 28sp Bold       — screen titles
 *  headlineMedium → 22sp ExtraBold  — section headers
 *  titleLarge     → 18sp Bold       — card titles
 *  titleMedium    → 15sp SemiBold   — sub-titles, tab labels
 *  titleSmall     → 13sp SemiBold   — badges, chips
 *  bodyLarge      → 16sp Normal     — body copy
 *  bodyMedium     → 14sp Normal     — secondary body
 *  bodySmall      → 12sp Normal     — captions, hints
 *  labelLarge     → 14sp Bold       — button text
 *  labelMedium    → 12sp SemiBold   — small labels
 *  labelSmall     → 10sp Medium     — tiny tags
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 32.sp,
        lineHeight   = 40.sp,
        letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontWeight   = FontWeight.Bold,
        fontSize     = 28.sp,
        lineHeight   = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 22.sp,
        lineHeight   = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontWeight   = FontWeight.Bold,
        fontSize     = 18.sp,
        lineHeight   = 26.sp
    ),
    titleLarge = TextStyle(
        fontWeight   = FontWeight.Bold,
        fontSize     = 18.sp,
        lineHeight   = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 15.sp,
        lineHeight   = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        lineHeight   = 18.sp
    ),
    bodyLarge = TextStyle(
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight   = FontWeight.Bold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight   = FontWeight.Medium,
        fontSize     = 10.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.5.sp
    )
)