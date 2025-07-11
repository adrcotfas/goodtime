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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.BetterDropdownMenu
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.firstMenuItemModifier
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.MoreVertical
import compose.icons.evaicons.outline.Trash

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreenTopBar(
    onNavigateBack: () -> Unit,
    onAddButtonClick: () -> Unit,
    onLabelButtonClick: () -> Unit,
    onCancel: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectAll: () -> Unit,
    showSelectionUi: Boolean,
    selectionCount: Int,
    showSeparator: Boolean,
    showBreaks: Boolean,
    onSetShowBreaks: (Boolean) -> Unit,
    showArchived: Boolean,
    onSetShowArchived: (Boolean) -> Unit,
) {
    val colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        )
    Column {
        Crossfade(showSelectionUi, label = "StatsScreen TopBar") {
            if (it) {
                TopAppBar(
                    title = {
                        if (selectionCount != 0) {
                            Text(if (selectionCount > 99) "99+" else selectionCount.toString())
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            onDeleteClick()
                        }) {
                            Icon(
                                imageVector = EvaIcons.Outline.Trash,
                                contentDescription = stringResource(R.string.main_delete),
                            )
                        }
                        IconButton(onClick = onSelectAll) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = stringResource(R.string.stats_select_all),
                            )
                        }
                        IconButton(onClick = onLabelButtonClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Label,
                                contentDescription = stringResource(R.string.labels_edit_label),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.main_cancel),
                            )
                        }
                    },
                    colors = colors,
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(stringResource(R.string.stats_title))
                    },
                    actions = {
                        IconButton(onClick = {
                            onAddButtonClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.stats_add_session),
                            )
                        }
                        IconButton(onClick = onLabelButtonClick) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Label,
                                stringResource(R.string.labels_select_labels),
                            )
                        }
                        var dropDownMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { dropDownMenuExpanded = true }) {
                                Icon(
                                    EvaIcons.Outline.MoreVertical,
                                    contentDescription = stringResource(R.string.stats_more_options),
                                )
                            }

                            BetterDropdownMenu(
                                expanded = dropDownMenuExpanded,
                                onDismissRequest = { dropDownMenuExpanded = false },
                            ) {
                                val paddingModifier = Modifier.padding(end = 32.dp)
                                DropdownMenuItem(
                                    modifier = firstMenuItemModifier,
                                    leadingIcon = {
                                        Checkbox(checked = showBreaks, onCheckedChange = {
                                            onSetShowBreaks(!showBreaks)
                                        })
                                    },
                                    text = {
                                        Text(
                                            modifier = paddingModifier,
                                            text = stringResource(R.string.stats_show_breaks),
                                        )
                                    },
                                    onClick = {
                                        onSetShowBreaks(!showBreaks)
                                        dropDownMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Checkbox(checked = showArchived, onCheckedChange = {
                                            onSetShowArchived(!showArchived)
                                        })
                                    },
                                    text = {
                                        Text(
                                            modifier = paddingModifier,
                                            text = stringResource(R.string.stats_show_archived),
                                        )
                                    },
                                    onClick = {
                                        onSetShowArchived(!showArchived)
                                        dropDownMenuExpanded = false
                                    },
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.main_navigate_back),
                            )
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
