package com.yusufcanmercan.weight_track_app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yusufcanmercan.weight_track_app.util.millisUntilNextDay
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import kotlinx.coroutines.delay

/**
 * Bugünün seçici günü (UTC gece yarısı). Ekran görünürken gece yarısında kendiliğinden ilerler;
 * uygulama arka plandan dönünce de yeniden okunur (saat ya da saat dilimi değişmiş olabilir).
 */
@Composable
fun rememberToday(): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val today by produceState(toPickerMillis(System.currentTimeMillis()), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                value = toPickerMillis(now)
                delay(millisUntilNextDay(now) + 1_000L)
            }
        }
    }
    return today
}
