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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.bl.AndroidTimeUtils.formatToPrettyDateAndTime
import com.apps.adrcotfas.goodtime.bl.LabelData
import com.apps.adrcotfas.goodtime.bl.isDefault
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.ui.common.EditableNumberListItem
import com.apps.adrcotfas.goodtime.ui.common.TextBox
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Clock

@Composable
fun AddEditSessionContent(
    session: Session,
    labelData: LabelData,
    onOpenDatePicker: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onOpenLabelSelector: () -> Unit,
    onUpdate: (Session) -> Unit,
    onValidate: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
                .animateContentSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        val context = LocalContext.current
        val (date, time) = session.timestamp.formatToPrettyDateAndTime(context)

        Row(
            modifier = Modifier.padding(start = 68.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilterChip(
                onClick = { onUpdate(session.copy(isWork = true)) },
                label = {
                    Text(stringResource(R.string.stats_focus))
                },
                selected = session.isWork,
            )

            FilterChip(
                onClick = { onUpdate(session.copy(isWork = false, interruptions = 0)) },
                label = {
                    Text(stringResource(R.string.stats_break))
                },
                selected = !session.isWork,
            )
        }

        EditableNumberListItem(
            title = stringResource(R.string.stats_duration),
            value = session.duration.let { if (it != 0L) it.toInt() else null },
            icon = {
                Spacer(modifier = Modifier.width(36.dp))
            },
            restoreValueOnClearFocus = false,
            onValueChange = { onUpdate(session.copy(duration = it.toLong())) },
            onValueEmpty = { onValidate(!it) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(0.7f)
                        .clickable {
                            onOpenDatePicker()
                        },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Icon(
                    modifier =
                        Modifier.padding(
                            start = 28.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 16.dp,
                        ),
                    imageVector = EvaIcons.Outline.Clock,
                    contentDescription = stringResource(R.string.stats_time),
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                modifier =
                    Modifier
                        .height(48.dp)
                        .weight(0.3f)
                        .clickable {
                            onOpenTimePicker()
                        },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    modifier = Modifier.padding(end = 24.dp),
                    text = time,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenLabelSelector()
                    },
            leadingContent = {
                Icon(
                    modifier = Modifier.padding(start = 12.dp),
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = stringResource(R.string.stats_label),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                if (labelData.isDefault()) {
                    Text(
                        text = stringResource(R.string.labels_add_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val colorIndex = labelData.colorIndex
                    LabelChip(session.label, colorIndex, onClick = onOpenLabelSelector)
                }
            },
        )
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                modifier = Modifier.padding(16.dp),
                imageVector = Icons.AutoMirrored.Outlined.Notes,
                contentDescription = stringResource(R.string.stats_add_notes),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            TextBox(
                modifier =
                    Modifier
                        .padding(top = 14.dp)
                        .weight(1f),
                value = session.notes,
                onValueChange = { onUpdate(session.copy(notes = it)) },
                placeholder = stringResource(R.string.stats_add_notes),
            )
        }
    }
}
