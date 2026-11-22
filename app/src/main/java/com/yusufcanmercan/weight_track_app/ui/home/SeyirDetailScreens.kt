package com.yusufcanmercan.weight_track_app.ui.home

import android.Manifest
import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.platform.WeeklyNotifications
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirEyebrow
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIcon
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIconTile
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirWeeklyValue
import com.yusufcanmercan.weight_track_app.util.toPickerMillis
import java.text.DateFormat
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(onSettings: () -> Unit, onMilestones: () -> Unit) {
    DetailColumn(R.string.nav_profile, onBack = null) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                ProfileLink(R.string.profile_settings, R.string.profile_settings_hint, R.drawable.ic_seyir_settings, onSettings)
                HorizontalDivider(Modifier.padding(start = Spacing.lg), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileLink(R.string.milestones_title, R.string.profile_milestones_hint, R.drawable.ic_seyir_target, onMilestones)
            }
        }
    }
}

@Composable
private fun ProfileLink(@StringRes label: Int, @StringRes hint: Int, @DrawableRes glyph: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp).padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeyirIconTile(glyph)
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
            StatNote(stringResource(hint))
        }
        SeyirIcon(R.drawable.ic_seyir_next, size = 16.dp, tint = colors.onSurfaceVariant)
    }
}

@Composable
fun MilestonesScreen(state: HomeUiState, onBack: () -> Unit, onSettings: () -> Unit) {
    val goal = state.goal
    val start = state.items.lastOrNull()?.weight?.weight
    val milestones = remember(state.dailyWeights, start, goal, state.heightCm) {
        if (start == null || goal == null) emptyList()
        else buildMilestones(state.dailyWeights, start, goal.goalKg, state.heightCm)
    }
    val next = milestones.indexOfFirst { it.reachedOn == null }
    DetailColumn(R.string.milestones_title, onBack = onBack) {
        if (goal == null) {
            Card { LinkText(stringResource(R.string.stat_goal_link), onSettings) }
        } else {
            Card {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        StatNote(stringResource(R.string.progress_arrival, goal.goalKg))
                        Text(
                            if (goal.reached) stringResource(R.string.home_goal_reached)
                            else stringResource(R.string.home_goal_remaining, abs(goal.remaining)),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    SeyirIconTile(R.drawable.ic_seyir_target, tileSize = 44.dp)
                }
            }
            milestones.forEachIndexed { index, milestone ->
                val colors = MaterialTheme.colorScheme
                Row(
                    Modifier.drawBehind {
                        val centerX = 18.dp.toPx()
                        val top = if (index == 0) center.y else 0f
                        val bottom = if (index == milestones.lastIndex) center.y else size.height + Spacing.md.toPx()
                        drawLine(colors.outlineVariant, Offset(centerX, top), Offset(centerX, bottom), strokeWidth = 1.dp.toPx())
                    },
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val completed = milestone.reachedOn != null
                    Box(
                        Modifier.size(36.dp).background(
                            if (completed || index == next) colors.primaryContainer else colors.surfaceContainerHigh,
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val glyph = when {
                            completed -> R.drawable.ic_seyir_check
                            milestone.kind == MilestoneKind.GOAL -> R.drawable.ic_seyir_target
                            else -> R.drawable.ic_seyir_next
                        }
                        SeyirIcon(glyph, tint = if (completed || index == next) colors.onPrimaryContainer else colors.onSurfaceVariant)
                    }
                    Card(Modifier.weight(1f), color = if (index == next) colors.surfaceContainerHigh else Color.Transparent) {
                        val label = when (milestone.kind) {
                            MilestoneKind.KILOGRAMS -> stringResource(R.string.change_kg, milestone.amount)
                            MilestoneKind.PERCENT -> stringResource(R.string.milestone_percent)
                            MilestoneKind.BMI -> stringResource(R.string.milestone_bmi)
                            MilestoneKind.GOAL -> stringResource(R.string.milestone_goal, milestone.amount)
                        }
                        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        milestone.reachedOn?.let { StatNote(formattedDay(it)) }
                        if (index == next) {
                            StatNote(
                                if (milestone.kind == MilestoneKind.KILOGRAMS) {
                                    stringResource(R.string.milestone_next_weight, abs(milestone.amount) * milestone.progress, abs(milestone.amount))
                                } else stringResource(R.string.milestone_next),
                            )
                            LinearProgressIndicator(
                                progress = { milestone.progress },
                                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                                color = colors.secondary,
                                trackColor = colors.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyScreen(state: HomeUiState, throughDay: Long?, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val end = throughDay ?: toPickerMillis(System.currentTimeMillis())
    val summary = remember(state.dailyWeights, end) { weeklySummary(state.dailyWeights, end) }
    val chart = remember(summary) { buildChartData(summary.days, period = summary.from..summary.to) }
    var notificationsEnabled by remember { mutableStateOf(WeeklyNotifications.isEnabled(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    var shareError by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) {
            WeeklyNotifications.setEnabled(context, true)
            notificationsEnabled = true
        }
    }
    val locale = LocalLocale.current.platformLocale
    val period = remember(summary.from, summary.to, locale) {
        DateIntervalFormat.getInstance("yMMMMd", locale).apply {
            timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
        }.format(DateInterval(summary.from, summary.to))
    }
    val weekTitle = stringResource(if (throughDay == null) R.string.weekly_this_week else R.string.weekly_last_week)
    val change = summary.displayChange
    val changeText = change?.let { stringResource(if (it == 0.0) R.string.weight_kg else R.string.change_kg, it) }
        ?: stringResource(if (summary.days.isEmpty()) R.string.weekly_no_measurements else R.string.home_week_no_data)
    val count = stringResource(R.string.weekly_count, summary.days.size, WEEK_DAYS)
    val reminderLabel = stringResource(R.string.weekly_notification_toggle)
    fun changeReminder(enabled: Boolean) {
        permissionDenied = false
        if (enabled && Build.VERSION.SDK_INT >= 33 && !WeeklyNotifications.hasPermission(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            WeeklyNotifications.setEnabled(context, enabled)
            notificationsEnabled = enabled
        }
    }
    DetailColumn(R.string.weekly_summary, onBack, bottom = {
        Button(
            enabled = summary.days.isNotEmpty() && !sharing,
            onClick = {
                shareError = false
                sharing = true
                scope.launch {
                    try {
                        shareWeeklyCard(context, weekTitle, period, changeText, count, chart)
                    } catch (error: kotlinx.coroutines.CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        shareError = true
                    } finally {
                        sharing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = CircleShape,
        ) {
            SeyirIcon(R.drawable.ic_seyir_share, size = 18.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.weekly_share))
        }
    }) {
        Column(Modifier.padding(top = Spacing.xl)) {
            Text(period, style = SeyirEyebrow, color = colors.onSurfaceVariant)
            Text(
                weekTitle,
                style = MaterialTheme.typography.headlineMedium,
            )
            if (change == null) {
                StatNote(changeText, Modifier.padding(top = Spacing.sm))
            } else {
                Row(Modifier.padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(changeText.removeSuffix(" kg"), Modifier.alignByBaseline(), style = SeyirWeeklyValue, color = colors.onPrimaryContainer)
                    Text("kg", Modifier.alignByBaseline(), style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
                }
            }
        }
        chart?.let {
            Card { WeightChart(it, height = 132.dp, compact = true, interactive = false) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SeyirIcon(R.drawable.ic_seyir_streak, size = 18.dp)
            Text(stringResource(R.string.weekly_count_label), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Text("${summary.days.size}/$WEEK_DAYS", style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
        }
        HorizontalDivider(color = colors.outlineVariant)
        StatNote(stringResource(R.string.weekly_context))
        Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainer) {
            Row(
                Modifier.fillMaxWidth().semantics { contentDescription = reminderLabel }
                    .toggleable(notificationsEnabled, role = Role.Switch, onValueChange = ::changeReminder)
                    .heightIn(min = 48.dp).padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(reminderLabel, Modifier.weight(1f).clearAndSetSemantics {}, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = notificationsEnabled, onCheckedChange = null)
            }
        }
        if (permissionDenied) StatNote(stringResource(R.string.weekly_notification_denied))
        if (shareError) Text(stringResource(R.string.weekly_share_error), color = colors.error)
    }
}

@Composable
private fun DetailColumn(
    title: Int,
    onBack: (() -> Unit)?,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.lg)) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        SeyirIcon(R.drawable.ic_seyir_back, description = stringResource(R.string.go_back))
                    }
                }
                Box(Modifier.weight(1f)) { ScreenTitle(stringResource(title)) }
            }
            content()
        }
        if (bottom != null) {
            Box(Modifier.fillMaxWidth().padding(top = Spacing.xs, bottom = Spacing.lg)) { bottom() }
        }
    }
}
