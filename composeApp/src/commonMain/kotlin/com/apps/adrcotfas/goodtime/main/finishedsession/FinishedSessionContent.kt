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
package com.apps.adrcotfas.goodtime.main.finishedsession

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.apps.adrcotfas.goodtime.bl.TimeUtils.formatMilliseconds
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.bl.isBreak
import com.apps.adrcotfas.goodtime.common.formatOverview
import com.apps.adrcotfas.goodtime.main.TimerUiState
import com.apps.adrcotfas.goodtime.ui.DragHandle
import com.apps.adrcotfas.goodtime.ui.FullscreenEffect
import com.apps.adrcotfas.goodtime.ui.TextBox
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedSessionSheet(
    timerUiState: TimerUiState,
    onNext: () -> Unit,
    onReset: () -> Unit,
    onUpdateFinishedSession: (updateDuration: Boolean, notes: String) -> Unit,
    onHideSheet: () -> Unit,
) {
    val viewModelStore = remember { ViewModelStore() }
    val localOwner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = viewModelStore
            }
        }
    CompositionLocalProvider(LocalViewModelStoreOwner provides localOwner) {
        val viewModel: FinishedSessionViewModel = koinViewModel()
        DisposableEffect(Unit) {
            onDispose { viewModelStore.clear() }
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        val finishedSessionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hideFinishedSessionSheet = {
            coroutineScope.launch { finishedSessionSheetState.hide() }.invokeOnCompletion {
                if (!finishedSessionSheetState.isVisible) {
                    onHideSheet()
                }
            }
        }

        val isBreak = rememberSaveable { timerUiState.timerType.isBreak }
        var addIdleTime by rememberSaveable { mutableStateOf(false) }
        var notes by rememberSaveable { mutableStateOf("") }
        val isFullscreen = uiState.isFullscreen

        // Auto-dismiss after 30 minutes since session ended (ViewModel updates elapsedRealtime)
        LaunchedEffect(timerUiState.isWithinInactivityTimeout) {
            if (!timerUiState.isWithinInactivityTimeout) {
                onReset()
                onHideSheet()
            }
        }

        val handleSheetClose = {
            // Unified update logic - no race conditions!
            val notesModified = notes.isNotEmpty()
            if (addIdleTime) {
                // Update duration, timestamp, AND notes (even if empty)
                onUpdateFinishedSession(true, notes)
            } else if (notesModified) {
                // Only update notes
                onUpdateFinishedSession(false, notes)
            }
            // If addIdleTime=false and notes empty: do nothing, just reset
            onReset()
        }

        val handleNext = {
            // Unified update logic - no race conditions!
            val notesModified = notes.isNotEmpty()
            if (addIdleTime) {
                // Update duration, timestamp, AND notes (even if empty)
                onUpdateFinishedSession(true, notes)
            } else if (notesModified) {
                // Only update notes
                onUpdateFinishedSession(false, notes)
            }
            // If addIdleTime=false and notes empty: do nothing, just proceed with next
            onNext()
        }

        ModalBottomSheet(
            onDismissRequest = {
                handleSheetClose()
                onHideSheet()
            },
            dragHandle = {
                if (!uiState.isLoading) {
                    DragHandle(
                        buttonText =
                            if (isBreak) {
                                uiState.strings.mainStartFocus
                            } else {
                                uiState.strings.mainStartBreak
                            },
                        onClose = {
                            handleSheetClose()
                            hideFinishedSessionSheet()
                        },
                        onClick = {
                            handleNext()
                            hideFinishedSessionSheet()
                        },
                        isEnabled = true,
                    )
                }
            },
            sheetState = finishedSessionSheetState,
        ) {
            if (isFullscreen) {
                FullscreenEffect()
            }

            if (uiState.isLoading) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                FinishedSessionContent(
                    timerUiState = timerUiState,
                    finishedSessionUiState = uiState,
                    addIdleMinutes = addIdleTime,
                    onChangeAddIdleMinutes = { addIdleTime = it },
                    notes = notes,
                    onNotesChanged = { notes = it },
                )
            }
        }
    }
}

@Composable
private fun FinishedSessionContent(
    timerUiState: TimerUiState,
    finishedSessionUiState: FinishedSessionUiState,
    addIdleMinutes: Boolean,
    onChangeAddIdleMinutes: (Boolean) -> Unit,
    notes: String,
    onNotesChanged: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val isBreak = timerUiState.timerType.isBreak
        val strings = finishedSessionUiState.strings
        Text(
            text =
                if (isBreak) {
                    strings.mainBreakComplete
                } else {
                    strings.mainFocusComplete
                },
            style = MaterialTheme.typography.titleLarge,
        )
        CurrentSessionCard(
            timerUiState,
            addIdleMinutes,
            onChangeAddIdleMinutes,
            finishedSessionUiState.isPro,
            notes,
            onNotesChanged,
            strings,
        )
        HistoryCard(finishedSessionUiState)
    }
}

@Composable
private fun CurrentSessionCard(
    timerUiState: TimerUiState,
    addIdleMinutes: Boolean,
    onAddIdleMinutesChanged: (Boolean) -> Unit,
    enabled: Boolean,
    notes: String,
    onNotesChanged: (String) -> Unit,
    strings: FinishedSessionStrings,
) {
    val isBreak = timerUiState.isBreak
    val idleMillis = timerUiState.idleTime

    val duration =
        timerUiState.completedMinutes.minutes.inWholeMilliseconds
            .plus(
                if (addIdleMinutes) idleMillis else 0,
            ).milliseconds
            .formatOverview()
    Card(modifier = Modifier.wrapContentSize()) {
        Column(
            modifier =
                Modifier
                    .animateContentSize()
                    .padding(16.dp),
        ) {
            Text(
                strings.mainThisSession,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.wrapContentHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        if (isBreak) strings.statsBreak else strings.statsFocus,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        duration,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }

                if (!isBreak) {
                    val interruptions = timerUiState.timeSpentPaused.milliseconds.inWholeMinutes
                    if (interruptions > 0) {
                        Column(
                            modifier = Modifier.wrapContentHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                strings.mainInterruptions,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                interruptions.minutes.formatOverview(),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }

                    if (idleMillis > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                modifier = Modifier.wrapContentHeight(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Text(strings.mainIdle, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    idleMillis.formatMilliseconds(),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Crossfade(
                                modifier = Modifier.size(36.dp),
                                targetState = addIdleMinutes,
                            ) {
                                if (it) {
                                    FilledTonalIconButton(onClick = { onAddIdleMinutesChanged(false) }) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = strings.mainConsiderIdleTimeAsExtraFocus,
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { onAddIdleMinutesChanged(true) }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = strings.mainConsiderIdleTimeAsExtraFocus,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TextBox(
                modifier = Modifier.padding(top = 12.dp),
                value = notes,
                onValueChange = onNotesChanged,
                enabled = enabled,
                placeholder = strings.statsAddNotes,
            )
        }
    }
}

@Composable
fun HistoryCard(finishedSessionUiState: FinishedSessionUiState) {
    if (finishedSessionUiState.todayWorkMinutes > 0 || finishedSessionUiState.todayBreakMinutes > 0) {
        val strings = finishedSessionUiState.strings
        Card(
            modifier =
                Modifier
                    .wrapContentSize()
                    .padding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .animateContentSize(),
            ) {
                Text(
                    strings.statsToday,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.wrapContentHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            strings.statsFocus,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            finishedSessionUiState.todayWorkMinutes.minutes.formatOverview(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Column(
                        modifier = Modifier.wrapContentHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            strings.statsBreak,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            finishedSessionUiState.todayBreakMinutes.minutes.formatOverview(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    if (finishedSessionUiState.todayInterruptedMinutes > 0) {
                        Column(
                            modifier = Modifier.wrapContentHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                strings.mainInterruptions,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                finishedSessionUiState.todayInterruptedMinutes.minutes.formatOverview(),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun FinishedSessionContentPreview() {
    FinishedSessionContent(
        timerUiState =
            TimerUiState(
                timerType = TimerType.FOCUS,
                completedMinutes = 25,
                timeSpentPaused = 2.minutes.inWholeMilliseconds,
                endTime = 0,
                elapsedRealtime = 3.minutes.inWholeMilliseconds,
            ),
        finishedSessionUiState =
            FinishedSessionUiState(
                todayWorkMinutes = 90,
                todayBreakMinutes = 55,
                todayInterruptedMinutes = 2,
                isPro = false,
            ),
        addIdleMinutes = true,
        onChangeAddIdleMinutes = {},
        notes = "Some notes",
        onNotesChanged = {},
    )
}
