package com.yusufcanmercan.weight_track_app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightViewModel
import com.yusufcanmercan.weight_track_app.ui.edit.WeightRuler
import com.yusufcanmercan.weight_track_app.ui.home.Card
import com.yusufcanmercan.weight_track_app.ui.settings.SettingsViewModel
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIcon
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirIconTile
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing

private enum class Step(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    @StringRes val field: Int = 0,
    @StringRes val error: Int = 0,
) {
    WELCOME(R.string.onboarding_welcome, R.string.onboarding_welcome_hint, R.drawable.ic_seyir_logo),
    NAME(R.string.onboarding_name, R.string.onboarding_name_hint, R.drawable.ic_seyir_user, R.string.onboarding_name_field),
    HEIGHT(
        R.string.onboarding_height, R.string.onboarding_height_hint, R.drawable.ic_seyir_note,
        R.string.settings_height_field, R.string.settings_height_invalid,
    ),
    WEIGHT(
        R.string.onboarding_weight, R.string.onboarding_weight_hint, R.drawable.ic_seyir_plus,
        R.string.onboarding_weight_field, R.string.weight_error,
    ),
    GOAL(
        R.string.onboarding_goal, R.string.onboarding_goal_hint, R.drawable.ic_seyir_target,
        R.string.settings_goal_field, R.string.settings_goal_invalid,
    ),
    REVIEW(R.string.onboarding_review, R.string.onboarding_review_hint, R.drawable.ic_seyir_check),
}

@Composable
internal fun OnboardingStartup(phase: OnboardingPhase, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        if (phase == OnboardingPhase.CHECKING) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(stringResource(R.string.onboarding_start_error), textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text(stringResource(R.string.onboarding_retry)) }
            }
        }
    }
}

@Composable
internal fun OnboardingScreen(
    phase: OnboardingPhase,
    onFinish: (OnboardingAnswers) -> Unit,
    onRetry: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    var name by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    var invalidStep by rememberSaveable { mutableStateOf<Step?>(null) }
    val editing = phase == OnboardingPhase.QUESTIONS
    val focus = LocalFocusManager.current
    val locale = LocalLocale.current.platformLocale
    val colors = MaterialTheme.colorScheme

    fun answers() = OnboardingAnswers(
        name = name.trim().takeIf { it.isNotEmpty() },
        heightCm = measurement(height),
        weightKg = measurement(weight),
        goalKg = measurement(goal),
    )

    fun onBack() {
        focus.clearFocus()
        invalidStep = null
        step = Step.entries[step.ordinal - 1]
    }

    fun next(skip: Boolean = false) {
        if (!editing) return
        if (skip && step == Step.WELCOME) {
            onFinish(OnboardingAnswers())
            return
        }
        if (step == Step.REVIEW) {
            onFinish(answers())
            return
        }
        val valid = when (step) {
            Step.HEIGHT -> height.isBlank() || measurement(height)
                ?.let { it in SettingsViewModel.MIN_HEIGHT_CM..SettingsViewModel.MAX_HEIGHT_CM } == true
            Step.WEIGHT -> weight.isBlank() || validWeight(weight)
            Step.GOAL -> goal.isBlank() || validWeight(goal)
            else -> true
        }
        if (!skip && !valid) {
            invalidStep = step
            return
        }
        if (skip) when (step) {
            Step.NAME -> name = ""
            Step.HEIGHT -> height = ""
            Step.WEIGHT -> weight = ""
            Step.GOAL -> goal = ""
            else -> Unit
        }
        focus.clearFocus()
        invalidStep = null
        step = Step.entries[step.ordinal + 1]
    }

    BackHandler(enabled = phase != OnboardingPhase.QUESTIONS || step != Step.WELCOME) {
        if (editing) onBack()
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = Spacing.xl)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (step != Step.WELCOME) {
                IconButton(onClick = ::onBack, enabled = editing) {
                    SeyirIcon(R.drawable.ic_seyir_back, description = stringResource(R.string.onboarding_back))
                }
            }
            LinearProgressIndicator(
                progress = { (step.ordinal + 1).toFloat() / Step.entries.size },
                modifier = Modifier.weight(1f),
                trackColor = colors.surfaceContainerHigh,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                stringResource(R.string.onboarding_progress, step.ordinal + 1, Step.entries.size),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
        }
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                (fadeIn(tween(120, delayMillis = 60)) togetherWith fadeOut(tween(60))).using(null)
            },
            label = "Onboarding step",
        ) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (page == Step.WELCOME) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(148.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) { SeyirIcon(page.icon, size = 64.dp) }
                    }
                } else {
                    SeyirIconTile(page.icon)
                }
                Text(
                    stringResource(page.title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    stringResource(page.description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
                if (page.field != 0) {
                    val value = when (page) {
                        Step.NAME -> name
                        Step.HEIGHT -> height
                        Step.WEIGHT -> weight
                        else -> goal
                    }
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            if (page == step) {
                                invalidStep = null
                                when (page) {
                                    Step.NAME -> name = it.take(SettingsViewModel.MAX_NAME_LENGTH)
                                    Step.HEIGHT -> height = it
                                    Step.WEIGHT -> weight = it
                                    Step.GOAL -> goal = it
                                    else -> Unit
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(page.field)) },
                        enabled = page == step && editing,
                        singleLine = true,
                        isError = invalidStep == page,
                        supportingText = if (invalidStep == page) {
                            { Text(stringResource(page.error)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (page == Step.NAME) KeyboardType.Text else KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }
                if (page == Step.WEIGHT) {
                    WeightRuler(measurement(weight) ?: 70.0, enabled = page == step && editing) {
                        invalidStep = null
                        weight = String.format(locale, "%.1f", it)
                    }
                    Text(
                        stringResource(R.string.ruler_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (page == Step.REVIEW) ReviewAnswers(answers())
            }
        }
        Column(Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
            if (phase == OnboardingPhase.SAVE_FAILED) {
                Text(
                    stringResource(R.string.onboarding_save_error),
                    color = colors.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }
            val action = when {
                phase == OnboardingPhase.SAVE_FAILED -> R.string.onboarding_retry
                phase == OnboardingPhase.SAVING -> R.string.onboarding_saving
                step == Step.WELCOME -> R.string.onboarding_start
                step == Step.REVIEW -> R.string.onboarding_finish
                else -> R.string.onboarding_continue
            }
            Button(
                onClick = { if (phase == OnboardingPhase.SAVE_FAILED) onRetry() else next() },
                enabled = phase != OnboardingPhase.SAVING,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(action), fontWeight = FontWeight.Bold) }
            if (step != Step.REVIEW && phase != OnboardingPhase.SAVE_FAILED) {
                TextButton(
                    onClick = { next(skip = true) },
                    enabled = editing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(if (step == Step.WELCOME) R.string.onboarding_skip else R.string.onboarding_skip_step))
                }
            }
        }
    }
}

@Composable
private fun ReviewAnswers(answers: OnboardingAnswers) {
    val fields = listOfNotNull(
        answers.name?.let { stringResource(R.string.onboarding_name_field) to it },
        answers.heightCm?.let { stringResource(R.string.onboarding_height_label) to stringResource(R.string.onboarding_cm, it) },
        answers.weightKg?.let { stringResource(R.string.onboarding_weight_label) to stringResource(R.string.weight_kg, it) },
        answers.goalKg?.let { stringResource(R.string.onboarding_goal_label) to stringResource(R.string.weight_kg, it) },
    )
    if (fields.isNotEmpty()) {
        Card {
            fields.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
                }
            }
        }
    }
}

private fun measurement(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

private fun validWeight(text: String): Boolean =
    measurement(text)?.let { it in EditWeightViewModel.MIN_KG..EditWeightViewModel.MAX_KG } == true
