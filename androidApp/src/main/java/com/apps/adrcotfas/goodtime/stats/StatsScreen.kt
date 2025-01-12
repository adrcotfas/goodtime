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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.apps.adrcotfas.goodtime.common.ConfirmationDialog
import com.apps.adrcotfas.goodtime.common.IconButtonWithBadge
import com.apps.adrcotfas.goodtime.common.SelectLabelDialog
import com.apps.adrcotfas.goodtime.ui.common.DatePickerDialog
import com.apps.adrcotfas.goodtime.ui.common.DragHandle
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.TimePicker
import com.apps.adrcotfas.goodtime.ui.common.toLocalTime
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Trash
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

private enum class TabType {
    Overview, History
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = koinViewModel()) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionsPagingItems = viewModel.sessions.collectAsLazyPagingItems()
    val selectedLabelsCount = uiState.selectedLabels.size
    val historyListState = rememberLazyListState()

    BackHandler(enabled = uiState.showSelectionUi) {
        if (uiState.showSelectionUi) {
            viewModel.clearShowSelectionUi()
        }
    }

    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            StatsScreenTopBar(
                onAddButtonClick = { viewModel.onAddEditSession() },
                onLabelButtonClick = {
                    if (uiState.showSelectionUi) {
                        viewModel.setShowEditLabelDialog(true)
                    } else {
                        viewModel.setShowSelectVisibleLabelsDialog(true)
                    }
                },
                selectedLabelsCount = selectedLabelsCount,
                onCancel = { viewModel.clearShowSelectionUi() },
                onDeleteClick = { viewModel.setShowDeleteConfirmationDialog(true) },
                onSelectAll = { viewModel.selectAllSessions(sessionsPagingItems.itemCount) },
                showSelectionUi = uiState.showSelectionUi,
                selectionCount = uiState.selectionCount,
                showSeparator = uiState.showSelectionUi && historyListState.canScrollBackward,
            )
        },
    ) { paddingValues ->
        var type by rememberSaveable { mutableStateOf(TabType.Overview) }
        val titles = listOf("Overview", "History")
        var showDatePicker by rememberSaveable { mutableStateOf(false) }
        var showTimePicker by rememberSaveable { mutableStateOf(false) }

        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            AnimatedVisibility(!uiState.showSelectionUi) {
                SecondaryTabRow(
                    selectedTabIndex = type.ordinal,
                    modifier = Modifier.wrapContentSize(),
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
                TabType.Overview -> OverviewTab()
                TabType.History -> {
                    HistoryTab(
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

        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()

        val hideSheet = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    viewModel.clearAddEditSession()
                }
            }
        }

        if (uiState.showAddSession) {
            ModalBottomSheet(
                onDismissRequest = { hideSheet() },
                sheetState = sheetState,
                dragHandle = {
                    DragHandle(
                        buttonText = "Save",
                        isEnabled = uiState.canSave,
                        onClose = { hideSheet() },
                        onClick = {
                            viewModel.saveSession()
                            hideSheet()
                        },
                    )
                },
            ) {
                AddEditSessionContent(
                    session = uiState.newSession,
                    labels = uiState.labels,
                    onUpdate = {
                        viewModel.updateSessionToEdit(it)
                    },
                    onValidate = {
                        viewModel.setCanSave(it)
                    },
                    onOpenLabelSelector = { viewModel.setShowSelectLabelDialog(true) },
                    onOpenDatePicker = { showDatePicker = true },
                    onOpenTimePicker = { showTimePicker = true },
                )
            }
        }
        if (showDatePicker) {
            val dateTime = Instant.fromEpochMilliseconds(uiState.newSession.timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val tomorrowMillis =
                LocalDateTime(
                    now.date.plus(DatePeriod(days = 1)),
                    LocalTime(hour = 0, minute = 0),
                ).toInstant(
                    TimeZone.currentSystemDefault(),
                ).toEpochMilliseconds()

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = dateTime.toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        utcTimeMillis < tomorrowMillis
                },
            )
            DatePickerDialog(
                onDismiss = { showDatePicker = false },
                onConfirm = {
                    val newDate = Instant.fromEpochMilliseconds(it)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    val newDateTime = LocalDateTime(newDate.date, dateTime.time)
                    val newTimestamp =
                        newDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
            val dateTime = Instant.fromEpochMilliseconds(uiState.newSession.timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val time = dateTime.time
            val timePickerState = rememberTimePickerState(
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
                        newDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

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
        if (uiState.showSelectLabelDialog) {
            SelectLabelDialog(
                title = "Select label",
                labels = uiState.labels,
                initialSelectedLabels = listOf(uiState.newSession.label),
                onDismiss = { viewModel.setShowSelectLabelDialog(false) },
                singleSelection = true,
                onConfirm = {
                    viewModel.updateSessionToEdit(
                        uiState.newSession.copy(
                            label = it.first(),
                        ),
                    )
                    viewModel.setShowSelectLabelDialog(false)
                },
            )
        }
        if (uiState.showSelectVisibleLabelsDialog) {
            SelectLabelDialog(
                title = "Select labels",
                labels = uiState.labels,
                initialSelectedLabels = uiState.selectedLabels,
                onDismiss = { viewModel.setShowSelectVisibleLabelsDialog(false) },
                singleSelection = false,
                onConfirm = {
                    viewModel.setSelectedLabels(it)
                    viewModel.setShowSelectVisibleLabelsDialog(false)
                },
            )
        }
        if (uiState.showDeleteConfirmationDialog) {
            ConfirmationDialog(
                title = "Delete selected sessions?",
                onDismiss = { viewModel.setShowDeleteConfirmationDialog(false) },
                onConfirm = {
                    viewModel.deleteSelectedSessions()
                    viewModel.clearShowSelectionUi()
                },
            )
        }
        if (uiState.showEditBulkLabelDialog) {
            SelectLabelDialog(
                title = "Edit label",
                labels = uiState.labels,
                onDismiss = { viewModel.setShowEditLabelDialog(false) },
                singleSelection = true,
                onConfirm = {
                    viewModel.setSelectedLabelToBulkEdit(it.first())
                    viewModel.setShowEditLabelDialog(false)
                    viewModel.setShowEditLabelConfirmationDialog(true)
                },
            )
        }
        if (uiState.showEditLabelConfirmationDialog) {
            ConfirmationDialog(
                title = "Change label of selected sessions?",
                onDismiss = { viewModel.setShowEditLabelConfirmationDialog(false) },
                onConfirm = {
                    viewModel.setShowEditLabelConfirmationDialog(false)
                    viewModel.bulkEditLabel()
                    viewModel.clearShowSelectionUi()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreenTopBar(
    onAddButtonClick: () -> Unit,
    onLabelButtonClick: () -> Unit,
    selectedLabelsCount: Int,
    onCancel: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectAll: () -> Unit,
    showSelectionUi: Boolean,
    selectionCount: Int,
    showSeparator: Boolean,
) {
    val colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = Color.Transparent,
    )
    Column {
        Crossfade(showSelectionUi, label = "StatsScreen TopBar") {
            if (it) {
                TopAppBar(
                    title = {
                        if (selectionCount != 0) {
                            // TODO: consider plurals
                            Text("${if (selectionCount > 99) "99+" else selectionCount.toString()} items")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            onDeleteClick()
                        }) {
                            Icon(
                                imageVector = EvaIcons.Outline.Trash,
                                contentDescription = "Delete",
                            )
                        }
                        IconButton(onClick = onSelectAll) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all",
                            )
                        }
                        IconButton(onClick = onLabelButtonClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Label,
                                contentDescription = "Select labels",
                            )
                        }
                    },
                    navigationIcon = {
                        if (showSelectionUi) {
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Cancel",
                                )
                            }
                        }
                    },
                    colors = colors,
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text("Statistics")
                    },
                    actions = {
                        if (!showSelectionUi) {
                            IconButton(onClick = {
                                onAddButtonClick()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add new session",
                                )
                            }
                        }
                        SelectLabelButton(selectedLabelsCount) {
                            onLabelButtonClick()
                        }
                    },
                    colors = colors,
                )
            }
        }
        if (showSeparator) {
            SubtleHorizontalDivider()
        }
    }
}

@Composable
fun SelectLabelButton(count: Int, onClick: () -> Unit) {
    IconButtonWithBadge(
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Label,
                contentDescription = "Navigate to archived labels",
            )
        },
        count = count,
        onClick = onClick,
    )
}
