/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.goodtime.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.apps.adrcotfas.goodtime.bl.DomainLabel
import com.apps.adrcotfas.goodtime.bl.TimeUtils.formatMilliseconds
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.data.settings.TimerStyleData
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControlState
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialRegion
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import com.apps.adrcotfas.goodtime.ui.breakColor
import com.apps.adrcotfas.goodtime.ui.getLabelColor
import com.apps.adrcotfas.goodtime.ui.hideUnless
import com.apps.adrcotfas.goodtime.ui.timerFontRobotoMap
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.ic_break
import goodtime_productivity.shared.generated.resources.ic_status_goodtime
import goodtime_productivity.shared.generated.resources.labels_break_budget
import goodtime_productivity.shared.generated.resources.stats_break
import goodtime_productivity.shared.generated.resources.stats_focus
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Composable
fun MainTimerView(
    modifier: Modifier,
    gestureModifier: Modifier,
    state: DialControlState<DialRegion>? = null,
    timerUiState: TimerUiState,
    displayTime: () -> Long,
    timerStyle: TimerStyleData,
    domainLabel: DomainLabel,
    onStart: () -> Unit,
    onToggle: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onBreakBudgetClick: (() -> Unit)? = null,
) {
    val label = domainLabel.label
    val labelColorIndex = label.colorIndex
    val labelColor = MaterialTheme.getLabelColor(labelColorIndex)
    val breakColor = MaterialTheme.breakColor()
    val isBreak = timerUiState.timerType != TimerType.FOCUS

    val isCountdown = domainLabel.profile.isCountdown

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CurrentStatusSection(
            Modifier.hideUnless(timerUiState.isActive),
            color = labelColor,
            isBreak = isBreak,
            isActive = timerUiState.isActive,
            isPaused = timerUiState.isPaused,
            isCountdown = isCountdown,
            streak = timerUiState.longBreakData.streak,
            sessionsBeforeLongBreak = timerUiState.sessionsBeforeLongBreak,
            breakBudget = timerUiState.breakBudgetMinutes,
            showStatus = timerStyle.showStatus,
            showStreak = timerStyle.showStreak,
            showBreakBudget = timerStyle.showBreakBudget && domainLabel.profile.isBreakEnabled && !timerUiState.isBreak,
            onBreakBudgetClick = onBreakBudgetClick,
        )

        TimerTextView(
            modifier = gestureModifier,
            state = state,
            isPaused = timerUiState.isPaused,
            timerStyle = timerStyle,
            millis = displayTime,
            color = if (isBreak) breakColor else labelColor,
            onClick = {
                onToggle?.let {
                    if (!timerUiState.isActive) {
                        onStart()
                    } else {
                        onToggle()
                    }
                }
            },
            onLongClick = onLongClick,
        )

        Spacer(modifier = Modifier.height(statusStripHeight()))
    }
}

/**
 * The sizes below are derived from the text they have to contain and are only ever *added* to,
 * never subtracted from a parent height. Deriving them the other way around
 * (chip height minus padding) leaves the content less room than a line of text at small system
 * font scales, which clips the break budget text - see `adb shell settings put system font_scale 0.7`.
 */
@Composable
private fun statusIconSize(): Dp = with(LocalDensity.current) {
    MaterialTheme.typography.labelLarge.lineHeight
        .toDp() * 0.9f
}

/** Same for all indicators, so they line up. */
@Composable
private fun statusChipHeight(): Dp = statusIconSize() + 10.dp

@Composable
private fun statusStripHeight(): Dp = statusChipHeight() + 10.dp

@Composable
fun CurrentStatusSection(
    modifier: Modifier = Modifier,
    color: Color,
    isBreak: Boolean,
    isActive: Boolean,
    isPaused: Boolean,
    isCountdown: Boolean,
    streak: Int,
    sessionsBeforeLongBreak: Int,
    breakBudget: Long,
    showStatus: Boolean,
    showStreak: Boolean,
    showBreakBudget: Boolean,
    onBreakBudgetClick: (() -> Unit)? = null,
) {
    val statusColor = color.copy(alpha = 0.75f)
    val statusBackgroundColor = color.copy(alpha = 0.15f)

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(statusStripHeight())
            .hideUnless(isActive),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        StatusIndicator(
            showStatus = showStatus,
            isPaused = isPaused,
            isBreak = isBreak,
            color = statusColor,
            backgroundColor = statusBackgroundColor,
        )
        StreakIndicator(
            showStreak = showStreak && isCountdown,
            isBreak = isBreak,
            streak = streak,
            sessionsBeforeLongBreak = sessionsBeforeLongBreak,
            color = statusColor,
            backgroundColor = statusBackgroundColor,
        )
        BreakBudgetIndicator(
            showBreakBudget = showBreakBudget && !isCountdown,
            breakBudget = breakBudget,
            onClick = onBreakBudgetClick,
        )
    }
}

@Composable
fun StatusIndicator(
    showStatus: Boolean,
    isPaused: Boolean,
    isBreak: Boolean,
    color: Color,
    backgroundColor: Color,
) {
    val alpha = remember(isPaused) { Animatable(1f) }
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            delay(500.milliseconds)
            alpha.animateTo(
                targetValue = 0.3f,
                animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
        } else {
            alpha.animateTo(targetValue = 1f, animationSpec = tween(200))
        }
    }

    AnimatedVisibility(
        showStatus,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
    ) {
        Box(
            modifier =
            Modifier
                .graphicsLayer { this.alpha = alpha.value }
                .padding(horizontal = 4.dp)
                .size(statusChipHeight())
                .clip(MaterialTheme.shapes.small)
                .background(backgroundColor),
        ) {
            Crossfade(
                modifier = Modifier.align(Alignment.Center),
                targetState = isBreak,
                label = "label icon",
            ) {
                if (it) {
                    Image(
                        modifier = Modifier.size(statusIconSize()),
                        colorFilter = ColorFilter.tint(color),
                        painter = painterResource(Res.drawable.ic_break),
                        contentDescription = stringResource(Res.string.stats_break),
                    )
                } else {
                    Image(
                        modifier = Modifier.size(statusIconSize()),
                        colorFilter = ColorFilter.tint(color),
                        painter = painterResource(Res.drawable.ic_status_goodtime),
                        contentDescription = stringResource(Res.string.stats_focus),
                    )
                }
            }
        }
    }
}

@Composable
fun StreakIndicator(
    showStreak: Boolean,
    isBreak: Boolean,
    streak: Int,
    sessionsBeforeLongBreak: Int,
    color: Color,
    backgroundColor: Color,
) {
    if (sessionsBeforeLongBreak >= 2) {
        AnimatedVisibility(
            showStreak,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Box(
                modifier =
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(statusChipHeight())
                    .clip(MaterialTheme.shapes.small)
                    .background(backgroundColor),
            ) {
                val numerator =
                    (streak % sessionsBeforeLongBreak).run {
                        plus(
                            if (!isBreak) {
                                1
                            } else if (this == 0 && streak != 0) {
                                sessionsBeforeLongBreak
                            } else {
                                0
                            },
                        )
                    }
                FractionText(
                    modifier = Modifier.align(Alignment.Center),
                    numerator = numerator,
                    denominator = sessionsBeforeLongBreak,
                    color = color,
                )
            }
        }
    }
}

@Composable
fun BreakBudgetIndicator(
    showBreakBudget: Boolean,
    breakBudget: Long,
    onClick: (() -> Unit)? = null,
) {
    val color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    val backgroundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)

    AnimatedVisibility(
        showBreakBudget,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
    ) {
        val iconSize =
            with(LocalDensity.current) {
                MaterialTheme.typography.labelSmall.lineHeight
                    .toDp()
            }
        Box(
            modifier =
            Modifier
                .padding(horizontal = 4.dp)
                .height(statusChipHeight())
                .clip(MaterialTheme.shapes.small)
                .background(backgroundColor)
                .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)
                .padding(horizontal = 6.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(color),
                    painter = painterResource(Res.drawable.ic_break),
                    contentDescription = stringResource(Res.string.labels_break_budget),
                )
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = breakBudget.minutes.formatOverview(),
                    style =
                    MaterialTheme.typography.labelSmall.copy(
                        color = color,
                    ),
                )
            }
        }
    }
}

private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸"
private const val SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈"

@Composable
fun FractionText(
    modifier: Modifier,
    numerator: Int,
    denominator: Int,
    color: Color,
) {
    val baseStyle =
        MaterialTheme.typography.labelLarge
            .copy(
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = TextUnit(0.0f, TextUnitType.Sp),
            ).toSpanStyle()

    val annotatedString =
        buildAnnotatedString {
            withStyle(baseStyle.copy(letterSpacing = TextUnit(-0.1f, TextUnitType.Em))) {
                append(SUPERSCRIPTS[numerator])
            }
            withStyle(baseStyle) {
                append("⁄")
            }
            withStyle(baseStyle.copy(letterSpacing = TextUnit(-0.3f, TextUnitType.Em))) {
                append(SUBSCRIPTS[denominator])
            }
        }

    Text(
        modifier = modifier.then(Modifier.padding(end = 1.dp)),
        text = annotatedString,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerTextView(
    modifier: Modifier,
    state: DialControlState<DialRegion>? = null,
    millis: () -> Long,
    color: Color,
    timerStyle: TimerStyleData,
    isPaused: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val fontMap = timerFontRobotoMap()
    val scale by animateFloatAsState(
        targetValue = if (state?.isPressed == true) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "timer scale",
    )

    val alpha = remember { Animatable(1f) }
    LaunchedEffect(isPaused) {
        if (isPaused) {
            delay(200.milliseconds)
            alpha.animateTo(
                targetValue = 0.3f,
                animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
        } else {
            alpha.animateTo(targetValue = 1f, animationSpec = tween(200))
        }
    }

    val clickableModifier =
        onClick?.let {
            Modifier.combinedClickable(
                indication = null,
                interactionSource = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        } ?: Modifier
    Text(
        modifier =
        Modifier
            .then(modifier)
            // the lambda overload keeps the animation reads out of the composition, otherwise the
            // blinking while paused recomposes this text on every frame
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha.value
            }
            .then(clickableModifier),
        text = millis().formatMilliseconds(timerStyle.minutesOnly),
        style =
        TextStyle(
            fontSize = timerStyle.inUseFontSize().em,
            fontFamily = fontMap[timerStyle.fontWeight],
            color = color,
        ),
    )
}

/** Status and break budget indicators, the combination used in flow mode. */
@Composable
private fun StatusAndBreakBudget(breakBudget: Long = 30) {
    CurrentStatusSection(
        color = MaterialTheme.getLabelColor(13),
        isBreak = false,
        isActive = true,
        isPaused = false,
        isCountdown = false,
        streak = 2,
        sessionsBeforeLongBreak = 3,
        breakBudget = breakBudget,
        showStatus = true,
        showStreak = true,
        showBreakBudget = true,
    )
}

@Preview
@Composable
fun CurrentStatusSectionPreview() {
    ApplicationTheme {
        StatusAndBreakBudget()
    }
}

/**
 * Both indicators must stay the same height and the break budget text must stay unclipped at
 * every font scale - it used to be cut in half below 1.0.
 */
@Preview
@Composable
fun CurrentStatusSectionFontScalesPreview() {
    ApplicationTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            listOf(0.7f, 0.85f, 1f, 1.3f).forEach { fontScale ->
                CompositionLocalProvider(
                    LocalDensity provides
                        Density(density = LocalDensity.current.density, fontScale = fontScale),
                ) {
                    StatusAndBreakBudget(breakBudget = 9)
                }
            }
        }
    }
}
