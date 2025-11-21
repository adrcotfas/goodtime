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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.getLabelData
import com.apps.adrcotfas.goodtime.data.model.isDefault
import com.apps.adrcotfas.goodtime.labels.main.LabelsViewModel
import com.apps.adrcotfas.goodtime.labels.main.unarchivedLabels
import com.apps.adrcotfas.goodtime.ui.AlertDialogButtonStack
import com.apps.adrcotfas.goodtime.ui.SelectLabelDialog
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.labels_clear_label
import goodtime_productivity.composeapp.generated.resources.labels_edit_active_label
import goodtime_productivity.composeapp.generated.resources.labels_edit_labels
import goodtime_productivity.composeapp.generated.resources.labels_select_active_label
import goodtime_productivity.composeapp.generated.resources.settings_timer_durations_title
import goodtime_productivity.composeapp.generated.resources.stats_no_items
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectActiveLabelDialog(
    viewModel: LabelsViewModel = koinViewModel(),
    initialSelectedLabel: String,
    onNavigateToLabels: () -> Unit,
    onNavigateToActiveLabel: () -> Unit,
    onNavigateToTimerDurations: () -> Unit,
    onClearLabel: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val labels = uiState.unarchivedLabels.filter { !it.isDefault() }.map { it.getLabelData() }

    val labelsIsEmpty = labels.isEmpty()
    val isDefaultLabelActive = initialSelectedLabel == Label.DEFAULT_LABEL_NAME

    if (uiState.isLoading) return
    SelectLabelDialog(
        title = stringResource(Res.string.labels_select_active_label),
        singleSelection = true,
        labels = labels,
        initialSelectedLabels = listOf(initialSelectedLabel),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        extraContent = {
            if (labelsIsEmpty) EmptyState()
        },
        buttons = {
            AlertDialogButtonStack {
                TextButton(onClick = {
                    if (isDefaultLabelActive) {
                        onNavigateToTimerDurations()
                    } else {
                        onNavigateToActiveLabel()
                    }
                }) {
                    Text(
                        stringResource(
                            if (labelsIsEmpty || isDefaultLabelActive) {
                                Res.string.settings_timer_durations_title
                            } else {
                                Res.string.labels_edit_active_label
                            },
                        ),
                    )
                }
                if (!isDefaultLabelActive) {
                    TextButton(onClick = onClearLabel) { Text(stringResource(Res.string.labels_clear_label)) }
                }
                TextButton(onClick = onNavigateToLabels) { Text(stringResource(Res.string.labels_edit_labels)) }
            }
        },
    )
}

@Composable
private fun EmptyState() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.stats_no_items),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
    }
}
