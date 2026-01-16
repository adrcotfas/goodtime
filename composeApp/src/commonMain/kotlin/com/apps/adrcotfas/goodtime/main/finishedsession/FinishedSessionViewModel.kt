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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.main_break_complete
import goodtime_productivity.composeapp.generated.resources.main_consider_idle_time_as_extra_focus
import goodtime_productivity.composeapp.generated.resources.main_focus_complete
import goodtime_productivity.composeapp.generated.resources.main_idle
import goodtime_productivity.composeapp.generated.resources.main_interruptions
import goodtime_productivity.composeapp.generated.resources.main_start_break
import goodtime_productivity.composeapp.generated.resources.main_start_focus
import goodtime_productivity.composeapp.generated.resources.main_this_session
import goodtime_productivity.composeapp.generated.resources.stats_add_notes
import goodtime_productivity.composeapp.generated.resources.stats_break
import goodtime_productivity.composeapp.generated.resources.stats_focus
import goodtime_productivity.composeapp.generated.resources.stats_today
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import kotlin.time.Instant

data class FinishedSessionUiState(
    val isPro: Boolean = false,
    val isFullscreen: Boolean = false,
    val todayWorkMinutes: Long = 0,
    val todayBreakMinutes: Long = 0,
    val todayInterruptedMinutes: Long = 0,
    val strings: FinishedSessionStrings = FinishedSessionStrings(),
    val isLoading: Boolean = true,
)

data class FinishedSessionStrings(
    val mainStartFocus: String = "",
    val mainStartBreak: String = "",
    val mainBreakComplete: String = "",
    val mainFocusComplete: String = "",
    val mainThisSession: String = "",
    val statsBreak: String = "",
    val statsFocus: String = "",
    val mainInterruptions: String = "",
    val mainIdle: String = "",
    val mainConsiderIdleTimeAsExtraFocus: String = "",
    val statsAddNotes: String = "",
    val statsToday: String = "",
)

class FinishedSessionViewModel(
    private val settingsRepo: SettingsRepository,
    private val localDataRepo: LocalDataRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val finishedSessionUiState = MutableStateFlow(FinishedSessionUiState())
    val uiState =
        finishedSessionUiState
            .onStart {
                loadHistoryState()
                loadStrings()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinishedSessionUiState())

    private var stringsLoaded = false
    private var historyLoaded = false

    private fun checkLoading() {
        if (stringsLoaded && historyLoaded) {
            finishedSessionUiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadStrings() {
        viewModelScope.launch {
            val strings =
                FinishedSessionStrings(
                    mainStartFocus = getString(Res.string.main_start_focus),
                    mainStartBreak = getString(Res.string.main_start_break),
                    mainBreakComplete = getString(Res.string.main_break_complete),
                    mainFocusComplete = getString(Res.string.main_focus_complete),
                    mainThisSession = getString(Res.string.main_this_session),
                    statsBreak = getString(Res.string.stats_break),
                    statsFocus = getString(Res.string.stats_focus),
                    mainInterruptions = getString(Res.string.main_interruptions),
                    mainIdle = getString(Res.string.main_idle),
                    mainConsiderIdleTimeAsExtraFocus = getString(Res.string.main_consider_idle_time_as_extra_focus),
                    statsAddNotes = getString(Res.string.stats_add_notes),
                    statsToday = getString(Res.string.stats_today),
                )
            stringsLoaded = true
            finishedSessionUiState.update { it.copy(strings = strings) }
            checkLoading()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadHistoryState() {
        viewModelScope.launch {
            settingsRepo.settings
                .distinctUntilChanged { old, new ->
                    old.workdayStart == new.workdayStart &&
                        old.isPro == new.isPro &&
                        old.uiSettings.fullscreenMode == new.uiSettings.fullscreenMode
                }.flatMapLatest { settings ->
                    finishedSessionUiState.update {
                        it.copy(
                            isPro = settings.isPro,
                            isFullscreen = settings.uiSettings.fullscreenMode,
                        )
                    }
                    localDataRepo.selectSessionsAfter(toMillisOfToday(settings.workdayStart))
                }.collect { sessions ->
                    val (todayWorkSessions, todayBreakSessions) =
                        sessions.partition { session -> session.isWork }

                    val todayWorkMinutes = todayWorkSessions.sumOf { it.duration }
                    val todayBreakMinutes = todayBreakSessions.sumOf { it.duration }
                    val todayInterruptedMinutes = todayWorkSessions.sumOf { it.interruptions }

                    historyLoaded = true
                    finishedSessionUiState.update {
                        it.copy(
                            todayWorkMinutes = todayWorkMinutes,
                            todayBreakMinutes = todayBreakMinutes,
                            todayInterruptedMinutes = todayInterruptedMinutes,
                        )
                    }
                    checkLoading()
                }
        }
    }

    private fun toMillisOfToday(workdayStart: Int): Long {
        val hour = workdayStart / 3600
        val minute = (workdayStart % 3600) / 60
        val second = workdayStart % 60

        val instant = Instant.fromEpochMilliseconds(timeProvider.now())
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val timeAtSecondOfDay = LocalDateTime(dateTime.date, LocalTime(hour, minute, second))
        val instant2 = timeAtSecondOfDay.toInstant(TimeZone.currentSystemDefault())
        return instant2.toEpochMilliseconds()
    }
}
