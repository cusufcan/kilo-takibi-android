package com.yusufcanmercan.weight_track_app.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Uygulama dili seçenekleri. Seçimi Android saklar (ADR-0008); biz yalnız ayarlar ve okuruz.
 * [tag] = dil etiketi ("tr", "en"); SYSTEM = boş = telefonun dilini izle.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    TURKISH("tr"),
    ENGLISH("en"),
    ;

    companion object {
        /** "tr", "en-US" ya da "" gibi etiket listesinden seçeneği bulur; tanınmazsa SYSTEM. */
        fun fromTags(tags: String): AppLanguage {
            val first = tags.substringBefore(',').substringBefore('-')
            return entries.firstOrNull { it.tag.isNotEmpty() && it.tag == first } ?: SYSTEM
        }

        fun current(): AppLanguage =
            fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

        /** Dili uygular; Android Activity'yi yeni dille yeniden oluşturur. */
        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }
}
