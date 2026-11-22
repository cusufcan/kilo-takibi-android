package com.yusufcanmercan.weight_track_app.data.settings

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

internal val WEEKLY_SUMMARY_KEY = booleanPreferencesKey("weekly_summary_enabled")

private const val LEGACY_WEEKLY_FILE = "weekly_notification"
private const val LEGACY_WEEKLY_ENABLED = "enabled"

/**
 * Eski SharedPreferences'taki haftalık özet tercihini bir kez DataStore'a taşır.
 */
internal fun settingsMigrations(context: Context): List<DataMigration<Preferences>> = listOf(
    SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = LEGACY_WEEKLY_FILE,
        keysToMigrate = setOf(LEGACY_WEEKLY_ENABLED),
    ) { legacy: SharedPreferencesView, current: Preferences ->
        current.toMutablePreferences().apply {
            if (WEEKLY_SUMMARY_KEY !in this) this[WEEKLY_SUMMARY_KEY] = legacy.getBoolean(LEGACY_WEEKLY_ENABLED, false)
        }.toPreferences()
    },
)
