package com.yusufcanmercan.weight_track_app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Marka rengi: eski uygulamanın moru #6200EE (ADR-0010, ADR-0018). Tüm renkler yalnız burada tanımlanır;
// ekranlar hex yazmaz, MaterialTheme.colorScheme'den okur.
private val Brand = Color(0xFF6200EE)

val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDDCFF),
    onPrimaryContainer = Color(0xFF22005D),
    // secondary = pano "accent-tonal": aynı mor aileden destek tonu (ADR-0018).
    secondary = Color(0xFF7C6299),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDECAF1),
    onSecondaryContainer = Color(0xFF1D192B),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1D1A21),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1D1A21),
    surfaceVariant = Color(0xFFE7E0EF),
    surfaceContainerLow = Color(0xFFF7F2FB),
    surfaceContainer = Color(0xFFF1ECF5),
    surfaceContainerHigh = Color(0xFFECE6F1),
    onSurfaceVariant = Color(0xFF4F4559),
    outline = Color(0xFF7F7389),
    outlineVariant = Color(0xFFD6CFDD),
    scrim = Color(0xA81D112B),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFDBB8FF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF3C245B),
    onPrimaryContainer = Color(0xFFEDDCFF),
    secondary = Color(0xFFAA96BF),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4D336E),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF151218),
    onBackground = Color(0xFFE7E0EE),
    surface = Color(0xFF151218),
    onSurface = Color(0xFFE7E0EE),
    surfaceVariant = Color(0xFF413D4A),
    surfaceContainerLow = Color(0xFF1B171F),
    surfaceContainer = Color(0xFF221E27),
    surfaceContainerHigh = Color(0xFF2B2630),
    onSurfaceVariant = Color(0xFFCDC2D8),
    outline = Color(0xFF978CA3),
    outlineVariant = Color(0xFF50445D),
    scrim = Color(0xA81D112B),
)

/** Kilo değişim rozetleri: Material şemasında "iyi/kötü" rengi olmadığı için ayrı tutulur. */
@Immutable
data class ChangeColors(
    val decreaseContainer: Color,
    val onDecreaseContainer: Color,
    val increaseContainer: Color,
    val onIncreaseContainer: Color,
)

val LightChangeColors = ChangeColors(
    decreaseContainer = Color(0xFFDCECE2),
    onDecreaseContainer = Color(0xFF22604D),
    increaseContainer = Color(0xFFF4E7DA),
    onIncreaseContainer = Color(0xFF78502F),
)

val DarkChangeColors = ChangeColors(
    decreaseContainer = Color(0xFF203A30),
    onDecreaseContainer = Color(0xFFB7DEC9),
    increaseContainer = Color(0xFF3B2B22),
    onIncreaseContainer = Color(0xFFEDC7AA),
)

val LocalChangeColors = staticCompositionLocalOf { LightChangeColors }
