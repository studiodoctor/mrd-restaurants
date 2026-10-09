package com.example.mrd.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mrd.R

/** UI font: Plus Jakarta Sans (SIL OFL) — geometric, friendly, very legible at small sizes. */
val JakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/** Display font: Fraunces (SIL OFL) — a warm "soft serif" that gives headings an editorial, food-magazine feel. */
val Fraunces = FontFamily(
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold),
)

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    line: Int,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val MrDTypography = Typography(
    displayLarge = style(Fraunces, FontWeight.Bold, 44, 52, -0.02f),
    displayMedium = style(Fraunces, FontWeight.Bold, 36, 44, -0.02f),
    displaySmall = style(Fraunces, FontWeight.Bold, 32, 40, -0.015f),
    headlineLarge = style(Fraunces, FontWeight.Bold, 30, 38, -0.01f),
    headlineMedium = style(Fraunces, FontWeight.SemiBold, 26, 34, -0.005f),
    headlineSmall = style(Fraunces, FontWeight.SemiBold, 22, 30),

    titleLarge = style(JakartaSans, FontWeight.Bold, 20, 28),
    titleMedium = style(JakartaSans, FontWeight.Bold, 16, 22, 0.005f),
    titleSmall = style(JakartaSans, FontWeight.SemiBold, 14, 20, 0.005f),

    bodyLarge = style(JakartaSans, FontWeight.Normal, 16, 24),
    bodyMedium = style(JakartaSans, FontWeight.Normal, 14, 21),
    bodySmall = style(JakartaSans, FontWeight.Normal, 12, 18),

    labelLarge = style(JakartaSans, FontWeight.SemiBold, 14, 20, 0.01f),
    labelMedium = style(JakartaSans, FontWeight.SemiBold, 12, 16, 0.02f),
    labelSmall = style(JakartaSans, FontWeight.Medium, 11, 16, 0.03f),
)
