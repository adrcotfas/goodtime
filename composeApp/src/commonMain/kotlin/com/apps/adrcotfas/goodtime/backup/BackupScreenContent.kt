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

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.apps.adrcotfas.goodtime.bl.TimeUtils
import com.apps.adrcotfas.goodtime.platform.getPlatformConfiguration
import com.apps.adrcotfas.goodtime.ui.ActionCard
import com.apps.adrcotfas.goodtime.ui.BetterListItem
import com.apps.adrcotfas.goodtime.ui.CircularProgressListItem
import com.apps.adrcotfas.goodtime.ui.CompactPreferenceGroupTitle
import com.apps.adrcotfas.goodtime.ui.SwitchListItem
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.CloudUpload
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_backup_now
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_disconnect
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_restore
import goodtime_productivity.composeapp.generated.resources.backup_actions_provider_google_drive
import goodtime_productivity.composeapp.generated.resources.backup_actions_provider_icloud
import goodtime_productivity.composeapp.generated.resources.backup_auto_backup
import goodtime_productivity.composeapp.generated.resources.backup_cloud
import goodtime_productivity.composeapp.generated.resources.backup_enable_cloud_sync
import goodtime_productivity.composeapp.generated.resources.backup_export_backup
import goodtime_productivity.composeapp.generated.resources.backup_export_csv
import goodtime_productivity.composeapp.generated.resources.backup_export_data
import goodtime_productivity.composeapp.generated.resources.backup_export_json
import goodtime_productivity.composeapp.generated.resources.backup_icloud_enable_in_settings
import goodtime_productivity.composeapp.generated.resources.backup_last_backup
import goodtime_productivity.composeapp.generated.resources.backup_local_storage
import goodtime_productivity.composeapp.generated.resources.backup_restore_backup
import goodtime_productivity.composeapp.generated.resources.backup_the_file_can_be_imported_back
import org.jetbrains.compose.resources.stringResource

/**
 * Formats a content URI path to a human-readable folder path.
 * Platform-specific implementation handles proper URI decoding.
 */
expect fun formatFolderPath(uriPath: String): String

@Composable
fun CloudBackupSection(
    enabled: Boolean,
    isConnected: Boolean,
    isCloudUnavailable: Boolean,
    onConnect: (() -> Unit)?,
    cloudAutoBackupEnabled: Boolean,
    onAutoBackupToggle: (Boolean) -> Unit,
    isAutoBackupInProgress: Boolean,
    lastBackupTimestamp: Long,
    onBackup: () -> Unit,
    isBackupInProgress: Boolean,
    onRestore: () -> Unit,
    isRestoreInProgress: Boolean,
    onDisconnect: (() -> Unit)?,
) {
    CompactPreferenceGroupTitle(text = stringResource(Res.string.backup_cloud))

    val cloudProviderName = if (getPlatformConfiguration().isAndroid)
        stringResource(Res.string.backup_actions_provider_google_drive)
    else
        stringResource(Res.string.backup_actions_provider_icloud)

    when {
        // Cloud unavailable (e.g., iCloud disabled in system settings)
        isCloudUnavailable -> {
            val message = stringResource(Res.string.backup_icloud_enable_in_settings)
            BetterListItem(
                leading = {
                    Icon(
                        imageVector = EvaIcons.Outline.CloudUpload,
                        contentDescription = null,
                    )
                },
                title = message,
                enabled = false,
            )
        }
        // Connected - show full controls
        isConnected -> {
            Column {
                SwitchListItem(
                    title = stringResource(Res.string.backup_auto_backup),
                    subtitle = if (cloudAutoBackupEnabled) cloudProviderName else null,
                    checked = cloudAutoBackupEnabled,
                    enabled = enabled,
                    showProgress = isAutoBackupInProgress,
                    onCheckedChange = onAutoBackupToggle,
                )

                if (lastBackupTimestamp > 0) {
                    val lastBackupTime = TimeUtils.formatDateTime(lastBackupTimestamp)
                    BetterListItem(
                        title = stringResource(Res.string.backup_last_backup),
                        trailing = lastBackupTime,
                        enabled = false,
                    )
                }

                CircularProgressListItem(
                    title = stringResource(Res.string.backup_actions_cloud_backup_now),
                    enabled = enabled,
                    showProgress = isBackupInProgress,
                ) {
                    onBackup()
                }

                CircularProgressListItem(
                    title = stringResource(Res.string.backup_actions_cloud_restore),
                    enabled = enabled,
                    showProgress = isRestoreInProgress,
                ) {
                    onRestore()
                }

                if (onDisconnect != null) {
                    BetterListItem(
                        title = stringResource(Res.string.backup_actions_cloud_disconnect),
                        enabled = enabled,
                        onClick = onDisconnect,
                    )
                }
            }
        }
        // Not connected and connect action available (Android)
        onConnect != null -> {
            ActionCard(
                icon = {
                    Icon(
                        imageVector = EvaIcons.Outline.CloudUpload,
                        contentDescription = null,
                    )
                },
                enabled = enabled,
                description = stringResource(Res.string.backup_enable_cloud_sync),
                onClick = onConnect,
            )
        }
    }
}

@Composable
fun LocalBackupSection(
    enabled: Boolean,
    localAutoBackupEnabled: Boolean,
    localAutoBackupPath: String,
    onLocalAutoBackupToggle: (Boolean) -> Unit,
    lastLocalAutoBackupTimestamp: Long,
    backupInProgress: Boolean,
    restoreInProgress: Boolean,
    onLocalBackup: () -> Unit,
    onLocalRestore: () -> Unit,
) {
    CompactPreferenceGroupTitle(text = stringResource(Res.string.backup_local_storage))

    SwitchListItem(
        title = stringResource(Res.string.backup_auto_backup),
        subtitle =
            if (localAutoBackupEnabled && localAutoBackupPath.isNotBlank()) {
                formatFolderPath(localAutoBackupPath)
            } else {
                null
            },
        checked = localAutoBackupEnabled,
        enabled = enabled,
        onCheckedChange = { onLocalAutoBackupToggle(it) },
    )

    if (lastLocalAutoBackupTimestamp > 0) {
        val lastBackupTime =
            TimeUtils.formatDateTime(lastLocalAutoBackupTimestamp)
        BetterListItem(
            title = stringResource(Res.string.backup_last_backup),
            trailing = lastBackupTime,
            enabled = false,
        )
    }

    CircularProgressListItem(
        title = stringResource(Res.string.backup_export_backup),
        subtitle = stringResource(Res.string.backup_the_file_can_be_imported_back),
        enabled = enabled,
        showProgress = backupInProgress,
    ) {
        onLocalBackup()
    }
    CircularProgressListItem(
        title = stringResource(Res.string.backup_restore_backup),
        enabled = enabled,
        showProgress = restoreInProgress,
    ) {
        onLocalRestore()
    }
}

@Composable
fun ExportCsvJsonSection(
    enabled: Boolean,
    isCsvBackupInProgress: Boolean,
    isJsonBackupInProgress: Boolean,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
) {
    CompactPreferenceGroupTitle(text = stringResource(Res.string.backup_export_data))

    CircularProgressListItem(
        title = stringResource(Res.string.backup_export_csv),
        enabled = enabled,
        showProgress = isCsvBackupInProgress,
    ) {
        onExportCsv()
    }
    CircularProgressListItem(
        title = stringResource(Res.string.backup_export_json),
        enabled = enabled,
        showProgress = isJsonBackupInProgress,
    ) {
        onExportJson()
    }
}
