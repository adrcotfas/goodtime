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

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.common.isUriPersisted
import com.apps.adrcotfas.goodtime.common.releasePersistableUriPermission
import com.apps.adrcotfas.goodtime.common.takePersistableUriPermission
import com.apps.adrcotfas.goodtime.data.backup.ActivityResultLauncherManager
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPromptResult
import com.apps.adrcotfas.goodtime.data.local.backup.BackupViewModel
import com.apps.adrcotfas.goodtime.data.settings.BackupSettings
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_failed_please_try_again
import goodtime_productivity.composeapp.generated.resources.backup_restore_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_restore_failed_please_try_again
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun BackupScreen(
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
    onNavigateToMainAndReset: () -> Unit,
) {
    val viewModel: BackupViewModel = koinInject()
    val activityResultLauncherManager: ActivityResultLauncherManager = koinInject()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    if (uiState.isLoading) return

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri ->
                activityResultLauncherManager.importCallback(uri)
            },
        )
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val data = result.data
            val uri = data?.data
            activityResultLauncherManager.exportCallback(uri)
        }

    val autoExportDirLauncher =
        rememberLauncherForActivityResult(
            contract = OpenDocumentTreeContract(),
            onResult = { uri ->
                uri?.let {
                    context.takePersistableUriPermission(uri)
                    viewModel.setBackupSettings(
                        BackupSettings(
                            autoBackupEnabled = true,
                            path = uri.toString(),
                        ),
                    )
                }
            },
        )

    LaunchedEffect(Unit) {
        activityResultLauncherManager.setup(importLauncher, exportLauncher)
    }

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED, Lifecycle.State.CREATED -> {
                viewModel.clearProgress()
            }

            else -> {
                // do nothing
            }
        }
    }

    LaunchedEffect(uiState.backupResult) {
        uiState.backupResult?.let {
            if (it != BackupPromptResult.CANCELLED) {
                Toast
                    .makeText(
                        context,
                        if (it == BackupPromptResult.SUCCESS) {
                            getString(Res.string.backup_completed_successfully)
                        } else {
                            getString(
                                Res.string.backup_failed_please_try_again,
                            )
                        },
                        Toast.LENGTH_SHORT,
                    ).show()
            }
            viewModel.clearBackupError()
        }
    }

    LaunchedEffect(uiState.restoreResult) {
        uiState.restoreResult?.let {
            if (it != BackupPromptResult.CANCELLED) {
                Toast
                    .makeText(
                        context,
                        if (it == BackupPromptResult.SUCCESS) {
                            getString(Res.string.backup_restore_completed_successfully)
                        } else {
                            getString(
                                Res.string.backup_restore_failed_please_try_again,
                            )
                        },
                        Toast.LENGTH_SHORT,
                    ).show()
            }
            if (it == BackupPromptResult.SUCCESS) {
                onNavigateToMainAndReset()
            }
            viewModel.clearRestoreError()
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.backupSettings.autoBackupEnabled && !context.isUriPersisted(uiState.backupSettings.path.toUri())) {
            viewModel.setBackupSettings(BackupSettings())
        }
    }

    BackupScreenContent(
        uiState = uiState,
        onNavigateToPro = onNavigateToPro,
        onNavigateBack = onNavigateBack,
        onAutoBackupToggle = {
            if (uiState.isPro) {
                if (uiState.backupSettings.autoBackupEnabled) {
                    context.releasePersistableUriPermission(uiState.backupSettings.path.toUri())
                    viewModel.setBackupSettings(BackupSettings())
                } else {
                    autoExportDirLauncher.launch(Uri.EMPTY)
                }
            }
        },
        onBackup = { viewModel.backup() },
        onRestore = { viewModel.restore() },
        onBackupToCsv = { viewModel.backupToCsv() },
        onBackupToJson = { viewModel.backupToJson() },
        onToggleExportSection = { viewModel.toggleExportSection() },
    )
}
