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
import com.apps.adrcotfas.goodtime.bl.TimerManager.Companion.COUNT_UP_HARD_LIMIT
import com.apps.adrcotfas.goodtime.bl.TimerState
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.bl.getBaseTime
import com.apps.adrcotfas.goodtime.bl.isActive
import com.apps.adrcotfas.goodtime.bl.isBreak
import com.apps.adrcotfas.goodtime.bl.isPaused
import com.apps.adrcotfas.goodtime.common.InstallDateProvider
import com.apps.adrcotfas.goodtime.common.Time
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.ThemePreference
import com.apps.adrcotfas.goodtime.data.settings.TimerStyleData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    val isCountdown: Boolean = false,
    val baseTime: Long = 0,
    val timerState: TimerState = TimerState.RESET,
    val timerType: TimerType = TimerType.FOCUS,
    val completedMinutes: Long = 0,
    val timeSpentPaused: Long = 0,
    val endTime: Long = 0,
    val elapsedRealtime: Long = 0,
    val sessionsBeforeLongBreak: Int = 0,
    val longBreakData: LongBreakData = LongBreakData(),
    val breakBudgetMinutes: Long = 0,
) {
    val displayTime = max(baseTime, 0)

    val isPaused = timerState.isPaused
    val isActive = timerState.isActive
    val isBreak = timerType.isBreak
    val isFinished = timerState == TimerState.FINISHED

    /** Time elapsed since the session finished (only valid when isFinished) */
    val idleTime: Long
        get() = if (isFinished) elapsedRealtime - endTime else 0

    /** Whether we're within the 30-minute window after session finished */
    val isWithinInactivityTimeout: Boolean
        get() = isFinished && idleTime < TimerManager.AUTOSTART_TIMEOUT
}

data class TimerMainUiState(
    val isLoading: Boolean = true,
    val timerStyle: TimerStyleData = TimerStyleData(),
    val darkThemePreference: ThemePreference = ThemePreference.SYSTEM,
    val dynamicColor: Boolean = false,
    val screensaverMode: Boolean = false,
    val fullscreenMode: Boolean = false,
    val trueBlackMode: Boolean = true,
    val flashScreen: Boolean = false,
    val dndDuringWork: Boolean = false,
    val sessionCountToday: Int = 0,
    val startOfToday: Long = 0,
    val showTutorial: Boolean = false,
    val isPro: Boolean = false,
)

class TimerViewModel(
    private val timerManager: TimerManager,
    private val timeProvider: TimeProvider,
    private val settingsRepo: SettingsRepository,
    private val localDataRepo: LocalDataRepository,
    private val installDateProvider: InstallDateProvider,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val timerUiState =
        timerManager.timerData.flatMapLatest {
            when (it.state) {
                TimerState.RUNNING, TimerState.PAUSED, TimerState.FINISHED ->
                    flow {
                        while (true) {
                            emitUiState(it)
                            delay(1000)
                        }
                    }

                else -> {
                    flow { emitUiState(it) }
                }
            }
        } // .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimerUiState())

    private val _uiState = MutableStateFlow(TimerMainUiState())
    val uiState =
        _uiState
            .onStart {
                loadData()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimerMainUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            settingsRepo.settings
                .distinctUntilChanged { old, new ->
                    old.timerStyle == new.timerStyle &&
                        old.uiSettings == new.uiSettings &&
                        old.isPro == new.isPro &&
                        old.showTutorial == new.showTutorial &&
                        old.flashScreen == new.flashScreen
                }.collect {
                    val settings = it
                    val uiSettings = settings.uiSettings
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            timerStyle = settings.timerStyle,
                            darkThemePreference = uiSettings.themePreference,
                            dynamicColor = uiSettings.useDynamicColor,
                            screensaverMode = uiSettings.screensaverMode,
                            fullscreenMode = uiSettings.fullscreenMode,
                            trueBlackMode = uiSettings.trueBlackMode,
                            flashScreen = settings.flashScreen,
                            dndDuringWork = uiSettings.dndDuringWork,
                            isPro = settings.isPro,
                            showTutorial = settings.showTutorial,
                        )
                    }
                }
        }

        viewModelScope.launch {
            uiState
                .map { it.startOfToday }
                .filter { it != 0L }
                .flatMapLatest { startOfToday ->
                    localDataRepo.selectNumberOfSessionsAfter(startOfToday)
                }.distinctUntilChanged()
                .collect { sessionCountToday ->
                    _uiState.update {
                        it.copy(sessionCountToday = sessionCountToday)
                    }
                }
        }
    }

    fun startTimer(type: TimerType = TimerType.FOCUS) {
        timerManager.start(type)
    }

    fun toggleTimer() {
        timerManager.toggle()
    }

    fun resetTimer(actionType: FinishActionType = FinishActionType.MANUAL_RESET) {
        timerManager.reset(actionType)
    }

    fun addOneMinute() {
        timerManager.addOneMinute()
    }

    private suspend fun FlowCollector<TimerUiState>.emitUiState(it: DomainTimerData) {
        val elapsedRealtime = timeProvider.elapsedRealtime()
        emit(
            TimerUiState(
                isReady = it.isReady,
                label = it.label,
                isCountdown = it.isCurrentSessionCountdown(),
                baseTime = it.getBaseTime(timeProvider),
                timerState = it.state,
                timerType = it.type,
                completedMinutes = it.completedMinutes,
                timeSpentPaused = it.timeSpentPaused,
                endTime = it.endTime,
                elapsedRealtime = elapsedRealtime,
                sessionsBeforeLongBreak = it.inUseSessionsBeforeLongBreak(),
                longBreakData = it.longBreakData,
                breakBudgetMinutes = it.getBreakBudget(elapsedRealtime).inWholeMinutes,
            ),
        )
    }

    fun skip() {
        timerManager.skip()
    }

    fun next() {
        timerManager.next()
    }

    fun updateFinishedSession(
        updateDuration: Boolean,
        notes: String,
    ) {
        timerManager.updateFinishedSession(updateDuration, notes)
    }

    fun initTimerStyle(
        maxSize: Float,
        screenWidth: Float,
    ) {
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

    fun setActiveLabel(labelName: String) {
        viewModelScope.launch {
            settingsRepo.activateLabelWithName(labelName)
        }
    }

    fun refreshStartOfToday() {
        viewModelScope.launch {
            val startOfToday =
                Time.startOfTodayAdjusted(settingsRepo.settings.map { it.workdayStart }.first())
            _uiState.update {
                it.copy(startOfToday = startOfToday)
            }
        }
    }

    fun forceFinish() = timerManager.finish(actionType = FinishActionType.FORCE_FINISH)

    /**
     * Check if we're within the inactivity timeout using CURRENT time.
     * This is needed for initial render after foregrounding when cached timerUiState may be stale.
     */
    fun isWithinInactivityTimeout(): Boolean {
        val timerData = timerManager.timerData.value
        if (timerData.state != TimerState.FINISHED) return false
        val idleTime = timeProvider.elapsedRealtime() - timerData.endTime
        return idleTime < TimerManager.AUTOSTART_TIMEOUT
    }

    fun onSendToBackground() = timerManager.onSendToBackground()

    fun onBringToForeground() = timerManager.onBringToForeground()

    fun setShouldAskForReview() = viewModelScope.launch { settingsRepo.setShouldAskForReview(true) }

    fun setShowTutorial(show: Boolean) {
        viewModelScope.launch {
            settingsRepo.setShowTutorial(show)
        }
    }

    fun isInstallOlderThan10Days(): Boolean = installDateProvider.isInstallOlderThan10Days()
}
