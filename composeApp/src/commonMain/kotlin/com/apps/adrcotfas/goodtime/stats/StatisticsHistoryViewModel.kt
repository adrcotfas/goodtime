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
package com.apps.adrcotfas.goodtime.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.bl.LabelData
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.HistoryIntervalType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek

data class StatisticsHistoryUiState(
    val isLoading: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val workdayStart: Int = 0,
    val data: HistoryChartData = HistoryChartData(),
    val type: HistoryIntervalType = HistoryIntervalType.DAYS,
    val overviewType: OverviewType = OverviewType.TIME,
    val selectedLabels: List<LabelData> = emptyList(),
    val isLineChart: Boolean = true,
)

class StatisticsHistoryViewModel(
    private val localDataRepo: LocalDataRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsHistoryUiState())
    val uiState =
        _uiState
            .onStart { loadData() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsHistoryUiState())

    private fun loadData() {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.statisticsSettings.showArchived }
                .distinctUntilChanged()
                .flatMapLatest { showArchived ->
                    if (showArchived) {
                        localDataRepo.selectAllLabels()
                    } else {
                        localDataRepo.selectLabelsByArchived(isArchived = false)
                    }
                }.collect { labels ->
                    _uiState.update {
                        it.copy(
                            selectedLabels =
                                labels.map { label ->
                                    LabelData(
                                        name = label.name,
                                        colorIndex = label.colorIndex,
                                    )
                                },
                        )
                    }
                }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .distinctUntilChanged { old, new ->
                    old.historyChartSettings.intervalType == new.historyChartSettings.intervalType &&
                        old.firstDayOfWeek == new.firstDayOfWeek &&
                        old.workdayStart == new.workdayStart &&
                        old.statisticsSettings.overviewType == new.statisticsSettings.overviewType &&
                        old.historyChartSettings.isLineChart == new.historyChartSettings.isLineChart
                }.collect { settings ->
                    val type = settings.historyChartSettings.intervalType
                    val overviewType = settings.statisticsSettings.overviewType
                    val firstDayOfWeek = DayOfWeek(settings.firstDayOfWeek)
                    _uiState.update { state ->
                        state.copy(
                            firstDayOfWeek = firstDayOfWeek,
                            workdayStart = settings.workdayStart,
                            type = type,
                            overviewType = overviewType,
                            isLineChart = settings.historyChartSettings.isLineChart,
                        )
                    }
                }
        }

        viewModelScope.launch {
            uiState
                .distinctUntilChanged { old, new ->
                    old.selectedLabels == new.selectedLabels &&
                        old.type == new.type &&
                        old.firstDayOfWeek == new.firstDayOfWeek &&
                        old.workdayStart == new.workdayStart &&
                        old.overviewType == new.overviewType &&
                        old.isLineChart == new.isLineChart
                }.flatMapLatest {
                    localDataRepo
                        .selectSessionsByLabels(
                            it.selectedLabels.map { label -> label.name },
                        ).map { sessions ->
                            withContext(Dispatchers.Default) {
                                computeHistoryChartData(
                                    sessions = sessions,
                                    labels = it.selectedLabels.map { label -> label.name },
                                    type = it.type,
                                    overviewType = it.overviewType,
                                    firstDayOfWeek = it.firstDayOfWeek,
                                    workDayStart = it.workdayStart,
                                    aggregate = it.isLineChart,
                                )
                            }
                        }
                }.collect { data ->
                    _uiState.update { it.copy(data = data, isLoading = false) }
                }
        }
    }

    fun setType(type: HistoryIntervalType) {
        viewModelScope.launch {
            settingsRepository.updateHistoryChartSettings { it.copy(intervalType = type) }
        }
    }

    fun setIsLineChart(isLineChart: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHistoryChartSettings { it.copy(isLineChart = isLineChart) }
        }
    }

    fun setSelectedLabels(selectedLabels: List<LabelData>) {
        _uiState.update { it.copy(selectedLabels = selectedLabels) }
    }
}
