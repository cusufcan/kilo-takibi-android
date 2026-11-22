package com.yusufcanmercan.weight_track_app.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.theme.LocalChangeColors
import com.yusufcanmercan.weight_track_app.ui.theme.LocalSeyirColors
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirEyebrow
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirHeroValue
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIcon
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIconTile
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: HomeUiState,
    displayName: String?,
    holdTrend: Boolean,
    keepEmptyHome: Boolean,
    onQuickEntry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMilestones: () -> Unit,
    onOpenWeekly: () -> Unit,
) {
    val today = rememberToday()
    val weekly = remember(state.dailyWeights, today) { weeklySummary(state.dailyWeights, today) }
    val chart = remember(weekly) { buildChartData(weekly.days, period = weekly.from..weekly.to) }
    val streak = remember(state.dailyWeights, today) { currentStreak(state.dailyWeights, today) }
    val firstUse = keepEmptyHome || (!state.isLoading && state.items.isEmpty())
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xl, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column {
                val locale = LocalLocale.current.platformLocale
                val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMd")
                val date = Date()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        SimpleDateFormat(pattern, locale).format(date) + " · " + SimpleDateFormat("EEEE", locale).format(date),
                        style = SeyirEyebrow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!firstUse && !state.isLoading) FilledTonalButton(
                        onClick = onOpenWeekly,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        SeyirIcon(R.drawable.ic_seyir_note, size = 16.dp)
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.weekly_summary))
                    }
                }
                Text(
                    if (displayName == null) stringResource(R.string.home_greeting)
                    else stringResource(R.string.home_greeting_named, displayName),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            if (firstUse) {
                EmptyState(onAdd = onQuickEntry, modifier = Modifier.fillMaxWidth())
            } else state.dailyWeights.lastOrNull()?.let { current ->
                HeroCard(
                    current, weekly.displayChange, current.dayMillis == today,
                    firstMeasurement = state.dailyWeights.size == 1,
                    holdTrend = holdTrend,
                )
                GoalCard(state.goal, state.items.lastOrNull()?.weight?.weight, onOpenSettings, onOpenMilestones)
                if (chart != null) {
                    Card(Modifier.clickable(onClick = onOpenProgress)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.last_seven_days), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            StatNote(stringResource(
                                if (state.dailyWeights.size == 1) R.string.chart_measurements
                                else R.string.measurements_and_trend,
                            ))
                        }
                        WeightChart(chart, Modifier.padding(top = Spacing.sm), height = 144.dp, compact = true, interactive = false)
                    }
                } else {
                    Card {
                        Text(stringResource(R.string.last_seven_days), style = MaterialTheme.typography.titleSmall)
                        StatNote(stringResource(R.string.weekly_no_measurements), Modifier.padding(top = Spacing.xs))
                        LinkText(stringResource(R.string.nav_progress), onOpenProgress)
                    }
                }
            }
        }
        if (!firstUse && !state.isLoading) FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.dailyWeights.isNotEmpty()) {
                TextButton(onClick = onOpenHistory) {
                    SeyirIconTile(R.drawable.ic_seyir_streak)
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            if (streak > 0) pluralStringResource(R.plurals.entry_streak, streak, streak)
                            else stringResource(R.string.nav_history),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (streak > 0) {
                            StatNote(stringResource(
                                if (weekly.days.lastOrNull()?.dayMillis == today) R.string.streak_logged_today
                                else R.string.streak_through_yesterday,
                            ))
                        }
                    }
                }
            }
            val addLabel = stringResource(R.string.home_add_entry)
            ExtendedFloatingActionButton(
                onClick = onQuickEntry,
                icon = { SeyirIcon(R.drawable.ic_seyir_plus) },
                text = { Text(addLabel, Modifier.clearAndSetSemantics {}, fontWeight = FontWeight.Bold) },
                modifier = Modifier.semantics { contentDescription = addLabel },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/**
 * Kenarlıksız kart; vurgulu olan `primaryContainer` kullanır.
 */
@Composable
internal fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(shape = shape, color = color, modifier = modifier.fillMaxWidth()) {
        Column(contentModifier.padding(Spacing.lg), content = content)
    }
}

@Composable
internal fun FirstEntryCard(@DrawableRes glyph: Int, hint: String, onAdd: () -> Unit) {
    Card {
        SeyirIconTile(glyph)
        Text(
            stringResource(R.string.home_empty_title),
            modifier = Modifier.padding(top = Spacing.md),
            style = MaterialTheme.typography.titleMedium,
        )
        StatNote(hint, Modifier.padding(top = Spacing.xs))
        Button(onClick = onAdd, modifier = Modifier.padding(top = Spacing.md).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.first_entry_action))
        }
    }
}

@Composable
private fun HeroCard(
    current: DailyWeight,
    weeklyChange: Double?,
    measuredToday: Boolean,
    firstMeasurement: Boolean,
    holdTrend: Boolean,
    modifier: Modifier = Modifier,
) {
    val ringColor = LocalSeyirColors.current.tonalDecoration
    val formattedTrend = "%.1f".format(LocalLocale.current.platformLocale, current.trendKg)
    var displayedTrend by remember { mutableStateOf(formattedTrend) }
    LaunchedEffect(formattedTrend, holdTrend) {
        if (!holdTrend) displayedTrend = formattedTrend
    }
    Card(
        modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentModifier = Modifier.heightIn(min = 224.dp).drawBehind {
            val center = Offset(size.width - 19.dp.toPx(), 80.dp.toPx())
            drawCircle(ringColor, 102.dp.toPx(), center, style = Stroke(1.dp.toPx()))
            drawCircle(ringColor, 135.dp.toPx(), center, style = Stroke(20.dp.toPx()))
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            SeyirIcon(R.drawable.ic_seyir_logo, size = 16.dp)
            Text(
                stringResource(if (firstMeasurement) R.string.first_measurement else R.string.home_trend_weight),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(modifier = Modifier.padding(vertical = Spacing.sm)) {
            AnimatedContent(
                targetState = displayedTrend,
                modifier = Modifier.alignByBaseline(),
                transitionSpec = { fadeIn(tween(120, delayMillis = 50)) togetherWith fadeOut(tween(50)) },
                label = "Trend weight",
            ) { value ->
                Text(value, style = SeyirHeroValue)
            }
            Text(
                text = "kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = Spacing.xs).alignByBaseline(),
            )
        }
        val label = stringResource(if (measuredToday) R.string.home_measurement_today else R.string.home_measurement_latest)
        val amount = stringResource(R.string.weight_kg, current.measuredKg)
        val lastDate = if (measuredToday) null else formattedDay(current.dayMillis)
        if (!firstMeasurement) {
            Text(
                text = buildAnnotatedString {
                    append(label)
                    append(" · ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(amount) }
                    if (lastDate != null) append(" · $lastDate")
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
            Text(
                stringResource(R.string.home_trend_explanation),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )
        }
        if (firstMeasurement && lastDate != null) {
            StatNote(lastDate, Modifier.padding(bottom = Spacing.sm))
        }
        if (weeklyChange != null) {
            ChangeBadge(weeklyChange, if (weeklyChange == 0.0) R.string.home_week_steady else R.string.home_week_change)
        } else {
            Text(
                text = stringResource(if (firstMeasurement) R.string.single_measurement_hint else R.string.home_week_no_data),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun GoalCard(goal: GoalProgress?, startingWeight: Double?, onOpenSettings: () -> Unit, onOpenMilestones: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalLocale.current.platformLocale
    Card(
        modifier = if (goal != null) Modifier.clickable(onClick = onOpenMilestones) else Modifier,
        contentModifier = if (goal != null) Modifier.heightIn(min = 104.dp) else Modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_goal_progress),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (goal != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (goal.reached) stringResource(R.string.home_goal_reached)
                        else stringResource(R.string.home_goal_remaining, abs(goal.remaining)),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    SeyirIcon(R.drawable.ic_seyir_next, size = 16.dp, tint = colors.onPrimaryContainer)
                }
            }
        }
        if (goal == null) {
            LinkText(stringResource(R.string.stat_goal_link), onOpenSettings)
        } else {
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier.padding(top = Spacing.md).fillMaxWidth().height(6.dp),
                color = colors.secondary,
                trackColor = colors.outlineVariant,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (startingWeight != null) {
                    StatNote(stringResource(R.string.weight_kg, startingWeight))
                }
                Text(
                    text = NumberFormat.getPercentInstance(locale).format(goal.progress.toDouble()),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                StatNote(stringResource(R.string.weight_kg, goal.goalKg))
            }
            if (goal.reached && abs(goal.remaining) >= 0.1) {
                val message = if (goal.remaining < 0) R.string.home_goal_reached_below else R.string.home_goal_reached_above
                StatNote(stringResource(message, abs(goal.remaining)), Modifier.padding(top = Spacing.xs))
            }
        }
    }
}

@Composable
internal fun BmiCard(state: HomeUiState, onOpenSettings: () -> Unit) {
    Card(Modifier.padding(top = Spacing.md)) {
        Text(
            text = stringResource(R.string.stat_bmi),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        val bmi = state.bmi
        if (bmi != null) {
            Row(
                modifier = Modifier.padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("%.1f".format(bmi.value), style = MaterialTheme.typography.titleLarge)
                StatNote(stringResource(bmiCategoryText(bmi.category)))
            }
            StatNote(stringResource(R.string.home_bmi_disclaimer), Modifier.padding(top = Spacing.xs))
        } else if (!state.hasHeight) {
            LinkText(stringResource(R.string.stat_bmi_link), onOpenSettings)
        }
    }
}

@Composable
internal fun StatNote(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    modifier = modifier,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

private fun bmiCategoryText(category: BmiCategory) = when (category) {
    BmiCategory.UNDERWEIGHT -> R.string.bmi_underweight
    BmiCategory.NORMAL -> R.string.bmi_normal
    BmiCategory.OVERWEIGHT -> R.string.bmi_overweight
    BmiCategory.OBESE -> R.string.bmi_obese
}

@Composable
internal fun LinkText(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = Spacing.xs, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            SeyirIcon(R.drawable.ic_seyir_next, size = 16.dp)
        }
    }
}

@Composable
internal fun ChangeBadge(change: Double, textRes: Int) {
    val colors = LocalChangeColors.current
    val (container, content) = when {
        change < 0 -> colors.decreaseContainer to colors.onDecreaseContainer
        change > 0 -> colors.increaseContainer to colors.onIncreaseContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Row(
            Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when {
                change < 0 -> R.drawable.ic_seyir_down
                change > 0 -> R.drawable.ic_seyir_up
                else -> R.drawable.ic_seyir_level
            }
            SeyirIcon(icon, size = 14.dp)
            Text(stringResource(textRes, change), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ScaleIllustration()
        Spacer(Modifier.height(Spacing.xl))
        Text(
            stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.home_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        Button(onClick = onAdd, shape = CircleShape, modifier = Modifier.heightIn(min = 48.dp)) {
            SeyirIcon(R.drawable.ic_seyir_plus, size = 16.dp)
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.first_entry_action), fontWeight = FontWeight.Bold)
        }
    }
}

/** Basit bir baskül çizimi: yuvarlak köşeli mor kare, içinde açık kadran ve ibre. */
@Composable
private fun ScaleIllustration() {
    val body = MaterialTheme.colorScheme.primary
    val face = MaterialTheme.colorScheme.primaryContainer
    val needle = MaterialTheme.colorScheme.onPrimaryContainer
    Canvas(modifier = Modifier.size(140.dp)) {
        drawRoundRect(body, size = size, cornerRadius = CornerRadius(36.dp.toPx()))
        val dial = Size(size.width * 0.6f, size.height * 0.6f)
        val topLeft = Offset((size.width - dial.width) / 2, (size.height - dial.height) / 2)
        drawOval(face, topLeft = topLeft, size = dial)
        val center = Offset(size.width / 2, size.height / 2)
        drawLine(
            needle,
            start = center,
            end = Offset(center.x + dial.width * 0.28f, center.y - dial.height * 0.2f),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(needle, radius = 6.dp.toPx(), center = center)
    }
}
