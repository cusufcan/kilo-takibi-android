package com.yusufcanmercan.weight_track_app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yusufcanmercan.weight_track_app.R

private val Inter = FontFamily(
    Font(R.font.seyir_regular, FontWeight.Normal),
    Font(R.font.seyir_semibold, FontWeight.SemiBold),
    Font(R.font.seyir_bold, FontWeight.Bold),
)

private fun seyirText(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Float = 0f,
    numeric: Boolean = false,
) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
    fontFeatureSettings = if (numeric) "tnum" else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

internal val SeyirTypography = Typography(
    displayLarge = seyirText(62, 66, FontWeight.Bold, -.085f, numeric = true),
    displayMedium = seyirText(54, 58, FontWeight.Bold, -.075f, numeric = true),
    displaySmall = seyirText(39, 42, FontWeight.Bold, -.062f),
    headlineLarge = seyirText(32, 36, FontWeight.Bold, -.055f),
    headlineMedium = seyirText(29, 34, FontWeight.Bold, -.04f),
    headlineSmall = seyirText(28, 32, FontWeight.Bold, -.055f),
    titleLarge = seyirText(23, 28, FontWeight.Bold, -.04f),
    titleMedium = seyirText(20, 24, FontWeight.SemiBold, -.035f, numeric = true),
    titleSmall = seyirText(14, 18, FontWeight.Bold),
    bodyLarge = seyirText(16, 23, FontWeight.Normal),
    bodyMedium = seyirText(14, 20, FontWeight.Normal),
    bodySmall = seyirText(12, 17, FontWeight.Normal),
    labelLarge = seyirText(13, 17, FontWeight.SemiBold, numeric = true),
    labelMedium = seyirText(11, 15, FontWeight.SemiBold, numeric = true),
    labelSmall = seyirText(11, 15, FontWeight.Normal, numeric = true),
)

internal val SeyirEyebrow = seyirText(11, 15, FontWeight.Bold, .045f)
internal val SeyirWeeklyValue = seyirText(66, 70, FontWeight.Bold, -.085f, numeric = true)

internal val SeyirHeroValue = seyirText(70, 74, FontWeight.Bold, -.085f, numeric = true)
