/**
 *     Goodtime Productivity
 *     Copyright (C) Adrian Cotfas
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
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.apps.adrcotfas.goodtime.bl.LabelData
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.common.TimeFormatProvider
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.SessionSelection
import com.apps.adrcotfas.goodtime.data.model.getLabelData
import com.apps.adrcotfas.goodtime.data.model.toExternal
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.StatisticsSettings
import com.apps.adrcotfas.goodtime.data.settings.select
import com.apps.adrcotfas.goodtime.data.settings.updateStatisticsSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val isPro: Boolean = false,
    val labels: List<LabelData> = emptyList(),
    val unarchivedLabelNames: Set<String> = emptySet(),
    val selectedLabels: List<String> = emptyList(),
    // Selection UI related fields
    val selection: SessionSelection = SessionSelection.Only(),
    val selectedLabelToBulkEdit: String? = null,
    // Add/Edit session related fields
    val sessionToEdit: Session? = null, // this does not change after initialization
    val newSession: Session = Session.default(),
    val showAddSession: Boolean = false,
    val canSave: Boolean = true,
    // Overview Tab related fields
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val workDayStart: Int = 0,
    val statisticsSettings: StatisticsSettings = StatisticsSettings(),
    val statisticsData: StatisticsData = StatisticsData(),
    val is24HourFormat: Boolean = true,
) {
    val showSelectionUi: Boolean
        get() = !selection.isEmpty
}

class StatisticsViewModel(
    private val localDataRepo: LocalDataRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val timeFormatProvider: TimeFormatProvider,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    val pagedSessions: Flow<PagingData<Session>> =
        uiState
            .distinctUntilChanged { old, new ->
                old.selectedLabels == new.selectedLabels &&
                    old.statisticsSettings.showBreaks == new.statisticsSettings.showBreaks
            }.flatMapLatest {
                selectSessionsForTimelinePaged(it.selectedLabels, it.statisticsSettings.showBreaks)
            }

    private fun selectSessionsForTimelinePaged(
        labels: List<String>,
        showBreaks: Boolean,
    ): Flow<PagingData<Session>> = Pager(PagingConfig(pageSize = 50, prefetchDistance = 50)) {
        localDataRepo.selectSessionsForTimelinePaged(labels, showBreaks = showBreaks)
    }.flow.map { value ->
        value.map {
            it.toExternal()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.update {
                it.copy(
                    isPro = settings.isPro,
                    workDayStart = settings.workdayStart,
                    firstDayOfWeek = DayOfWeek(settings.firstDayOfWeek),
                    is24HourFormat = timeFormatProvider.is24HourFormat(),
                )
            }

            settingsRepository
                .select { it.statisticsSettings }
                .flatMapLatest { statisticsSettings ->
                    _uiState.update { it.copy(statisticsSettings = statisticsSettings) }
                    if (statisticsSettings.showArchived) {
                        localDataRepo.selectAllLabels()
                    } else {
                        localDataRepo.selectLabelsByArchived(isArchived = false)
                    }
                }.collect { labels ->
                    _uiState.update {
                        it.copy(
                            labels = labels.map { label -> label.getLabelData() },
                            unarchivedLabelNames = labels.filterNot { label -> label.isArchived }.map { label -> label.name }.toSet(),
                            selectedLabels = labels.map { label -> label.name },
                        )
                    }
                }
        }

        viewModelScope.launch {
            uiState
                .filter { it.labels.isNotEmpty() }
                .map { it.selectedLabels }
                .distinctUntilChanged()
                .flatMapLatest { selectedLabels ->
                    localDataRepo
                        .selectSessionsByLabels(selectedLabels)
                        .map { sessions ->
                            withContext(defaultDispatcher) {
                                computeStatisticsData(
                                    sessions = sessions,
                                    firstDayOfWeek = uiState.value.firstDayOfWeek,
                                    secondOfDay = uiState.value.workDayStart,
                                )
                            }
                        }
                }.collect { data ->
                    _uiState.update { it.copy(statisticsData = data, isLoading = false) }
                }
        }
    }

    fun setSelectedLabels(selectedLabels: List<String>) {
        _uiState.update { it.copy(selectedLabels = selectedLabels) }
    }

    fun toggleSessionIsSelected(id: Long) {
        _uiState.update { it.copy(selection = it.selection.toggle(id)) }
    }

    fun clearShowSelectionUi() {
        _uiState.update { it.copy(selection = SessionSelection.Only()) }
    }

    fun selectAllSessions(allSessionsCount: Int) {
        _uiState.update {
            it.copy(
                selection =
                SessionSelection.AllExcept(
                    total = allSessionsCount,
                    labels = it.selectedLabels,
                    showBreaks = it.statisticsSettings.showBreaks,
                ),
            )
        }
    }

    fun deleteSelectedSessions() {
        val selection = uiState.value.selection
        viewModelScope.launch {
            localDataRepo.deleteSessions(selection)
        }
    }

    fun updateSessionToEdit(session: Session) {
        _uiState.update { state ->
            state.copy(newSession = session)
        }
    }

    fun setCanSave(isValid: Boolean) {
        _uiState.update { it.copy(canSave = isValid) }
    }

    fun saveSession() {
        viewModelScope.launch {
            val newSession = uiState.value.newSession
            val sessionToEditId = uiState.value.sessionToEdit?.id
            sessionToEditId?.let {
                localDataRepo.updateSession(newSession.id, newSession)
            } ?: localDataRepo.insertSession(newSession)
        }
    }

    fun onAddEditSession(sessionToEdit: Session? = null) {
        val session = sessionToEdit ?: generateNewSession()
        _uiState.update {
            it.copy(
                sessionToEdit = sessionToEdit,
                newSession = session,
                showAddSession = true,
                canSave = sessionToEdit != null,
            )
        }
    }

    fun clearAddEditSession() {
        _uiState.update { it.copy(showAddSession = false) }
    }

    private fun generateNewSession(): Session = Session.create(
        duration = 0,
        timestamp = timeProvider.now(),
        interruptions = 0,
        label = Label.DEFAULT_LABEL_NAME,
        isWork = true,
    )

    fun setSelectedLabelToBulkEdit(label: String) {
        _uiState.update { it.copy(selectedLabelToBulkEdit = label) }
    }

    fun bulkEditLabel() {
        val label = uiState.value.selectedLabelToBulkEdit ?: return
        val selection = uiState.value.selection
        viewModelScope.launch {
            localDataRepo.updateSessionsLabel(label, selection)
        }
    }

    fun setOverviewType(type: OverviewType) {
        viewModelScope.launch {
            settingsRepository.updateStatisticsSettings { it.copy(overviewType = type) }
        }
    }

    fun setOverviewDurationType(type: OverviewDurationType) {
        viewModelScope.launch {
            settingsRepository.updateStatisticsSettings { it.copy(overviewDurationType = type) }
        }
    }

    fun setPieChartViewType(type: OverviewDurationType) {
        viewModelScope.launch {
            settingsRepository.updateStatisticsSettings { it.copy(pieChartViewType = type) }
        }
    }

    fun setShowBreaks(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateStatisticsSettings { it.copy(showBreaks = enabled) }
        }
    }

    fun setShowArchived(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateStatisticsSettings { it.copy(showArchived = enabled) }
        }
    }
}
