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

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.apps.adrcotfas.goodtime.bl.TimeUtils
import com.apps.adrcotfas.goodtime.ui.ActionCard
import com.apps.adrcotfas.goodtime.ui.BetterListItem
import com.apps.adrcotfas.goodtime.ui.CircularProgressListItem
import com.apps.adrcotfas.goodtime.ui.SwitchListItem
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.CloudUpload
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_backup_now
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_restore
import goodtime_productivity.composeapp.generated.resources.backup_actions_icloud_drive
import goodtime_productivity.composeapp.generated.resources.backup_auto_backup
import goodtime_productivity.composeapp.generated.resources.backup_icloud_enable_in_settings
import goodtime_productivity.composeapp.generated.resources.backup_last_backup
import org.jetbrains.compose.resources.stringResource

/**
 * Cloud backup section for iOS builds.
 * Shows disabled ActionCard when iCloud is unavailable,
 * otherwise shows iCloud backup controls.
 */
@Composable
fun CloudBackupSection(
    uiState: BackupUiState,
    enabled: Boolean,
    onAutoBackupToggle: (Boolean) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val isICloudUnavailable = uiState.cloudIssue == CloudAutoBackupIssue.ICLOUD_UNAVAILABLE

    if (isICloudUnavailable) {
        // iCloud is disabled in system settings - show disabled card
        ActionCard(
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.CloudUpload,
                    contentDescription = null,
                )
            },
            enabled = false,
            description = stringResource(Res.string.backup_icloud_enable_in_settings),
            onClick = {},
        )
    } else {
        // iCloud available - show full cloud backup controls
        SwitchListItem(
            title = stringResource(Res.string.backup_auto_backup),
            subtitle =
                if (uiState.backupSettings.cloudAutoBackupEnabled) {
                    stringResource(Res.string.backup_actions_icloud_drive)
                } else {
                    null
                },
            checked = uiState.backupSettings.cloudAutoBackupEnabled,
            enabled = enabled,
            showProgress = uiState.isCloudAutoBackupToggleInProgress,
            onCheckedChange = onAutoBackupToggle,
        )

        // Show last backup time if available
        if (uiState.backupSettings.cloudAutoBackupEnabled &&
            uiState.backupSettings.cloudLastBackupTimestamp > 0
        ) {
            val lastBackupTime = TimeUtils.formatDateTime(uiState.backupSettings.cloudLastBackupTimestamp)
            BetterListItem(
                title = stringResource(Res.string.backup_last_backup),
                trailing = lastBackupTime,
                enabled = false,
            )
        }

        // Backup now
        CircularProgressListItem(
            title = stringResource(Res.string.backup_actions_cloud_backup_now),
            enabled = enabled,
            showProgress = uiState.isCloudBackupInProgress,
        ) {
            onBackup()
        }

        // Restore
        CircularProgressListItem(
            title = stringResource(Res.string.backup_actions_cloud_restore),
            enabled = enabled,
            showProgress = uiState.isCloudRestoreInProgress,
        ) {
            onRestore()
        }

        // No disconnect on iOS - iCloud is system-level
    }
}
