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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.common.UnlockFeaturesActionCard
import com.apps.adrcotfas.goodtime.ui.SnackbarController
import com.apps.adrcotfas.goodtime.ui.SnackbarEvent
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.TopBar
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_actions_icloud_drive
import goodtime_productivity.composeapp.generated.resources.backup_and_restore_title
import goodtime_productivity.composeapp.generated.resources.backup_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_failed_please_try_again
import goodtime_productivity.composeapp.generated.resources.backup_no_backups_found
import goodtime_productivity.composeapp.generated.resources.backup_restore_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_restore_failed_please_try_again
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun BackupScreen(
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
    onNavigateToMainAndReset: () -> Unit,
) {
    val backupViewModel: BackupViewModel = koinViewModel()
    val cloudBackupViewModel: CloudBackupViewModel = koinViewModel()

    val backupUiState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val cloudUiState by cloudBackupViewModel.uiState.collectAsStateWithLifecycle()

    if (backupUiState.isLoading) return

    // Handle local backup results
    LaunchedEffect(backupUiState.backupResult) {
        backupUiState.backupResult?.let {
            if (it != BackupPromptResult.CANCELLED) {
                val message =
                    if (it == BackupPromptResult.SUCCESS) {
                        getString(Res.string.backup_completed_successfully)
                    } else {
                        getString(Res.string.backup_failed_please_try_again)
                    }
                SnackbarController.sendEvent(SnackbarEvent(message = message))
            }
            backupViewModel.clearBackupError()
        }
    }

    // Handle local restore results
    LaunchedEffect(backupUiState.restoreResult) {
        backupUiState.restoreResult?.let {
            if (it != BackupPromptResult.CANCELLED) {
                val message =
                    when (it) {
                        BackupPromptResult.SUCCESS -> getString(Res.string.backup_restore_completed_successfully)
                        BackupPromptResult.NO_BACKUPS_FOUND -> getString(Res.string.backup_no_backups_found)
                        else -> getString(Res.string.backup_restore_failed_please_try_again)
                    }
                SnackbarController.sendEvent(
                    SnackbarEvent(message = message, duration = SnackbarDuration.Short),
                )
            }
            if (it == BackupPromptResult.SUCCESS) {
                onNavigateToMainAndReset()
            }
            backupViewModel.clearRestoreError()
        }
    }

    // Handle cloud backup results
    LaunchedEffect(cloudUiState.backupResult) {
        cloudUiState.backupResult?.let {
            val message =
                if (it == BackupPromptResult.SUCCESS) {
                    getString(Res.string.backup_completed_successfully)
                } else {
                    getString(Res.string.backup_failed_please_try_again)
                }
            SnackbarController.sendEvent(SnackbarEvent(message = message))
            cloudBackupViewModel.clearBackupResult()
        }
    }

    // Handle cloud restore results
    //TODO# can we remove the restoreResult and just fire a snackbarevent from the viewmodel instead? valid for android too and valid for all the above snackbar events sent from this screen and the android ones
    LaunchedEffect(cloudUiState.restoreResult) {
        cloudUiState.restoreResult?.let {
            val message =
                when (it) {
                    BackupPromptResult.SUCCESS -> getString(Res.string.backup_restore_completed_successfully)
                    BackupPromptResult.NO_BACKUPS_FOUND -> getString(Res.string.backup_no_backups_found)
                    else -> getString(Res.string.backup_restore_failed_please_try_again)
                }
            SnackbarController.sendEvent(
                SnackbarEvent(message = message, duration = SnackbarDuration.Short),
            )
            if (it == BackupPromptResult.SUCCESS) {
                //TODO# remove this functionality. Restoring a backup should not make the app leave the backup screen(here and on the android side too)
                onNavigateToMainAndReset()
            }
            cloudBackupViewModel.clearRestoreResult()
        }
    }

    val cloudProviderName = stringResource(Res.string.backup_actions_icloud_drive)

    val listState = rememberScrollState()
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
            UnlockFeaturesActionCard(
                backupUiState.isPro,
                onNavigateToPro = onNavigateToPro,
            )

            CloudBackupSection(
                enabled = backupUiState.isPro,
                isConnected = cloudUiState.isConnected,
                isCloudUnavailable = cloudUiState.isCloudUnavailable,
                onConnect = null,
                cloudProviderName = cloudProviderName,
                cloudAutoBackupEnabled = backupUiState.backupSettings.cloudAutoBackupEnabled,
                onAutoBackupToggle = { cloudBackupViewModel.toggleAutoBackup(it) },
                isAutoBackupInProgress = cloudUiState.isAutoBackupToggleInProgress,
                lastBackupTimestamp = backupUiState.backupSettings.cloudLastBackupTimestamp,
                onBackup = { cloudBackupViewModel.backup() },
                isBackupInProgress = cloudUiState.isBackupInProgress,
                onRestore = { cloudBackupViewModel.restore() },
                isRestoreInProgress = cloudUiState.isRestoreInProgress,
                onDisconnect = null,
            )

            SubtleHorizontalDivider()

            // iOS doesn't have local auto-backup feature
            LocalBackupSection(
                enabled = backupUiState.isPro,
                localAutoBackupEnabled = false,
                localAutoBackupPath = "",
                onLocalAutoBackupToggle = { /* Not used on iOS */ },
                lastLocalAutoBackupTimestamp = 0L,
                backupInProgress = backupUiState.isBackupInProgress,
                restoreInProgress = backupUiState.isRestoreInProgress,
                onLocalBackup = { backupViewModel.backup() },
                onLocalRestore = { backupViewModel.restore() },
            )

            SubtleHorizontalDivider()

            ExportCsvJsonSection(
                enabled = backupUiState.isPro,
                isCsvBackupInProgress = backupUiState.isCsvBackupInProgress,
                isJsonBackupInProgress = backupUiState.isJsonBackupInProgress,
                onExportCsv = { backupViewModel.exportCsv() },
                onExportJson = { backupViewModel.exportJson() },
            )
        }
    }

    // Cloud restore picker dialog
    if (cloudUiState.showRestoreDialog) {
        CloudRestorePickerDialog(
            cloudProviderName = cloudProviderName,
            backups = cloudUiState.availableBackups,
            onDismiss = { cloudBackupViewModel.dismissRestoreDialog() },
            onBackupSelected = { cloudBackupViewModel.selectBackupToRestore(it) },
        )
    }
}
