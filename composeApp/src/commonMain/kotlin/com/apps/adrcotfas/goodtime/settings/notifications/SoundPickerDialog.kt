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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import com.apps.adrcotfas.goodtime.ui.PreferenceGroupTitle
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_silent
import goodtime_productivity.composeapp.generated.resources.settings_system_sounds
import org.jetbrains.compose.resources.stringResource

@Composable expect fun NotificationSoundPickerDialog(
    title: String,
    selectedItem: SoundData,
    onSelected: (SoundData) -> Unit,
    onSave: (SoundData) -> Unit,
    onDismiss: () -> Unit,
)

@Composable
fun NotificationSoundPickerDialogContent(
    title: String,
    selectedItem: SoundData,
    items: Set<SoundData>,
    onSelected: (SoundData) -> Unit,
    onSave: (SoundData) -> Unit,
    onDismiss: () -> Unit,
    platformSpecificContent: (LazyListScope.() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
                Modifier
                    .background(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(top = 24.dp)
                        .fillMaxHeight(0.75f),
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    modifier =
                        Modifier
                            .padding(start = 24.dp)
                            .fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    platformSpecificContent?.invoke(this)

                    item(key = "system sounds") {
                        PreferenceGroupTitle(
                            modifier = Modifier.animateItem(),
                            text = stringResource(Res.string.settings_system_sounds),
                        )
                    }
                    item(key = "silent") {
                        NotificationSoundItem(
                            modifier = Modifier.animateItem(),
                            name = stringResource(Res.string.settings_silent),
                            isSilent = true,
                            isSelected = selectedItem.isSilent,
                        ) {
                            onSelected(SoundData(isSilent = true))
                        }
                    }
                    items(items.toList(), key = { it.uriString }) { item ->
                        val isSelected = selectedItem == item
                        NotificationSoundItem(
                            modifier = Modifier.animateItem(),
                            name = item.name,
                            isSelected = isSelected,
                            onSelected = { onSelected(item) },
                            content = {},
                        )
                    }
                }
                SoundPickerButtonsRow(onSave, onDismiss, selectedItem)
            }
        }
    }
}
