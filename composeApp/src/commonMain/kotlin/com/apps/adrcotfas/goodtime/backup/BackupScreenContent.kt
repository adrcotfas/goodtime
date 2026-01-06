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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.bl.TimeUtils
import com.apps.adrcotfas.goodtime.common.UnlockFeaturesActionCard
import com.apps.adrcotfas.goodtime.ui.ActionCard
import com.apps.adrcotfas.goodtime.ui.BetterListItem
import com.apps.adrcotfas.goodtime.ui.CircularProgressListItem
import com.apps.adrcotfas.goodtime.ui.CompactPreferenceGroupTitle
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.SwitchListItem
import com.apps.adrcotfas.goodtime.ui.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.CloudUpload
import compose.icons.evaicons.outline.Unlock
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_backup_now
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_disconnect
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_restore
import goodtime_productivity.composeapp.generated.resources.backup_actions_provider_google_drive
import goodtime_productivity.composeapp.generated.resources.backup_and_restore_title
import goodtime_productivity.composeapp.generated.resources.backup_auto_backup
import goodtime_productivity.composeapp.generated.resources.backup_cloud
import goodtime_productivity.composeapp.generated.resources.backup_dialog_cloud_restore_picker_subtitle
import goodtime_productivity.composeapp.generated.resources.backup_dialog_cloud_restore_picker_title
import goodtime_productivity.composeapp.generated.resources.backup_enable_cloud_sync
import goodtime_productivity.composeapp.generated.resources.backup_export_backup
import goodtime_productivity.composeapp.generated.resources.backup_export_csv
import goodtime_productivity.composeapp.generated.resources.backup_export_data
import goodtime_productivity.composeapp.generated.resources.backup_export_json
import goodtime_productivity.composeapp.generated.resources.backup_last_backup
import goodtime_productivity.composeapp.generated.resources.backup_local_storage
import goodtime_productivity.composeapp.generated.resources.backup_restore_backup
import goodtime_productivity.composeapp.generated.resources.backup_the_file_can_be_imported_back
import goodtime_productivity.composeapp.generated.resources.main_cancel
import goodtime_productivity.composeapp.generated.resources.unlock_premium
import goodtime_productivity.composeapp.generated.resources.unlock_premium_to_access_features
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
    onConnect: () -> Unit,
    cloudAutoBackupEnabled: Boolean,
    onAutoBackupToggle: (Boolean) -> Unit,
    isAutoBackupInProgress: Boolean,
    lastBackupTimestamp: Long,
    onBackup: () -> Unit,
    isBackupInProgress: Boolean,
    onRestore: () -> Unit,
    isRestoreInProgress: Boolean,
    onDisconnect: () -> Unit,
) {
    CompactPreferenceGroupTitle(text = stringResource(Res.string.backup_cloud))

    if (isConnected) {
        Column {
            SwitchListItem(
                title = stringResource(Res.string.backup_auto_backup),
                subtitle =
                    if (cloudAutoBackupEnabled) {
                        stringResource(Res.string.backup_actions_provider_google_drive)
                    } else {
                        null
                    },
                checked = cloudAutoBackupEnabled,
                enabled = enabled,
                showProgress = isAutoBackupInProgress,
                onCheckedChange = onAutoBackupToggle,
            )

            if (cloudAutoBackupEnabled &&
                lastBackupTimestamp > 0
            ) {
                val lastBackupTime =
                    TimeUtils.formatDateTime(lastBackupTimestamp)
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

            BetterListItem(
                title = stringResource(Res.string.backup_actions_cloud_disconnect),
                enabled = enabled,
                onClick = onDisconnect,
            )
        }
    } else {
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