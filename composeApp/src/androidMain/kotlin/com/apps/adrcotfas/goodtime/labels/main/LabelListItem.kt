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
package com.apps.adrcotfas.goodtime.labels.main

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.ui.common.BetterDropdownMenu
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.firstMenuItemModifier
import com.apps.adrcotfas.goodtime.ui.common.lastMenuItemModifier
import com.apps.adrcotfas.goodtime.ui.getLabelColor
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Archive
import compose.icons.evaicons.outline.Copy
import compose.icons.evaicons.outline.Edit
import compose.icons.evaicons.outline.MoreVertical
import compose.icons.evaicons.outline.Trash

@Composable
fun LabelListItem(
    label: Label,
    isDragging: Boolean,
    @SuppressLint("ModifierParameter")
    dragModifier: Modifier,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val labelName = label.name

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .clickable { onEdit() }
                .let {
                    if (isDragging) {
                        it.background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f),
                        )
                    } else {
                        it
                    }
                }.padding(4.dp),
    ) {
        Icon(
            modifier =
                dragModifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                ),
            imageVector = Icons.Filled.DragIndicator,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            contentDescription = "Drag indicator for $labelName",
        )
        Icon(
            modifier =
                Modifier
                    .padding(8.dp),
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = labelName,
            tint = MaterialTheme.getLabelColor(label.colorIndex),
        )
        Text(
            modifier = Modifier.weight(1f),
            text = labelName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )

        var dropDownMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { dropDownMenuExpanded = true }) {
                Icon(
                    EvaIcons.Outline.MoreVertical,
                    contentDescription = stringResource(R.string.labels_more_about, labelName),
                )
            }
            BetterDropdownMenu(
                expanded = dropDownMenuExpanded,
                onDismissRequest = { dropDownMenuExpanded = false },
            ) {
                val paddingModifier = Modifier.padding(end = 32.dp)
                DropdownMenuItem(
                    modifier = firstMenuItemModifier,
                    text = {
                        Text(
                            modifier = paddingModifier,
                            text = stringResource(R.string.main_edit),
                        )
                    },
                    onClick = {
                        onEdit()
                        dropDownMenuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Edit,
                            contentDescription =
                                stringResource(
                                    R.string.labels_edit_label,
                                    labelName,
                                ),
                        )
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            modifier = paddingModifier,
                            text = stringResource(R.string.main_duplicate),
                        )
                    },
                    onClick = {
                        onDuplicate()
                        dropDownMenuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Copy,
                            contentDescription =
                                stringResource(
                                    R.string.labels_duplicate_label_name,
                                    labelName,
                                ),
                        )
                    },
                )
                SubtleHorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            modifier = paddingModifier,
                            text = stringResource(R.string.labels_archived),
                        )
                    },
                    onClick = {
                        onArchive()
                        dropDownMenuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Archive,
                            contentDescription =
                                stringResource(
                                    R.string.labels_archive_label_name,
                                    labelName,
                                ),
                        )
                    },
                )
                DropdownMenuItem(
                    modifier = lastMenuItemModifier,
                    text = {
                        Text(
                            modifier = paddingModifier,
                            text = stringResource(R.string.main_delete),
                        )
                    },
                    onClick = {
                        onDelete()
                        dropDownMenuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Trash,
                            contentDescription =
                                stringResource(
                                    R.string.labels_delete_label_name,
                                    labelName,
                                ),
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun LabelCardPreview() {
    LabelListItem(
        label =
            Label(
                name = "Default",
                useDefaultTimeProfile = false,
                timerProfile = TimerProfile(sessionsBeforeLongBreak = 4),
            ),
        isDragging = false,
        dragModifier = Modifier,
        onEdit = {},
        onDuplicate = {},
        onArchive = {},
        onDelete = {},
    )
}
