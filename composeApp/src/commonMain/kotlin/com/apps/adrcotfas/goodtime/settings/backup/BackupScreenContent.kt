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
package com.apps.adrcotfas.goodtime.settings.backup

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
import com.apps.adrcotfas.goodtime.backup.BackupUiState
import com.apps.adrcotfas.goodtime.backup.CloudProvider
import com.apps.adrcotfas.goodtime.backup.isBusy
import com.apps.adrcotfas.goodtime.bl.TimeUtils
import com.apps.adrcotfas.goodtime.platform.getPlatformConfiguration
import com.apps.adrcotfas.goodtime.ui.ActionCard
import com.apps.adrcotfas.goodtime.ui.BetterListItem
import com.apps.adrcotfas.goodtime.ui.CircularProgressListItem
import com.apps.adrcotfas.goodtime.ui.CompactPreferenceGroupTitle
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.SwitchListItem
import com.apps.adrcotfas.goodtime.ui.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Unlock
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_backup_now
import goodtime_productivity.composeapp.generated.resources.backup_actions_cloud_restore
import goodtime_productivity.composeapp.generated.resources.backup_actions_icloud_drive
import goodtime_productivity.composeapp.generated.resources.backup_actions_provider_google_drive
import goodtime_productivity.composeapp.generated.resources.backup_actions_provider_icloud
import goodtime_productivity.composeapp.generated.resources.backup_and_restore_title
import goodtime_productivity.composeapp.generated.resources.backup_auto_backup
import goodtime_productivity.composeapp.generated.resources.backup_dialog_cloud_restore_picker_subtitle
import goodtime_productivity.composeapp.generated.resources.backup_dialog_cloud_restore_picker_title
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreenContent(
    uiState: BackupUiState,
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
    onAutoBackupToggle: (Boolean) -> Unit,
    onCloudBackup: () -> Unit = {},
    onCloudRestore: () -> Unit = {},
    onCloudRestoreDismiss: () -> Unit = {},
    onCloudBackupSelected: (String) -> Unit = {},
    onLocalBackup: () -> Unit,
    onLocalRestore: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onToggleExportSection: () -> Unit,
) {
    val listState = rememberScrollState()
    val enabled = uiState.isPro
    val isAndroid = getPlatformConfiguration().isAndroid

    val cloudProviderName =
        when (uiState.cloudProvider) {
            CloudProvider.GOOGLE_DRIVE -> stringResource(Res.string.backup_actions_provider_google_drive)
            CloudProvider.ICLOUD -> stringResource(Res.string.backup_actions_provider_icloud)
        }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopBar(
                    title = stringResource(Res.string.backup_and_restore_title),
                    onNavigateBack = { onNavigateBack() },
                    showSeparator = listState.canScrollBackward,
                )
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(listState)
                        .background(MaterialTheme.colorScheme.background),
            ) {
                if (!enabled) {
                    ActionCard(
                        icon = {
                            Icon(
                                imageVector = EvaIcons.Outline.Unlock,
                                contentDescription = stringResource(Res.string.unlock_premium),
                            )
                        },
                        description = stringResource(Res.string.unlock_premium_to_access_features),
                    ) {
                        onNavigateToPro()
                    }
                }

                // === CLOUD BACKUP SECTION ===
                CompactPreferenceGroupTitle(text = cloudProviderName)

                if (isAndroid) {
                    // TODO: Implement Google Drive auto backup (separate from local auto backup)
                    SwitchListItem(
                        title = stringResource(Res.string.backup_auto_backup),
                        subtitle = null, // TODO: Show "Google Drive" when enabled
                        checked = false, // TODO: Replace with Google Drive auto backup enabled state
                        enabled = enabled,
                        onCheckedChange = { /* TODO: Implement Google Drive auto backup toggle */ },
                    )

                    // TODO: Show last Google Drive backup timestamp (separate from local)
                    if (false) { // TODO: Check Google Drive backup timestamp > 0
                        val lastBackupTime = "" // TODO: Format Google Drive backup timestamp
                        BetterListItem(
                            title = stringResource(Res.string.backup_last_backup),
                            trailing = lastBackupTime,
                            enabled = false,
                        )
                    }
                } else {
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
                        onCheckedChange = { onAutoBackupToggle(it) },
                    )

                    // Show last backup time if available
                    if (uiState.backupSettings.cloudAutoBackupEnabled && uiState.backupSettings.lastBackupTimestamp > 0) {
                        val lastBackupTime =
                            TimeUtils.formatDateTime(uiState.backupSettings.lastBackupTimestamp)
                        BetterListItem(
                            title = stringResource(Res.string.backup_last_backup),
                            trailing = lastBackupTime,
                            enabled = false,
                        )
                    }
                }

                // Cloud backup now button
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_actions_cloud_backup_now),
                    enabled = enabled,
                    showProgress = uiState.isCloudBackupInProgress,
                ) {
                    onCloudBackup()
                }

                // Cloud restore button
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_actions_cloud_restore),
                    enabled = enabled,
                    showProgress = uiState.isCloudRestoreInProgress,
                ) {
                    onCloudRestore()
                }

                // === LOCAL STORAGE SECTION ===
                SubtleHorizontalDivider()
                CompactPreferenceGroupTitle(text = stringResource(Res.string.backup_local_storage))

                // Android only: local auto backup
                if (isAndroid) {
                    SwitchListItem(
                        title = stringResource(Res.string.backup_auto_backup),
                        subtitle =
                            if (uiState.backupSettings.autoBackupEnabled && uiState.backupSettings.path.isNotBlank()) {
                                formatFolderPath(uiState.backupSettings.path)
                            } else {
                                null
                            },
                        checked = uiState.backupSettings.autoBackupEnabled,
                        enabled = enabled,
                        onCheckedChange = { onAutoBackupToggle(it) },
                    )

                    // Show last backup time if available
                    if (uiState.backupSettings.lastBackupTimestamp > 0) {
                        val lastBackupTime =
                            TimeUtils.formatDateTime(uiState.backupSettings.lastBackupTimestamp)
                        BetterListItem(
                            title = stringResource(Res.string.backup_last_backup),
                            trailing = lastBackupTime,
                            enabled = false,
                        )
                    }
                }

                // Manual local backup and restore (both platforms)
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_export_backup),
                    subtitle = stringResource(Res.string.backup_the_file_can_be_imported_back),
                    enabled = enabled,
                    showProgress = uiState.isBackupInProgress,
                ) {
                    onLocalBackup()
                }
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_restore_backup),
                    enabled = enabled,
                    showProgress = uiState.isRestoreInProgress,
                ) {
                    onLocalRestore()
                }

                // === EXPORT DATA SECTION ===
                SubtleHorizontalDivider()
                CompactPreferenceGroupTitle(text = stringResource(Res.string.backup_export_data))

                CircularProgressListItem(
                    title = stringResource(Res.string.backup_export_csv),
                    enabled = enabled,
                    showProgress = uiState.isCsvBackupInProgress,
                ) {
                    onExportCsv()
                }
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_export_json),
                    enabled = enabled,
                    showProgress = uiState.isJsonBackupInProgress,
                ) {
                    onExportJson()
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isBusy,
            enter = fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clearAndSetSemantics { }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume taps */ },
                        ),
            )
        }

        // Cloud restore picker dialog
        if (uiState.showCloudRestoreDialog && uiState.availableCloudBackups.isNotEmpty()) {
            CloudRestorePickerDialog(
                cloudProviderName = cloudProviderName,
                backups = uiState.availableCloudBackups,
                onDismiss = onCloudRestoreDismiss,
                onBackupSelected = onCloudBackupSelected,
            )
        }
    }
}

@Composable
private fun CloudRestorePickerDialog(
    cloudProviderName: String,
    backups: List<String>,
    onDismiss: () -> Unit,
    onBackupSelected: (String) -> Unit,
) {
    var selectedBackup by remember(backups) { mutableStateOf(backups.firstOrNull() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.backup_dialog_cloud_restore_picker_title)) },
        text = {
            Column {
                Text(
                    stringResource(Res.string.backup_dialog_cloud_restore_picker_subtitle, cloudProviderName),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
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
