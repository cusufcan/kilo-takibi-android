package com.yusufcanmercan.weight_track_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.ui.settings.SettingsViewModel
import com.yusufcanmercan.weight_track_app.ui.navigation.AppNavHost
import com.yusufcanmercan.weight_track_app.ui.navigation.AppLaunch
import com.yusufcanmercan.weight_track_app.ui.onboarding.OnboardingPhase
import com.yusufcanmercan.weight_track_app.ui.onboarding.OnboardingScreen
import com.yusufcanmercan.weight_track_app.ui.onboarding.OnboardingStartup
import com.yusufcanmercan.weight_track_app.ui.onboarding.OnboardingViewModel
import com.yusufcanmercan.weight_track_app.platform.SeyirWidget
import com.yusufcanmercan.weight_track_app.ui.theme.KiloTakibiTheme
import dagger.hilt.android.AndroidEntryPoint

/** Uygulamanın tek Activity'si (ADR-0004). Tüm ekranlar bunun içinde Compose ile çizilir. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private var appLaunch by mutableStateOf<AppLaunch?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        var firstContentReady = false
        splash.setKeepOnScreenCondition { !firstContentReady }
        if (savedInstanceState == null) appLaunch = launchRequest(intent)
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val onboarding by onboardingViewModel.phase.collectAsStateWithLifecycle()
            // Açılış ekranı, tema ve ilk ekran hazır olduğunda kalkar.
            if (!settings.isLoading) {
                LaunchedEffect(settings.themeMode, onboarding) {
                    if (onboarding == OnboardingPhase.HOME) SeyirWidget.refresh(this@MainActivity)
                }
                KiloTakibiTheme(themeMode = settings.themeMode) {
                    val lightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
                    SideEffect {
                        if (onboarding != OnboardingPhase.CHECKING && onboarding != OnboardingPhase.HOME) {
                            firstContentReady = true
                        }
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = lightSurface
                            isAppearanceLightNavigationBars = lightSurface
                        }
                    }
                    Surface(modifier = Modifier.fillMaxSize()) {
                        when (onboarding) {
                            OnboardingPhase.HOME -> AppNavHost(
                                launch = appLaunch,
                                displayName = settings.displayName,
                                onReady = { firstContentReady = true },
                                onLaunchHandled = { appLaunch = null },
                            )
                            OnboardingPhase.CHECKING, OnboardingPhase.CHECK_FAILED ->
                                OnboardingStartup(onboarding, onboardingViewModel::retry)
                            else -> OnboardingScreen(onboarding, onboardingViewModel::finish, onboardingViewModel::retry)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appLaunch = launchRequest(intent)
    }

    private fun launchRequest(intent: Intent?): AppLaunch? = when (intent?.action) {
        QUICK_ENTRY -> AppLaunch.QuickEntry
        OPEN_WEEKLY -> AppLaunch.OpenWeekly(intent.getLongExtra(THROUGH_DAY, 0L).takeUnless { it == 0L })
        else -> null
    }

    companion object {
        private const val QUICK_ENTRY = "com.yusufcanmercan.weight_track_app.QUICK_ENTRY"
        private const val OPEN_WEEKLY = "com.yusufcanmercan.weight_track_app.OPEN_WEEKLY"
        private const val THROUGH_DAY = "through_day"

        fun quickEntryIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
            .setAction(QUICK_ENTRY).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun weeklyIntent(context: Context, throughDay: Long): Intent = Intent(context, MainActivity::class.java)
            .setAction(OPEN_WEEKLY).putExtra(THROUGH_DAY, throughDay)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
