package com.yusufcanmercan.weight_track_app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Marka rengi: eski uygulamanın moru #6200EE (ADR-0010). Tüm renkler yalnız burada tanımlanır;
// ekranlar hex yazmaz, MaterialTheme.colorScheme'den okur.
private val Brand = Color(0xFF6200EE)

val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3EDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFEDE6F5),
    surfaceContainer = Color(0xFFF3EFF8),
    onSurfaceVariant = Color(0xFF4A4458),
    outline = Color(0xFF7B7487),
    outlineVariant = Color(0xFFE6DFF3),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4A00B0),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    surfaceContainer = Color(0xFF211F26),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454F),
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
    decreaseContainer = Color(0xFFE8F5E9),
    onDecreaseContainer = Color(0xFF1B5E20),
    increaseContainer = Color(0xFFFDECEA),
    onIncreaseContainer = Color(0xFFB3261E),
)

val DarkChangeColors = ChangeColors(
    decreaseContainer = Color(0xFF1B3A1F),
    onDecreaseContainer = Color(0xFFA5D6A7),
    increaseContainer = Color(0xFF4A1F1C),
    onIncreaseContainer = Color(0xFFF2B8B5),
)

val LocalChangeColors = staticCompositionLocalOf { LightChangeColors }
