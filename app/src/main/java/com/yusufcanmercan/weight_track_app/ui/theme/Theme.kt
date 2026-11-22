package com.yusufcanmercan.weight_track_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.yusufcanmercan.weight_track_app.data.settings.ThemeMode

// Yumuşak köşeler: kartlar large/extraLarge'ı kullanır.
private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Uygulama teması. [themeMode] SYSTEM ise telefonun açık/koyu ayarını izler. Renk sabit marka moru (ADR-0010). */
@Composable
fun KiloTakibiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(
        LocalChangeColors provides if (darkTheme) DarkChangeColors else LightChangeColors,
        LocalSeyirColors provides if (darkTheme) DarkSeyirColors else LightSeyirColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = AppShapes,
            typography = SeyirTypography,
            content = content,
        )
    }
}
