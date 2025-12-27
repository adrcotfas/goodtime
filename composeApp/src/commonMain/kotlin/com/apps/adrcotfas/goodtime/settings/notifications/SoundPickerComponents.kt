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
package com.apps.adrcotfas.goodtime.settings.notifications

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import com.apps.adrcotfas.goodtime.ui.BetterDropdownMenu
import com.apps.adrcotfas.goodtime.ui.firstMenuItemModifier
import com.apps.adrcotfas.goodtime.ui.lastMenuItemModifier
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.Outline
import compose.icons.evaicons.fill.Music
import compose.icons.evaicons.outline.Bell
import compose.icons.evaicons.outline.BellOff
import compose.icons.evaicons.outline.CheckmarkCircle2
import compose.icons.evaicons.outline.Trash
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.main_cancel
import goodtime_productivity.composeapp.generated.resources.main_ok
import goodtime_productivity.composeapp.generated.resources.settings_delete_sound
import goodtime_productivity.composeapp.generated.resources.settings_remove
import org.jetbrains.compose.resources.stringResource

@Composable
private fun NotificationSoundItemBase(
    modifier: Modifier = Modifier,
    name: String,
    isSilent: Boolean = false,
    isCustomSound: Boolean = false,
    isSelected: Boolean,
    content: @Composable () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .let {
                    if (isSelected) {
                        it.background(
                            MaterialTheme.colorScheme.inverseSurface.copy(
                                alpha = 0.1f,
                            ),
                        )
                    } else {
                        it
                    }
                }.padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()

        Icon(
            imageVector =
                if (isCustomSound) {
                    EvaIcons.Fill.Music
                } else if (isSilent) {
                    EvaIcons.Outline.BellOff
                } else {
                    EvaIcons.Outline.Bell
                },
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        if (isSelected) {
            Icon(
                imageVector = EvaIcons.Outline.CheckmarkCircle2,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun NotificationSoundItem(
    modifier: Modifier = Modifier,
    name: String,
    isSilent: Boolean = false,
    isCustomSound: Boolean = false,
    isSelected: Boolean,
    onSelected: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    NotificationSoundItemBase(
        modifier = modifier.clickable { onSelected() },
        name = name,
        isSilent = isSilent,
        isCustomSound = isCustomSound,
        isSelected = isSelected,
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationSoundItem(
    modifier: Modifier = Modifier,
    name: String,
    isSilent: Boolean = false,
    isCustomSound: Boolean = false,
    isSelected: Boolean,
    onRemove: (() -> Unit)? = null,
    onSelected: () -> Unit,
) {
    var dropDownMenuExpanded by remember { mutableStateOf(false) }
    NotificationSoundItemBase(
        modifier =
            modifier.combinedClickable(
                onClick = { onSelected() },
                onLongClick = {
                    if (onRemove != null) {
                        dropDownMenuExpanded = true
                    }
                },
            ),
        name = name,
        isSilent = isSilent,
        isCustomSound = isCustomSound,
        isSelected = isSelected,
    ) {
        if (onRemove != null) {
            BetterDropdownMenu(
                expanded = dropDownMenuExpanded,
                onDismissRequest = { dropDownMenuExpanded = false },
            ) {
                val paddingModifier = Modifier.padding(end = 32.dp)
                DropdownMenuItem(
                    modifier = firstMenuItemModifier.then(lastMenuItemModifier),
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Trash,
                            contentDescription = stringResource(Res.string.settings_delete_sound),
                        )
                    },
                    text = { Text(modifier = paddingModifier, text = stringResource(Res.string.settings_remove)) },
                    onClick = {
                        onRemove()
                        dropDownMenuExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SoundPickerButtonsRow(
    onSave: (SoundData) -> Unit,
    onDismiss: () -> Unit,
    selectedItem: SoundData,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            modifier =
                Modifier
                    .padding(end = 8.dp, bottom = 4.dp),
            onClick = onDismiss,
        ) { Text(stringResource(Res.string.main_cancel)) }

        TextButton(
            modifier =
                Modifier
                    .padding(end = 8.dp, bottom = 4.dp),
            onClick = {
                onSave(selectedItem)
                onDismiss()
            },
        ) { Text(stringResource(Res.string.main_ok)) }
    }
}
