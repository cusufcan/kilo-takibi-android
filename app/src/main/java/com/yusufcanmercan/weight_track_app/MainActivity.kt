package com.yusufcanmercan.weight_track_app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.ui.settings.SettingsViewModel
import androidx.compose.ui.Modifier
import com.yusufcanmercan.weight_track_app.ui.navigation.AppNavHost
import com.yusufcanmercan.weight_track_app.ui.theme.KiloTakibiTheme
import dagger.hilt.android.AndroidEntryPoint

/** Uygulamanın tek Activity'si (ADR-0004). Tüm ekranlar bunun içinde Compose ile çizilir. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
            // Ayar okunana kadar çizme: yanlış temayla bir kare çakılmasın.
            if (!settings.isLoading) {
                KiloTakibiTheme(themeMode = settings.themeMode) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}
