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

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.apps.adrcotfas.goodtime.bl.isDefault
import com.apps.adrcotfas.goodtime.common.installIsOlderThan10Days
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.ui.common.ConfirmationDialog
import com.apps.adrcotfas.goodtime.ui.common.DatePickerDialog
import com.apps.adrcotfas.goodtime.ui.common.DragHandle
import com.apps.adrcotfas.goodtime.ui.common.SelectLabelDialog
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.TimePicker
import com.apps.adrcotfas.goodtime.ui.common.toLocalTime
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Lock
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.labels_edit_label
import goodtime_productivity.composeapp.generated.resources.labels_select_label
import goodtime_productivity.composeapp.generated.resources.main_save
import goodtime_productivity.composeapp.generated.resources.stats_change_label_of_selected_sessions
import goodtime_productivity.composeapp.generated.resources.stats_delete_selected_sessions
import goodtime_productivity.composeapp.generated.resources.stats_overview
import goodtime_productivity.composeapp.generated.resources.stats_timeline
import goodtime_productivity.composeapp.generated.resources.unlock_premium
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

private enum class TabType {
    Overview,
    Timeline,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = koinViewModel(),
    historyViewModel: StatisticsHistoryViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val isLoadingHistoryChartData = historyUiState.isLoading
    val sessionsPagingItems = viewModel.pagedSessions.collectAsLazyPagingItems()
    val historyListState = rememberLazyListState()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showSelectVisibleLabelsDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectLabelDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var showEditBulkLabelDialog by rememberSaveable { mutableStateOf(false) }
    var showEditLabelConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    val isLoading = uiState.isLoading || isLoadingHistoryChartData

    BackHandler(enabled = uiState.showSelectionUi) {
        if (uiState.showSelectionUi) {
            viewModel.clearShowSelectionUi()
        }
    }

    Scaffold(
        topBar = {
            StatisticsScreenTopBar(
                onNavigateBack = onNavigateBack,
                onAddButtonClick = { viewModel.onAddEditSession() },
                onLabelButtonClick = {
                    if (uiState.showSelectionUi) {
                        showEditBulkLabelDialog = true
                    } else {
                        showSelectVisibleLabelsDialog = true
                    }
                },
                onCancel = { viewModel.clearShowSelectionUi() },
                onDeleteClick = { showDeleteConfirmationDialog = true },
                onSelectAll = { viewModel.selectAllSessions(sessionsPagingItems.itemCount) },
                showSelectionUi = uiState.showSelectionUi,
                selectionCount = uiState.selectionCount,
                showSeparator = uiState.showSelectionUi && historyListState.canScrollBackward,
                showBreaks = uiState.statisticsSettings.showBreaks,
                onSetShowBreaks = viewModel::setShowBreaks,
                showArchived = uiState.statisticsSettings.showArchived,
                onSetShowArchived = viewModel::setShowArchived,
            )
        },
    ) { paddingValues ->
        var type by rememberSaveable { mutableStateOf(TabType.Overview) }
        val titles =
            listOf(
                stringResource(Res.string.stats_overview),
                stringResource(Res.string.stats_timeline),
            )

        Crossfade(isLoading) { isLoading ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                Column(
                    modifier = Modifier.padding(paddingValues),
                ) {
                    AnimatedVisibility(!uiState.showSelectionUi) {
                        SecondaryTabRow(
                            selectedTabIndex = type.ordinal,
                            modifier = Modifier.wrapContentSize(),
                            divider = { SubtleHorizontalDivider() },
                        ) {
                            titles.forEachIndexed { index, title ->
                                Tab(
                                    selected = type == TabType.entries[index],
                                    onClick = { type = TabType.entries[index] },
                                    text = { Text(title) },
                                )
                            }
                        }
                    }

                    when (type) {
                        TabType.Overview ->
                            OverviewTab(
                                firstDayOfWeek = uiState.firstDayOfWeek,
                                workDayStart = uiState.workDayStart,
                                statisticsSettings = uiState.statisticsSettings,
                                statisticsData = uiState.statisticsData,
                                onChangeOverviewType = {
                                    viewModel.setOverviewType(it)
                                },
                                onChangeOverviewDurationType = {
                                    viewModel.setOverviewDurationType(it)
                                },
                                onChangePieChartOverviewType = {
                                    viewModel.setPieChartViewType(it)
                                },
                                historyChartViewModel = historyViewModel,
                            )

                        TabType.Timeline -> {
                            TimelineTab(
                                listState = historyListState,
                                sessions = sessionsPagingItems,
                                isSelectAllEnabled = uiState.isSelectAllEnabled,
                                selectedSessions = uiState.selectedSessions,
                                unselectedSessions = uiState.unselectedSessions,
                                labels = uiState.labels,
                                onClick = { session ->
                                    if (uiState.showSelectionUi) {
                                        viewModel.toggleSessionIsSelected(session.id)
                                    } else {
                                        viewModel.onAddEditSession(session)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSessionIsSelected(it.id)
                                },
                            )
                        }
                    }
                }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val hideSheet = { viewModel.clearAddEditSession() }

                if (uiState.showAddSession) {
                    ModalBottomSheet(
                        onDismissRequest = { hideSheet() },
                        sheetState = sheetState,
                        dragHandle = {
                            DragHandle(
                                buttonText = stringResource(Res.string.main_save),
                                isEnabled = uiState.canSave && uiState.isPro,
                                buttonIcon =
                                    if (uiState.isPro) {
                                        null
                                    } else {
                                        {
                                            Icon(
                                                imageVector = EvaIcons.Outline.Lock,
                                                contentDescription = stringResource(Res.string.unlock_premium),
                                            )
                                        }
                                    },
                                onClose = { hideSheet() },
                                onClick = {
                                    viewModel.saveSession()
                                    // ask for in app review if the user just saved a session
                                    if (context.installIsOlderThan10Days()) {
                                        viewModel.setShouldAskForReview()
                                    }
                                    hideSheet()
                                },
                            )
                        },
                    ) {
                        AddEditSessionContent(
                            session = uiState.newSession,
                            labelData = uiState.labels.first { it.name == uiState.newSession.label },
                            onUpdate = {
                                viewModel.updateSessionToEdit(it)
                            },
                            onValidate = {
                                viewModel.setCanSave(it)
                            },
                            onOpenLabelSelector = { showSelectLabelDialog = true },
                            onOpenDatePicker = { showDatePicker = true },
                            onOpenTimePicker = { showTimePicker = true },
                        )
                    }
                }
                val timeZone = TimeZone.currentSystemDefault()
                if (showDatePicker) {
                    val dateTime =
                        Instant
                            .fromEpochMilliseconds(uiState.newSession.timestamp)
                            .toLocalDateTime(timeZone)
                    val now =
                        Instant
                            .fromEpochMilliseconds(
                                Clock.System.now().toEpochMilliseconds(),
                            ).toLocalDateTime(timeZone)
                    val tomorrowMillis =
                        LocalDateTime(
                            now.date.plus(DatePeriod(days = 1)),
                            LocalTime(hour = 0, minute = 0),
                        ).toInstant(
                            timeZone,
                        ).toEpochMilliseconds()

                    val datePickerState =
                        rememberDatePickerState(
                            initialSelectedDateMillis =
                                dateTime
                                    .toInstant(timeZone)
                                    .toEpochMilliseconds(),
                            selectableDates =
                                object : SelectableDates {
                                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis < tomorrowMillis
                                },
                        )
                    DatePickerDialog(
                        onDismiss = { showDatePicker = false },
                        onConfirm = {
                            val selectedUtcDate =
                                Instant
                                    .fromEpochMilliseconds(it)
                                    .toLocalDateTime(TimeZone.UTC)
                                    .date
                            val newDateTime = LocalDateTime(selectedUtcDate, dateTime.time)
                            val newTimestamp =
                                newDateTime
                                    .toInstant(timeZone)
                                    .toEpochMilliseconds()
                            viewModel.updateSessionToEdit(
                                uiState.newSession.copy(
                                    timestamp = newTimestamp,
                                ),
                            )
                            showDatePicker = false
                        },
                        datePickerState = datePickerState,
                    )
                }
                if (showTimePicker) {
                    val dateTime =
                        Instant
                            .fromEpochMilliseconds(uiState.newSession.timestamp)
                            .toLocalDateTime(timeZone)
                    val time = dateTime.time
                    val timePickerState =
                        rememberTimePickerState(
                            initialHour = time.hour,
                            initialMinute = time.minute,
                            is24Hour = DateFormat.is24HourFormat(context),
                        )
                    TimePicker(
                        onDismiss = { showTimePicker = false },
                        onConfirm = {
                            val newTime = it.toLocalTime()
                            val newDateTime = LocalDateTime(dateTime.date, newTime)
                            val newTimestamp =
                                newDateTime
                                    .toInstant(timeZone)
                                    .toEpochMilliseconds()

                            viewModel.updateSessionToEdit(
                                uiState.newSession.copy(
                                    timestamp = newTimestamp,
                                ),
                            )
                            showTimePicker = false
                        },
                        timePickerState = timePickerState,
                    )
                }
                if (showSelectLabelDialog) {
                    SelectLabelDialog(
                        title = stringResource(Res.string.labels_select_label),
                        labels = uiState.labels.filter { !it.isDefault() },
                        initialSelectedLabels = persistentListOf(uiState.newSession.label),
                        onDismiss = { showSelectLabelDialog = false },
                        singleSelection = true,
                        onConfirm = {
                            viewModel.updateSessionToEdit(
                                uiState.newSession.copy(
                                    label = if (it.isNotEmpty()) it.first() else Label.DEFAULT_LABEL_NAME,
                                ),
                            )
                            showSelectLabelDialog = false
                        },
                    )
                }
                if (showSelectVisibleLabelsDialog) {
                    SelectStatsVisibleLabelsDialog(
                        labels = uiState.labels,
                        initialSelectedLabels = uiState.selectedLabels,
                        onDismiss = { showSelectVisibleLabelsDialog = false },
                        onConfirm = {
                            viewModel.setSelectedLabels(it)

                            val labelData =
                                uiState.labels.filter { label ->
                                    it.contains(label.name)
                                }

                            historyViewModel.setSelectedLabels(labelData)
                            showSelectVisibleLabelsDialog = false
                        },
                        isLineChart = historyUiState.isLineChart,
                        onSetLineChart = {
                            historyViewModel.setIsLineChart(it)
                        },
                    )
                }
                if (showDeleteConfirmationDialog) {
                    ConfirmationDialog(
                        title = stringResource(Res.string.stats_delete_selected_sessions),
                        onDismiss = { showDeleteConfirmationDialog = false },
                        onConfirm = {
                            viewModel.deleteSelectedSessions()
                            viewModel.clearShowSelectionUi()
                            showDeleteConfirmationDialog = false
                        },
                    )
                }
                if (showEditBulkLabelDialog) {
                    SelectLabelDialog(
                        title = stringResource(Res.string.labels_edit_label),
                        labels = uiState.labels.filter { !it.isDefault() },
                        onDismiss = { showEditBulkLabelDialog = false },
                        singleSelection = true,
                        onConfirm = {
                            viewModel.setSelectedLabelToBulkEdit(it.first())
                            showEditBulkLabelDialog = false
                            showEditLabelConfirmationDialog = true
                        },
                        showIcons = false,
                        forceShowClearLabel = true,
                    )
                }
                if (showEditLabelConfirmationDialog) {
                    ConfirmationDialog(
                        title = stringResource(Res.string.stats_change_label_of_selected_sessions),
                        onDismiss = { showEditLabelConfirmationDialog = false },
                        onConfirm = {
                            viewModel.bulkEditLabel()
                            viewModel.clearShowSelectionUi()
                            showEditLabelConfirmationDialog = false
                        },
                    )
                }
            }
        }
    }
}
