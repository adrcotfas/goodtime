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

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.apps.adrcotfas.goodtime.BuildConfig
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.bl.FinishActionType
import com.apps.adrcotfas.goodtime.bl.getLabelData
import com.apps.adrcotfas.goodtime.common.askForAlarmPermission
import com.apps.adrcotfas.goodtime.common.installIsOlderThan10Days
import com.apps.adrcotfas.goodtime.common.isPortrait
import com.apps.adrcotfas.goodtime.common.screenWidth
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.settings.isDarkTheme
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialConfig
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControl
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControlButton
import com.apps.adrcotfas.goodtime.main.dialcontrol.rememberCustomDialControlState
import com.apps.adrcotfas.goodtime.main.dialcontrol.updateEnabledOptions
import com.apps.adrcotfas.goodtime.main.finishedsession.FinishedSessionSheet
import com.apps.adrcotfas.goodtime.onboarding.MainUiState
import com.apps.adrcotfas.goodtime.onboarding.MainViewModel
import com.apps.adrcotfas.goodtime.onboarding.tutorial.TutorialScreen
import com.apps.adrcotfas.goodtime.settings.permissions.getPermissionsState
import com.apps.adrcotfas.goodtime.settings.timerstyle.InitTimerStyle
import com.apps.adrcotfas.goodtime.ui.common.SnackbarAction
import com.apps.adrcotfas.goodtime.ui.common.SnackbarController
import com.apps.adrcotfas.goodtime.ui.common.SnackbarEvent
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    navController: NavController,
    onSurfaceClick: () -> Unit,
    hideBottomBar: Boolean,
    viewModel: TimerViewModel,
    mainViewModel: MainViewModel,
    onUpdateClicked: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(TimerMainUiState())
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle(MainUiState())

    val updateAvailable = mainUiState.isUpdateAvailable
    val wasNotificationPermissionDenied = mainUiState.wasNotificationPermissionDenied

    if (uiState.isLoading) return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    InitTimerStyle(viewModel)

    LifecycleResumeEffect(Unit) {
        viewModel.refreshStartOfToday()
        onPauseOrDispose {
            // do nothing
        }
    }

    val timerUiState by viewModel.timerUiState.collectAsStateWithLifecycle(TimerUiState())

    val timerStyle = uiState.timerStyle
    val label = timerUiState.label

    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current

    val dialControlState =
        rememberCustomDialControlState(
            config = DialConfig(),
            onLeft = viewModel::skip,
            onTop = viewModel::addOneMinute,
            onRight = viewModel::skip,
            onBottom = viewModel::resetTimer,
        )

    dialControlState.updateEnabledOptions(timerUiState)
    val gestureModifier =
        dialControlState.let {
            Modifier
                .pointerInput(it) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        it.onDown(position = down.position)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        var change =
                            awaitTouchSlopOrCancellation(pointerId = down.id) { change, _ ->
                                change.consume()
                            }
                        while (change != null && change.pressed) {
                            change =
                                awaitDragOrCancellation(change.id)?.also { inputChange ->
                                    if (inputChange.pressed && timerUiState.isActive) {
                                        dialControlState.onDrag(dragAmount = inputChange.positionChange())
                                    }
                                }
                        }
                        it.onRelease()
                    }
                }
        }

    val yOffset = remember { Animatable(0f) }
    ScreensaverMode(
        screensaverMode = uiState.screensaverMode,
        isActive = timerUiState.isActive,
        screenWidth = configuration.screenWidth,
        yOffset = yOffset,
    )

    val isDarkTheme = uiState.darkThemePreference.isDarkTheme(isSystemInDarkTheme())
    val backgroundColor by animateColorAsState(
        if (isDarkTheme &&
            uiState.trueBlackMode &&
            timerUiState.isActive
        ) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "main background color",
    )

    val flashScreenBackgroundColor by
        if (timerUiState.isFinished && uiState.flashScreen) {
            val infiniteTransition = rememberInfiniteTransition(label = "flash")
            infiniteTransition.animateColor(
                initialValue = backgroundColor,
                targetValue = MaterialTheme.colorScheme.onSurface,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(200, easing = FastOutLinearInEasing, delayMillis = 2000),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "flashColor",
            )
        } else {
            rememberUpdatedState(backgroundColor)
        }

    val permissionState = getPermissionsState()
    val actionBadgeItemCount = permissionState.count() + if (updateAvailable) 1 else 0
    val interactionSource = remember { MutableInteractionSource() }

    var showNavigationSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectLabelDialog by rememberSaveable { mutableStateOf(false) }

    val showTutorial = uiState.showTutorial

    AnimatedVisibility(
        timerUiState.isReady,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            onSurfaceClick()
                        },
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(flashScreenBackgroundColor)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentAlignment = Alignment.Center,
                ) {
                    val tutorialModifier =
                        if (showTutorial) Modifier.blur(radius = 4.dp) else Modifier
                    val modifier =
                        Modifier.offset {
                            if (configuration.isPortrait) {
                                IntOffset(
                                    0,
                                    yOffset.value.roundToInt(),
                                )
                            } else {
                                IntOffset(yOffset.value.roundToInt(), 0)
                            }
                        }

                    val alphaModifier =
                        Modifier.graphicsLayer {
                            alpha = if (dialControlState.isDragging) 0.38f else 1f
                        }
                    MainTimerView(
                        modifier = alphaModifier.then(modifier),
                        state = dialControlState,
                        gestureModifier = gestureModifier.then(tutorialModifier),
                        timerUiState = timerUiState,
                        timerStyle = timerStyle,
                        domainLabel = label,
                        onStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (permissionState.shouldAskForAlarmPermission) {
                                coroutineScope.launch {
                                    showAlarmPermissionSnackbar(context)
                                }
                            } else {
                                viewModel.startTimer()
                            }
                        },
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (permissionState.shouldAskForAlarmPermission) {
                                coroutineScope.launch {
                                    showAlarmPermissionSnackbar(context)
                                }
                            } else {
                                viewModel.toggleTimer()
                            }
                        },
                        onLongClick = { navController.navigate(SettingsDest) },
                    )
                    DialControl(
                        modifier = modifier,
                        state = dialControlState,
                        dialContent = { region ->
                            DialControlButton(
                                enabled = !dialControlState.isDisabled(region),
                                selected = region == dialControlState.selectedOption,
                                region = region,
                            )
                        },
                    )
                    if (showTutorial) {
                        TutorialScreen(
                            onClose = { viewModel.setShowTutorial(false) },
                        )
                    } else {
                        BottomAppBar(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            hide = hideBottomBar,
                            onShowSheet = { showNavigationSheet = true },
                            onLabelClick = { showSelectLabelDialog = true },
                            labelData = label.getLabelData(),
                            sessionCountToday = uiState.sessionCountToday,
                            badgeItemCount = actionBadgeItemCount,
                            navController = navController,
                        )
                    }
                }
            }
        }
    }

    if (showNavigationSheet) {
        MainNavigationSheet(
            onHideSheet = { showNavigationSheet = false },
            navController = navController,
            onUpdateClicked = onUpdateClicked,
            actionBadgeCount = actionBadgeItemCount,
            showPro = BuildConfig.IS_FDROID || !uiState.isPro,
            isUpdateAvailable = updateAvailable,
            wasNotificationPermissionDenied = wasNotificationPermissionDenied,
            onNotificationPermissionGranted = { granted: Boolean ->
                mainViewModel.setNotificationPermissionGranted(granted)
            },
        )
    }

    var showFinishedSessionSheet by remember(timerUiState.isFinished) {
        mutableStateOf(timerUiState.isFinished)
    }

    if (showFinishedSessionSheet) {
        FinishedSessionSheet(
            timerUiState = timerUiState,
            onHideSheet = { showFinishedSessionSheet = false },
            onNext = { updateWorkTime ->
                viewModel.next(updateWorkTime)
                // ask for in app review if the user just started a break session
                if (context.installIsOlderThan10Days() && !timerUiState.isBreak) {
                    viewModel.setShouldAskForReview()
                }
            },
            onReset = { updateWorkTime ->
                if (updateWorkTime) {
                    viewModel.resetTimer(updateWorkTime = true)
                } else {
                    viewModel.resetTimer(false, actionType = FinishActionType.MANUAL_DO_NOTHING)
                }
            },
            onUpdateNotes = viewModel::updateNotesForLastCompletedSession,
        )
    }

    if (showSelectLabelDialog) {
        SelectActiveLabelDialog(
            initialSelectedLabel = timerUiState.label.label.name,
            onDismiss = { showSelectLabelDialog = false },
            onConfirm = { selectedLabels ->
                if (selectedLabels.isNotEmpty()) {
                    val first = selectedLabels.first()
                    if (first != label.label.name) {
                        viewModel.setActiveLabel(first)
                    }
                }
                showSelectLabelDialog = false
            },
            onNavigateToLabels = {
                navController.navigate(LabelsDest)
                showSelectLabelDialog = false
            },
            onNavigateToActiveLabel = {
                navController.navigate(AddEditLabelDest(name = timerUiState.label.getLabelName()))
                showSelectLabelDialog = false
            },
            onNavigateToTimerDurations = {
                navController.navigate(TimerDurationsDest)
                showSelectLabelDialog = false
            },
            onClearLabel = {
                viewModel.setActiveLabel(Label.DEFAULT_LABEL_NAME)
                showSelectLabelDialog = false
            },
        )
    }
}

private suspend fun showAlarmPermissionSnackbar(context: Context) {
    SnackbarController.sendEvent(
        event =
            SnackbarEvent(
                message = context.getString(R.string.settings_allow_alarms),
                action =
                    SnackbarAction(
                        name = context.getString(R.string.settings_allow),
                        action = {
                            context.askForAlarmPermission()
                        },
                    ),
            ),
    )
}
