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
package com.apps.adrcotfas.goodtime.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_restore
import goodtime_productivity.composeapp.generated.resources.backup_dialog_cloud_restore_picker_subtitle
import goodtime_productivity.composeapp.generated.resources.backup_dialog_cloud_restore_picker_title
import goodtime_productivity.composeapp.generated.resources.main_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun CloudRestorePickerDialog(
    backups: List<String>,
    onDismiss: () -> Unit,
    onBackupSelected: (String) -> Unit,
) {
    var selectedBackup by remember(backups) { mutableStateOf(backups.firstOrNull() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.backup_dialog_cloud_restore_picker_title)) },
        text = {
            LazyColumn(
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                items(backups.size) { index ->
                    val backup = backups[index]
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedBackup = backup }
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedBackup == backup,
                            onClick = { selectedBackup = backup },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = backup,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedBackup.isNotBlank()) {
                        onBackupSelected(selectedBackup)
                    }
                },
            ) {
                Text(stringResource(Res.string.backup_actions_cloud_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.main_cancel))
            }
        },
    )
}
