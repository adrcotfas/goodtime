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

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.apps.adrcotfas.goodtime.data.settings.isDarkTheme
import com.apps.adrcotfas.goodtime.main.MainTimerView
import com.apps.adrcotfas.goodtime.main.TimerMainUiState
import com.apps.adrcotfas.goodtime.main.TimerUiState
import com.apps.adrcotfas.goodtime.main.TimerViewModel
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.main_break_complete
import goodtime_productivity.shared.generated.resources.main_focus_complete
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val pipAspectRatio = Rational(5, 3)

private fun pipParams(autoEnter: Boolean): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder().setAspectRatio(pipAspectRatio)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
    }
    return builder.build()
}

private fun shouldEnterPip(
    timerUiState: TimerUiState,
    uiState: TimerMainUiState,
) = timerUiState.isActive && uiState.isPro && uiState.pipMode

fun ComponentActivity.setupPictureInPicture(
    timerViewModel: TimerViewModel,
    onPipModeChanged: (Boolean) -> Unit,
) {
    addOnPictureInPictureModeChangedListener { onPipModeChanged(it.isInPictureInPictureMode) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        lifecycleScope.launch {
            combine(timerViewModel.timerUiState, timerViewModel.uiState, ::shouldEnterPip)
                .distinctUntilChanged()
                .collect { setPictureInPictureParams(pipParams(autoEnter = it)) }
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

    val isDarkTheme = uiState.darkThemePreference.isDarkTheme(isSystemInDarkTheme())
    ApplicationTheme(darkTheme = isDarkTheme, dynamicColor = uiState.dynamicColor) {
        val backgroundColor =
            if (isDarkTheme && uiState.trueBlackMode && timerUiState.isActive) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surface
            }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            if (timerUiState.isFinished) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text =
                    stringResource(
                        if (timerUiState.isBreak) {
                            Res.string.main_break_complete
                        } else {
                            Res.string.main_focus_complete
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
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
