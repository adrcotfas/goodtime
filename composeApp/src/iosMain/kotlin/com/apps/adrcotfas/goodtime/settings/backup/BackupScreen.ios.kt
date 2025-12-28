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
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPromptResult
import com.apps.adrcotfas.goodtime.data.local.backup.BackupViewModel
import com.apps.adrcotfas.goodtime.data.settings.BackupSettings
import com.apps.adrcotfas.goodtime.ui.SnackbarController
import com.apps.adrcotfas.goodtime.ui.SnackbarEvent
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_failed_please_try_again
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
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message =
                            if (it == BackupPromptResult.SUCCESS) {
                                getString(Res.string.backup_completed_successfully)
                            } else {
                                getString(Res.string.backup_failed_please_try_again)
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
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message =
                            if (it == BackupPromptResult.SUCCESS) {
                                getString(Res.string.backup_restore_completed_successfully)
                            } else {
                                getString(Res.string.backup_restore_failed_please_try_again)
                            },
                        duration = SnackbarDuration.Short,
                    ),
                )
            }
            if (it == BackupPromptResult.SUCCESS) {
                onNavigateToMainAndReset()
            }
            viewModel.clearRestoreError()
        }
    }

    BackupScreenContent(
        uiState = uiState,
        onNavigateToPro = onNavigateToPro,
        onNavigateBack = onNavigateBack,
        onAutoBackupToggle = { isEnabled ->
            if (uiState.isPro) {
                // TODO: Implement iCloud backup for iOS
                // For iOS, auto backup should use iCloud Drive for seamless sync
                // - Use FileManager to save backups to iCloud container
                // - Access via FileManager.default.url(forUbiquityContainerIdentifier:)
                // - Enable iCloud capability in Xcode project settings
                viewModel.setBackupSettings(
                    BackupSettings(
                        autoBackupEnabled = isEnabled,
                        path = "icloud", // Placeholder for iCloud path
                    ),
                )
            }
        },
        onBackup = { viewModel.backup() },
        onRestore = { viewModel.restore() },
        onBackupToCsv = { viewModel.backupToCsv() },
        onBackupToJson = { viewModel.backupToJson() },
        onToggleExportSection = { viewModel.toggleExportSection() },
    )
}
