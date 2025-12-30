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

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.backup.BackupPromptResult
import com.apps.adrcotfas.goodtime.backup.BackupResultKind
import com.apps.adrcotfas.goodtime.backup.BackupViewModel
import com.apps.adrcotfas.goodtime.backup.CloudAutoBackupIssue
import com.apps.adrcotfas.goodtime.ui.SnackbarController
import com.apps.adrcotfas.goodtime.ui.SnackbarEvent
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_export_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_export_failed_please_try_again
import goodtime_productivity.composeapp.generated.resources.backup_failed_please_try_again
import goodtime_productivity.composeapp.generated.resources.backup_icloud_full
import goodtime_productivity.composeapp.generated.resources.backup_icloud_unavailable
import goodtime_productivity.composeapp.generated.resources.backup_icloud_unknown_error
import goodtime_productivity.composeapp.generated.resources.backup_no_backups_found
import goodtime_productivity.composeapp.generated.resources.backup_restore_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_restore_failed_please_try_again
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun BackupScreen(
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
    onNavigateToMainAndReset: () -> Unit,
) {
    val viewModel: BackupViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) return

    LaunchedEffect(uiState.backupResult) {
        uiState.backupResult?.let {
            if (it != BackupPromptResult.CANCELLED) {
                val isExport = uiState.backupResultKind == BackupResultKind.EXPORT
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message =
                            if (it == BackupPromptResult.SUCCESS) {
                                if (isExport) {
                                    getString(Res.string.backup_export_completed_successfully)
                                } else {
                                    getString(Res.string.backup_completed_successfully)
                                }
                            } else {
                                if (isExport) {
                                    getString(Res.string.backup_export_failed_please_try_again)
                                } else {
                                    getString(Res.string.backup_failed_please_try_again)
                                }
                            },
                    ),
                )
            }
            viewModel.clearBackupError()
        }
    }

    LaunchedEffect(uiState.restoreResult) {
        uiState.restoreResult?.let {
            if (it != BackupPromptResult.CANCELLED) {
                val message = when (it) {
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
            viewModel.clearRestoreError()
        }
    }

    LaunchedEffect(uiState.cloudIssue) {
        uiState.cloudIssue?.let { issue ->
            val msg =
                when (issue) {
                    CloudAutoBackupIssue.ICLOUD_UNAVAILABLE -> getString(Res.string.backup_icloud_unavailable)
                    CloudAutoBackupIssue.ICLOUD_FULL -> getString(Res.string.backup_icloud_full)
                    CloudAutoBackupIssue.UNKNOWN -> getString(Res.string.backup_icloud_unknown_error)
                }
            SnackbarController.sendEvent(SnackbarEvent(message = msg, duration = SnackbarDuration.Short))
            viewModel.clearCloudIssue()
        }
    }

    BackupScreenContent(
        uiState = uiState,
        onNavigateToPro = onNavigateToPro,
        onNavigateBack = onNavigateBack,
        onAutoBackupToggle = { isEnabled ->
            if (uiState.isPro) {
                // iOS: only turn ON the switch after confirming iCloud is available and an initial backup succeeds.
                viewModel.toggleCloudAutoBackup(isEnabled)
            }
        },
        onCloudBackup = { viewModel.performCloudBackup() },
        onCloudRestore = { viewModel.performCloudRestore() },
        onCloudRestoreDismiss = { viewModel.dismissCloudRestoreDialog() },
        onCloudBackupSelected = { fileName -> viewModel.restoreSelectedCloudBackup(fileName) },
        onLocalBackup = { viewModel.backup() },
        onLocalRestore = { viewModel.restore() },
        onExportCsv = { viewModel.backupToCsv() },
        onExportJson = { viewModel.backupToJson() },
        onToggleExportSection = { viewModel.toggleExportSection() },
    )
}
