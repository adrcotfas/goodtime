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
package com.apps.adrcotfas.goodtime.labels.archived

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.labels.main.LabelsViewModel
import com.apps.adrcotfas.goodtime.labels.main.archivedLabels
import com.apps.adrcotfas.goodtime.ui.common.BetterDropdownMenu
import com.apps.adrcotfas.goodtime.ui.common.ConfirmationDialog
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import com.apps.adrcotfas.goodtime.ui.common.firstMenuItemModifier
import com.apps.adrcotfas.goodtime.ui.common.lastMenuItemModifier
import com.apps.adrcotfas.goodtime.ui.getLabelColor
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.MoreVertical
import compose.icons.evaicons.outline.Trash
import compose.icons.evaicons.outline.Undo
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.labels_archived_labels_title
import goodtime_productivity.composeapp.generated.resources.labels_delete
import goodtime_productivity.composeapp.generated.resources.labels_delete_desc
import goodtime_productivity.composeapp.generated.resources.labels_more_about
import goodtime_productivity.composeapp.generated.resources.labels_unarchive
import goodtime_productivity.composeapp.generated.resources.main_delete
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedLabelsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LabelsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val labels = uiState.archivedLabels

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var labelToDelete by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.labels_archived_labels_title),
                onNavigateBack = onNavigateBack,
                showSeparator = listState.canScrollBackward,
            )
        },
        content = {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .padding(it)
                        .fillMaxSize(),
            ) {
                items(labels, key = Label::name) { label ->
                    ArchivedLabelListItem(
                        Modifier.animateItem(),
                        label = label,
                        onUnarchive = { viewModel.setArchived(label.name, false) },
                        onDelete = {
                            labelToDelete = label.name
                            showDeleteConfirmationDialog = true
                        },
                        enableUnarchive = uiState.isPro,
                        onLastItemUnarchive =
                            if (labels.size == 1) {
                                onNavigateBack
                            } else {
                                null
                            },
                    )
                }
            }
            if (showDeleteConfirmationDialog) {
                ConfirmationDialog(
                    title = stringResource(Res.string.labels_delete, labelToDelete),
                    subtitle = stringResource(Res.string.labels_delete_desc),
                    onConfirm = {
                        val lastLabelDeleted = labels.size == 1
                        viewModel.deleteLabel(labelToDelete)
                        showDeleteConfirmationDialog = false
                        if (lastLabelDeleted) {
                            onNavigateBack()
                        }
                    },
                    onDismiss = { showDeleteConfirmationDialog = false },
                )
            }
        },
    )
}

@Composable
fun ArchivedLabelListItem(
    modifier: Modifier,
    enableUnarchive: Boolean,
    label: Label,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    onLastItemUnarchive: (() -> Unit)? = null,
) {
    val labelName = label.name

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(4.dp),
    ) {
        Icon(
            modifier =
                Modifier
                    .padding(8.dp),
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.getLabelColor(label.colorIndex),
        )
        Text(
            text = label.name,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.weight(1f))

        var dropDownMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { dropDownMenuExpanded = true }) {
                Icon(
                    EvaIcons.Outline.MoreVertical,
                    contentDescription = stringResource(Res.string.labels_more_about, labelName),
                )
            }

            BetterDropdownMenu(
                expanded = dropDownMenuExpanded,
                onDismissRequest = { dropDownMenuExpanded = false },
            ) {
                val paddingModifier = Modifier.padding(end = 32.dp)
                DropdownMenuItem(
                    enabled = enableUnarchive,
                    modifier = firstMenuItemModifier,
                    text = {
                        Text(
                            modifier = paddingModifier,
                            text = stringResource(Res.string.labels_unarchive),
                        )
                    },
                    onClick = {
                        onUnarchive()
                        dropDownMenuExpanded = false
                        onLastItemUnarchive?.invoke()
                    },
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Undo,
                            contentDescription = "${stringResource(Res.string.labels_unarchive)} $labelName",
                        )
                    },
                )
                DropdownMenuItem(
                    modifier = lastMenuItemModifier,
                    text = {
                        Text(
                            modifier = paddingModifier,
                            text = stringResource(Res.string.main_delete),
                        )
                    },
                    onClick = {
                        onDelete()
                        dropDownMenuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Trash,
                            contentDescription = "${stringResource(Res.string.main_delete)} $labelName",
                        )
                    },
                )
            }
        }
    }
}
