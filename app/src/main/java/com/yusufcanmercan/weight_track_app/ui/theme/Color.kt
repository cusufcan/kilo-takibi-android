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
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    // Yüzeyler markanın çok açık mor tonundan türer (ADR-0017): tek ana renk, 60-30-10.
    background = Color(0xFFFCFAFF),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFFCFAFF),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFE7E0EF),
    surfaceContainerLow = Color(0xFFF8F4FD),
    surfaceContainer = Color(0xFFF3EEFC),
    surfaceContainerHigh = Color(0xFFEDE7F8),
    onSurfaceVariant = Color(0xFF5E5870),
    outline = Color(0xFF8E8799),
    outlineVariant = Color(0xFFE6E0EE),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF3B2A6B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1EC),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1EC),
    surfaceVariant = Color(0xFF413D4A),
    surfaceContainerLow = Color(0xFF1A1820),
    surfaceContainer = Color(0xFF211E28),
    surfaceContainerHigh = Color(0xFF2B2833),
    onSurfaceVariant = Color(0xFFBAB3C6),
    outline = Color(0xFF8C8597),
    outlineVariant = Color(0xFF37333F),
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
