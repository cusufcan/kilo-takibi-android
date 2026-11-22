package com.yusufcanmercan.weight_track_app.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.local.Weight
import com.yusufcanmercan.weight_track_app.ui.home.Card
import com.yusufcanmercan.weight_track_app.ui.home.DAY_MILLIS
import com.yusufcanmercan.weight_track_app.ui.home.HomeUiState
import com.yusufcanmercan.weight_track_app.ui.home.StatNote
import com.yusufcanmercan.weight_track_app.ui.home.currentStreak
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIconTile
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import kotlin.math.abs

@Composable
internal fun WeightEntryResult(state: HomeUiState, entry: Weight?) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StatNote(stringResource(R.string.feedback_measurement))
        if (entry != null && state.items.size == 1) {
            Card {
                Text(
                    stringResource(R.string.feedback_first_entry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            return@Column
        }
        if (entry != null) {
            val entryDay = toPickerMillis(entry.timeStamp)
            val trend = state.dailyWeights.find { it.dayMillis == entryDay }
            val previous = state.dailyWeights.lastOrNull { it.dayMillis < entryDay }
            val change = if (trend != null && previous != null) trend.trendKg - previous.trendKg else null
            val headline = when {
                change == null -> null
                abs(change) < 0.05 -> stringResource(R.string.feedback_steady)
                change < 0.0 -> stringResource(R.string.feedback_down, abs(change))
                else -> stringResource(R.string.feedback_up, change)
            }
            Card {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        StatNote(stringResource(R.string.home_trend_weight))
                        if (headline != null) Text(headline, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        stringResource(R.string.weight_kg, trend?.trendKg ?: entry.weight),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            val measurementChange = previous?.let { entry.weight - it.measuredKg }
            val context = when {
                measurementChange == null || abs(measurementChange) < 0.05 -> stringResource(R.string.feedback_context)
                measurementChange > 0 -> stringResource(R.string.feedback_measurement_up, measurementChange)
                else -> stringResource(R.string.feedback_measurement_down, abs(measurementChange))
            }
            Text(
                context,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
            )
            val streak = currentStreak(state.dailyWeights, entryDay)
            if (streak > 0) {
                val extended = remember(state.items, state.dailyWeights, entry.id, entryDay) {
                    streak > currentStreak(state.dailyWeights, entryDay - DAY_MILLIS) &&
                        state.items.none { it.weight.id != entry.id && toPickerMillis(it.weight.timeStamp) == entryDay }
                }
                val visibility = remember(entry.id) {
                    MutableTransitionState(!extended).apply { targetState = true }
                }
                AnimatedVisibility(visibleState = visibility, enter = fadeIn(tween(170))) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ) {
                        Row(
                            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            SeyirIconTile(R.drawable.ic_seyir_streak, tileSize = 32.dp, iconSize = 18.dp)
                            Text(
                                pluralStringResource(R.plurals.entry_streak, streak, streak),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
