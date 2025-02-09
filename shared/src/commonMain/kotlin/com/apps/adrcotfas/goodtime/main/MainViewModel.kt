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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.bl.DomainLabel
import com.apps.adrcotfas.goodtime.bl.DomainTimerData
import com.apps.adrcotfas.goodtime.bl.FinishActionType
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.TimerManager
import com.apps.adrcotfas.goodtime.bl.TimerState
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.bl.getBaseTime
import com.apps.adrcotfas.goodtime.bl.isActive
import com.apps.adrcotfas.goodtime.bl.isBreak
import com.apps.adrcotfas.goodtime.bl.isPaused
import com.apps.adrcotfas.goodtime.bl.isRunning
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.ThemePreference
import com.apps.adrcotfas.goodtime.data.settings.TimerStyleData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max

data class TimerUiState(
    val isReady: Boolean = false,
    val label: DomainLabel = DomainLabel(),
    val baseTime: Long = 0,
    val timerState: TimerState = TimerState.RESET,
    val timerType: TimerType = TimerType.WORK,
    val completedMinutes: Long = 0,
    val timeSpentPaused: Long = 0,
    val endTime: Long = 0,
    val sessionsBeforeLongBreak: Int = 0,
    val longBreakData: LongBreakData = LongBreakData(),
    val breakBudgetMinutes: Long = 0,
) {
    fun workSessionIsInProgress(): Boolean {
        return timerState.isActive && timerType == TimerType.WORK
    }

    val displayTime = max(baseTime, 0)

    val isRunning = timerState.isRunning
    val isPaused = timerState.isPaused
    val isActive = timerState.isActive
    val isBreak = timerType.isBreak
    val isFinished = timerState == TimerState.FINISHED
}

data class MainUiState(
    val isLoading: Boolean = false,
    val timerStyle: TimerStyleData = TimerStyleData(),
    val darkThemePreference: ThemePreference = ThemePreference.SYSTEM,
    val dynamicColor: Boolean = false,
    val screensaverMode: Boolean = false,
    val fullscreenMode: Boolean = false,
    val trueBlackMode: Boolean = true,
    val dndDuringWork: Boolean = false,
    val isMainScreen: Boolean = true,
) {
    fun isDarkTheme(isSystemInDarkTheme: Boolean): Boolean {
        return darkThemePreference == ThemePreference.DARK ||
            (darkThemePreference == ThemePreference.SYSTEM && isSystemInDarkTheme)
    }
}

class MainViewModel(
    private val timerManager: TimerManager,
    private val timeProvider: TimeProvider,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val timerUiState = timerManager.timerData.flatMapLatest {
        when (it.state) {
            TimerState.RUNNING, TimerState.PAUSED -> flow {
                while (true) {
                    emitUiState(it)
                    delay(1000)
                }
            }

            else -> {
                flow { emitUiState(it) }
            }
        }
    } // TODO: .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimerUiState())

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.onStart {
        loadData()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            settingsRepo.settings.map { Pair(it.timerStyle, it.uiSettings) }.distinctUntilChanged()
                .collect { (timerStyle, uiSettings) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            timerStyle = timerStyle,
                            darkThemePreference = uiSettings.themePreference,
                            dynamicColor = uiSettings.useDynamicColor,
                            screensaverMode = uiSettings.screenSaverModePossible(),
                            fullscreenMode = uiSettings.fullscreenMode,
                            trueBlackMode = uiSettings.trueBlackModePossible(),
                            dndDuringWork = uiSettings.dndDuringWork,
                        )
                    }
                }
        }
    }

    fun startTimer(type: TimerType = TimerType.WORK) {
        timerManager.start(type)
    }

    fun toggleTimer(): Boolean {
        val canToggle = timerManager.canToggle()
        if (canToggle) {
            timerManager.toggle()
            return true
        } else {
            return false
        }
    }

    fun resetTimer(
        updateWorkTime: Boolean = false,
        actionType: FinishActionType = FinishActionType.MANUAL_RESET,
    ) {
        timerManager.reset(updateWorkTime, actionType)
    }

    fun addOneMinute() {
        timerManager.addOneMinute()
    }

    fun setIsMainScreen(isMainScreen: Boolean) {
        _uiState.update {
            it.copy(isMainScreen = isMainScreen)
        }
    }

    private suspend fun FlowCollector<TimerUiState>.emitUiState(
        it: DomainTimerData,
    ) {
        emit(
            TimerUiState(
                isReady = it.isReady,
                label = it.label,
                baseTime = it.getBaseTime(timeProvider),
                timerState = it.state,
                timerType = it.type,
                completedMinutes = it.completedMinutes,
                timeSpentPaused = it.timeSpentPaused,
                endTime = it.endTime,
                sessionsBeforeLongBreak = it.inUseSessionsBeforeLongBreak(),
                longBreakData = it.longBreakData,
                breakBudgetMinutes = it.getBreakBudget(timeProvider.elapsedRealtime()).inWholeMinutes,
            ),
        )
    }

    fun skip() {
        timerManager.skip()
    }

    fun next(updateWorkTime: Boolean = false) {
        timerManager.next(updateWorkTime)
    }

    fun updateNotesForLastCompletedSession(notes: String) {
        timerManager.updateNotesForLastCompletedSession(notes = notes)
    }

    fun initTimerStyle(maxSize: Float, screenWidth: Float) {
        viewModelScope.launch {
            settingsRepo.updateTimerStyle {
                it.copy(
                    minSize = floor(maxSize / 1.5f),
                    maxSize = maxSize,
                    fontSize = floor(maxSize * 0.9f),
                    currentScreenWidth = screenWidth,
                )
            }
        }
    }
}
