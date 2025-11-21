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
package com.apps.adrcotfas.goodtime.settings.timerdurations

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.ui.ConfirmationDialog
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.labels_delete
import goodtime_productivity.composeapp.generated.resources.settings_delete_profile
import goodtime_productivity.composeapp.generated.resources.settings_delete_profile_confirmation
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerProfileBottomSheet(
    profiles: List<TimerProfile>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<TimerProfile?>(null) }

    if (showDeleteConfirmationDialog) {
        ConfirmationDialog(
            title = stringResource(Res.string.settings_delete_profile),
            subtitle =
                stringResource(
                    Res.string.settings_delete_profile_confirmation,
                    profileToDelete?.name ?: "",
                ),
            onConfirm = {
                profileToDelete?.name?.let { onDelete(it) }
                showDeleteConfirmationDialog = false
            },
            onDismiss = { showDeleteConfirmationDialog = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(modifier = Modifier.animateContentSize()) {
            items(profiles) { profile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text(
                        text = profile.name ?: "",
                        modifier = Modifier.padding(start = 16.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        profileToDelete = profile
                        showDeleteConfirmationDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription =
                                stringResource(
                                    Res.string.labels_delete,
                                    profile.name ?: "",
                                ),
                        )
                    }
                }
            }
        }
    }
}
