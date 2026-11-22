package com.yusufcanmercan.weight_track_app.ui

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.yusufcanmercan.weight_track_app.MainActivity
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.data.repository.WeightRepository
import com.yusufcanmercan.weight_track_app.data.settings.SettingsRepository
import com.yusufcanmercan.weight_track_app.ui.edit.WEIGHT_INPUT_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Uygulamanın üç ana akışını gerçek ekranlarla dener. Veritabanı ve ayarlar her testte
 * boştur (bkz. TestDataModules). Metinler kaynaklardan okunur; cihaz dili ne olursa olsun çalışır.
 */
@HiltAndroidTest
class AppFlowTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var weights: WeightRepository
    @Inject lateinit var settings: SettingsRepository

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun inject() = hilt.inject()

    @After
    fun close() {
        scenario?.close()
    }

    private fun text(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)
    private fun kg(value: Double) = text(R.string.weight_kg, value)

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Uygulamayı açılış soruları geçilmiş ve [measurements] kayıtlı olarak açar. */
    private fun launchHome(vararg measurements: Weight) {
        runBlocking {
            measurements.forEach { weights.add(it) }
            settings.completeOnboarding()
        }
        launch()
    }

    private fun waitForText(value: String, present: Boolean = true) = compose.waitUntil(TIMEOUT) {
        compose.onAllNodes(hasText(value, substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty() == present
    }

    private fun clickText(@StringRes id: Int) {
        waitForText(text(id))
        compose.onNodeWithText(text(id)).performClick()
    }

    private fun typeInto(@StringRes label: Int, value: String) {
        compose.onNode(hasSetTextAction() and hasText(text(label)) and isEnabled()).performTextReplacement(value)
    }

    @Test
    fun acilis_sorulari_kilo_ve_hedefi_kaydedip_bugune_gecer() {
        launch()

        clickText(R.string.onboarding_start)
        clickText(R.string.onboarding_skip_step) // ad
        clickText(R.string.onboarding_skip_step) // boy
        waitForText(text(R.string.onboarding_weight_field))
        typeInto(R.string.onboarding_weight_field, "82")
        clickText(R.string.onboarding_continue)
        waitForText(text(R.string.settings_goal_field))
        typeInto(R.string.settings_goal_field, "75")
        clickText(R.string.onboarding_continue)
        clickText(R.string.onboarding_finish)

        waitForText(text(R.string.home_goal_progress))
        waitForText(kg(82.0))
    }

    @Test
    fun yeni_tarti_eklenince_bugun_ekraninda_gorunur() {
        launchHome(Weight(weight = 83.0, timeStamp = System.currentTimeMillis() - DAY))

        val addEntry = hasContentDescription(text(R.string.home_add_entry))
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(addEntry).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(addEntry)[0].performClick()
        waitForText(text(R.string.add_title))
        compose.onNodeWithTag(WEIGHT_INPUT_TAG).performTextReplacement("81.5")
        clickText(R.string.save)
        waitForText(text(R.string.feedback_title))
        clickText(R.string.back_to_today)

        waitForText(text(R.string.add_title), present = false)
        waitForText(kg(81.5))
    }

    @Test
    fun gecmisteki_kayit_acilip_silinir() {
        launchHome(Weight(weight = 80.0, timeStamp = System.currentTimeMillis() - DAY))

        val historyTab = hasContentDescription(text(R.string.nav_history)) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(historyTab).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(historyTab).performClick()
        waitForText(kg(80.0))
        compose.onNodeWithText(kg(80.0), useUnmergedTree = true).performClick()
        waitForText(text(R.string.edit_title))
        compose.onNodeWithContentDescription(text(R.string.delete)).performClick()
        clickText(R.string.delete) // onay penceresi

        waitForText(kg(80.0), present = false)
        waitForText(text(R.string.history_first_entry_hint))
    }

    private companion object {
        const val TIMEOUT = 10_000L
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
