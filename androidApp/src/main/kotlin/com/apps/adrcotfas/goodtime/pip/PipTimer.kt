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
package com.apps.adrcotfas.goodtime.pip

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.bl.TimerService
import com.apps.adrcotfas.goodtime.data.settings.isDarkTheme
import com.apps.adrcotfas.goodtime.main.MainTimerView
import com.apps.adrcotfas.goodtime.main.TimerMainUiState
import com.apps.adrcotfas.goodtime.main.TimerUiState
import com.apps.adrcotfas.goodtime.main.TimerViewModel
import com.apps.adrcotfas.goodtime.main.finishedsession.PipFinishedSessionContent
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.main_pause
import goodtime_productivity.shared.generated.resources.main_resume
import goodtime_productivity.shared.generated.resources.main_start_break
import goodtime_productivity.shared.generated.resources.main_start_focus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private val pipAspectRatio = Rational(5, 3)

private fun pipParams(
    autoEnter: Boolean,
    sourceRectHint: Rect? = null,
    actions: List<RemoteAction>? = null,
): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder().setAspectRatio(pipAspectRatio)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
    }
    sourceRectHint?.let { builder.setSourceRectHint(it) }
    actions?.let { builder.setActions(it) }
    return builder.build()
}

/**
 * Without a source rect hint the system masks the enter-PiP resize with an
 * app-icon overlay that looks like a splash screen; hint the region the timer
 * occupies so it animates the content instead. The timer is centered within
 * the safe-drawing insets, so center the hint there too.
 */
private fun ComponentActivity.sourceRectHint(): Rect? {
    val decor = window.decorView
    if (decor.width == 0 || decor.height == 0) return null
    val insets =
        ViewCompat.getRootWindowInsets(decor)?.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        ) ?: Insets.NONE
    val left = insets.left
    val right = decor.width - insets.right
    val centerY = (insets.top + decor.height - insets.bottom) / 2
    val height = (right - left) * pipAspectRatio.denominator / pipAspectRatio.numerator
    val top = (centerY - height / 2).coerceAtLeast(0)
    return Rect(left, top, right, top + height)
}

private fun shouldEnterPip(
    timerUiState: TimerUiState,
    uiState: TimerMainUiState,
) = timerUiState.isActive && uiState.isPro && uiState.pipMode && !uiState.isLoading

private data class PipState(
    val autoEnter: Boolean,
    val isActive: Boolean,
    val isPaused: Boolean,
    val isBreak: Boolean,
    val isCountdownProfile: Boolean,
    val isBreakEnabled: Boolean,
    val hasBreakBudget: Boolean,
    val isFinished: Boolean,
)

private suspend fun Context.pipActions(state: PipState): List<RemoteAction> {
    if (state.isFinished) {
        return listOf(
            remoteAction(
                iconRes = R.drawable.ic_skip_next,
                title =
                getString(
                    if (!state.isBreak && state.isBreakEnabled) {
                        Res.string.main_start_break
                    } else {
                        Res.string.main_start_focus
                    },
                ),
                action = TimerService.Companion.Action.Next,
            ),
        )
    }
    if (!state.isActive) return emptyList()
    return buildList {
        if (state.isCountdownProfile || !state.isBreak) {
            add(
                remoteAction(
                    iconRes = if (state.isPaused) R.drawable.ic_play else R.drawable.ic_pause,
                    title = getString(if (state.isPaused) Res.string.main_resume else Res.string.main_pause),
                    action = TimerService.Companion.Action.Toggle,
                ),
            )
        }
        // count-up focus can start a break only with budget; a break can always skip back
        val canSkip =
            state.isBreakEnabled &&
                (state.isCountdownProfile || state.isBreak || state.hasBreakBudget)
        if (canSkip) {
            add(
                remoteAction(
                    iconRes = R.drawable.ic_skip_next,
                    title = getString(if (state.isBreak) Res.string.main_start_focus else Res.string.main_start_break),
                    action = TimerService.Companion.Action.Skip,
                ),
            )
        }
    }
}

private fun Context.remoteAction(
    iconRes: Int,
    title: String,
    action: TimerService.Companion.Action,
) = RemoteAction(
    Icon.createWithResource(this, iconRes),
    title,
    title,
    PendingIntent.getService(
        this,
        0,
        TimerService.createIntentWithAction(this, action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    ),
)

fun ComponentActivity.setupPictureInPicture(
    timerViewModel: TimerViewModel,
    onPipModeChanged: (Boolean) -> Unit,
) {
    addOnPictureInPictureModeChangedListener { onPipModeChanged(it.isInPictureInPictureMode) }
    var lastState: PipState? = null
    suspend fun updateParams(state: PipState) = setPictureInPictureParams(pipParams(state.autoEnter, sourceRectHint(), pipActions(state)))
    lifecycleScope.launch {
        combine(timerViewModel.timerUiState, timerViewModel.uiState) { timerUiState, uiState ->
            PipState(
                autoEnter = shouldEnterPip(timerUiState, uiState),
                isActive = timerUiState.isActive,
                isPaused = timerUiState.isPaused,
                isBreak = timerUiState.isBreak,
                isCountdownProfile = timerUiState.label.profile.isCountdown,
                isBreakEnabled = timerUiState.label.profile.isBreakEnabled,
                hasBreakBudget = timerUiState.breakBudgetMinutes > 0,
                isFinished = timerUiState.isFinished,
            )
        }.distinctUntilChanged()
            .collect {
                lastState = it
                updateParams(it)
            }
    }
    // the first collect can run before layout, when the source rect is not yet computable
    window.decorView.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
        if (r - l != or - ol || b - t != ob - ot) {
            lastState?.let { lifecycleScope.launch { updateParams(it) } }
        }
    }
}

/**
 * Pre-S has no auto-enter support; call from [ComponentActivity.onUserLeaveHint].
 */
fun ComponentActivity.enterPipIfNeeded(timerViewModel: TimerViewModel) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
        shouldEnterPip(timerViewModel.timerUiState.value, timerViewModel.uiState.value)
    ) {
        enterPictureInPictureMode(pipParams(autoEnter = true))
    }
}

@Composable
fun PipTimerScreen(viewModel: TimerViewModel) {
    val timerUiState by viewModel.timerUiState.collectAsStateWithLifecycle()
    val displayTime by viewModel.displayTime.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val idleTime by viewModel.idleTime.collectAsStateWithLifecycle()

    val isDarkTheme = uiState.darkThemePreference.isDarkTheme(isSystemInDarkTheme())
    ApplicationTheme(darkTheme = isDarkTheme, dynamicColor = uiState.dynamicColor) {
        val backgroundColor =
            if (isDarkTheme && uiState.trueBlackMode && timerUiState.isActive) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surface
            }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (timerUiState.isFinished) {
                    PipFinishedSessionContent(
                        timerUiState = timerUiState,
                        idleTime = { idleTime },
                    )
                } else {
                    val timerStyle = uiState.timerStyle
                    val scale =
                        if (timerStyle.currentScreenWidth > 0f) {
                            maxWidth.value / timerStyle.currentScreenWidth
                        } else {
                            1f
                        }
                    MainTimerView(
                        modifier =
                        Modifier
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .wrapContentHeight(unbounded = true)
                            .requiredWidth(timerStyle.currentScreenWidth.dp),
                        gestureModifier = Modifier,
                        timerUiState = timerUiState,
                        displayTime = { displayTime },
                        timerStyle = timerStyle,
                        domainLabel = timerUiState.label,
                        onStart = {},
                    )
                }
            }
        }
    }
}
