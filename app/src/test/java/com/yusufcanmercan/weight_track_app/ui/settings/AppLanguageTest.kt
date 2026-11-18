package com.yusufcanmercan.weight_track_app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `bos etiket SYSTEM demektir`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(""))
    }

    @Test
    fun `tr ve en taninir`() {
        assertEquals(AppLanguage.TURKISH, AppLanguage.fromTags("tr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en"))
    }

    @Test
    fun `bolge eki ve liste olsa da ilk dil esas alinir`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en-US,tr"))
        assertEquals(AppLanguage.TURKISH, AppLanguage.fromTags("tr-TR"))
    }

    @Test
    fun `taninmayan dil SYSTEM sayilir`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("de"))
    }
}
