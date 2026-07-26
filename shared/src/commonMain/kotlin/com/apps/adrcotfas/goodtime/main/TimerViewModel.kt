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
import com.apps.adrcotfas.goodtime.common.InstallDateProvider
import com.apps.adrcotfas.goodtime.common.Time
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.ThemePreference
import com.apps.adrcotfas.goodtime.data.settings.TimerStyleData
import com.apps.adrcotfas.goodtime.data.settings.UiSettings
import com.apps.adrcotfas.goodtime.data.settings.select
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

data class TimerUiState(
    val isReady: Boolean = false,
    val label: DomainLabel = DomainLabel(),
    val isCountdown: Boolean = false,
    val timerState: TimerState = TimerState.RESET,
    val timerType: TimerType = TimerType.FOCUS,
    val completedMinutes: Long = 0,
    val timeSpentPaused: Long = 0,
    val endTime: Long = 0,
    val sessionsBeforeLongBreak: Int = 0,
    val longBreakData: LongBreakData = LongBreakData(),
    val breakBudgetMinutes: Long = 0,
) {
    val isPaused = timerState.isPaused
    val isActive = timerState.isActive
    val isBreak = timerType.isBreak
    val isFinished = timerState == TimerState.FINISHED
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
    val pipMode: Boolean = true,
)

private data class TimerScreenSettings(
    val timerStyle: TimerStyleData,
    val uiSettings: UiSettings,
    val isPro: Boolean,
    val showTutorial: Boolean,
    val flashScreen: Boolean,
)

class TimerViewModel(
    private val timerManager: TimerManager,
    private val timeProvider: TimeProvider,
    private val settingsRepo: SettingsRepository,
    private val localDataRepo: LocalDataRepository,
    private val installDateProvider: InstallDateProvider,
) : ViewModel() {
    /**
     * Emits [selector] once per second while [tickWhile] holds for the current timer state, and once
     * otherwise. [distinctUntilChanged] collapses ticks that don't change the value, so a selector
     * that omits the ticking time (e.g. [TimerUiState]) only re-emits on real state changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> tickingFlow(
        tickWhile: (TimerState) -> Boolean,
        selector: (DomainTimerData) -> T,
    ) = timerManager.timerData
        .flatMapLatest { data ->
            if (tickWhile(data.runtime.state)) {
                flow {
                    while (true) {
                        emit(selector(data))
                        // Delay to the next second boundary so the display doesn't skip a second.
                        delay((1000 - timeProvider.elapsedRealtime() % 1000).milliseconds)
                    }
                }
            } else {
                flow { emit(selector(data)) }
            }
        }.distinctUntilChanged()

    // Stable timer state, without the per-second ticking time: tick only while RUNNING to refresh
    // the whole-minute break budget; distinctUntilChanged then collapses the identical per-second
    // emissions so the main screen doesn't recompose every second.
    val timerUiState =
        tickingFlow(tickWhile = { it == TimerState.RUNNING }, selector = ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimerUiState())

    // The ticking countdown/count-up value, as its own narrow flow so only the timer text recomposes.
    val displayTime =
        tickingFlow(tickWhile = { it == TimerState.RUNNING }) {
            max(it.getBaseTime(timeProvider), 0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Time elapsed since the session finished, ticking only while FINISHED (drives the finished
    // sheet's idle counter and its 30-minute auto-dismiss); the ticker stops once reset.
    val idleTime =
        tickingFlow(tickWhile = { it == TimerState.FINISHED }) {
            if (it.runtime.state == TimerState.FINISHED) {
                timeProvider.elapsedRealtime() - it.runtime.endTime
            } else {
                0L
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

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
            settingsRepo
                .select {
                    TimerScreenSettings(
                        timerStyle = it.timerStyle,
                        uiSettings = it.uiSettings,
                        isPro = it.isPro,
                        showTutorial = it.showTutorial,
                        flashScreen = it.flashScreen,
                    )
                }.collect { settings ->
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
                            pipMode = uiSettings.pipMode,
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

    fun resetBreakBudget() {
        timerManager.resetBreakBudget()
    }

    private fun toUiState(it: DomainTimerData): TimerUiState = TimerUiState(
        isReady = it.isReady,
        label = it.label,
        isCountdown = it.isCurrentSessionCountdown(),
        timerState = it.runtime.state,
        timerType = it.runtime.type,
        completedMinutes = it.completedMinutes,
        timeSpentPaused = it.runtime.timeSpentPaused,
        endTime = it.runtime.endTime,
        sessionsBeforeLongBreak = it.inUseSessionsBeforeLongBreak(),
        longBreakData = it.longBreakData,
        breakBudgetMinutes = it.getBreakBudget(timeProvider.elapsedRealtime()).inWholeMinutes,
    )

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

    /**
     * Check if we're within the inactivity timeout using CURRENT time.
     * This is needed for initial render after foregrounding when cached timerUiState may be stale.
     */
    fun isWithinInactivityTimeout(): Boolean {
        val timerData = timerManager.timerData.value
        if (timerData.runtime.state != TimerState.FINISHED) return false
        return currentIdleTime() < TimerManager.AUTOSTART_TIMEOUT
    }

    /** Time elapsed since the session finished, computed live (0 if not finished). */
    fun currentIdleTime(): Long {
        val timerData = timerManager.timerData.value
        if (timerData.runtime.state != TimerState.FINISHED) return 0
        return timeProvider.elapsedRealtime() - timerData.runtime.endTime
    }

    /**
     * Request the in-app review flow if the user is engaged enough to be worth asking:
     * old install, enough completed sessions, and not asked recently. The Play API has its
     * own opaque quota; the local throttle keeps each call at a moment we chose.
     */
    fun askForReviewIfEligible() {
        viewModelScope.launch {
            val lastAsked = settingsRepo.settings.first().lastAskedForReviewTime
            if (installDateProvider.isInstallOlderThan10Days() &&
                timeProvider.now() - lastAsked >= MIN_TIME_BETWEEN_REVIEW_ASKS.inWholeMilliseconds &&
                localDataRepo.selectNumberOfSessionsAfter(0).first() >= MIN_SESSIONS_FOR_REVIEW
            ) {
                settingsRepo.setLastAskedForReviewTime(timeProvider.now())
                settingsRepo.setShouldAskForReview(true)
            }
        }
    }

    fun setShowTutorial(show: Boolean) {
        viewModelScope.launch {
            settingsRepo.setShowTutorial(show)
        }
    }
}

private const val MIN_SESSIONS_FOR_REVIEW = 10
private val MIN_TIME_BETWEEN_REVIEW_ASKS = 30.days
