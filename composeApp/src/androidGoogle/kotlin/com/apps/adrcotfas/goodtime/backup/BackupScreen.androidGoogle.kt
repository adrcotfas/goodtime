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

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.common.isUriPersisted
import com.apps.adrcotfas.goodtime.common.releasePersistableUriPermission
import com.apps.adrcotfas.goodtime.common.takePersistableUriPermission
import com.apps.adrcotfas.goodtime.data.backup.ActivityResultLauncherManager
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_actions_provider_google_drive
import goodtime_productivity.composeapp.generated.resources.backup_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_export_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_export_failed_please_try_again
import goodtime_productivity.composeapp.generated.resources.backup_failed_please_try_again
import goodtime_productivity.composeapp.generated.resources.backup_google_drive_auth_cancelled
import goodtime_productivity.composeapp.generated.resources.backup_google_drive_auth_failed
import goodtime_productivity.composeapp.generated.resources.backup_google_drive_unavailable
import goodtime_productivity.composeapp.generated.resources.backup_no_backups_found
import goodtime_productivity.composeapp.generated.resources.backup_restore_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_restore_failed_please_try_again
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun BackupScreen(
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
    onNavigateToMainAndReset: () -> Unit,
) {
    val viewModel: BackupViewModel = koinInject()
    val cloudBackupService: CloudBackupService = koinInject()
    val activityResultLauncherManager: ActivityResultLauncherManager = koinInject()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val scope = rememberCoroutineScope()

    // Google Drive specific - always available in Google Play builds
    val googleDriveBackupService = koinInject<GoogleDriveBackupService>()
    val googleDriveAuthState by googleDriveBackupService.authState.collectAsStateWithLifecycle()

    // Track pending operation to retry after auth
    var pendingCloudOperation by remember { mutableStateOf<CloudOperation?>(null) }

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
            contract =
                OpenDocumentTreeContract(),
            onResult = { uri ->
                uri?.let {
                    context.takePersistableUriPermission(uri)
                    viewModel.setBackupSettings(
                        uiState.backupSettings.copy(
                            autoBackupEnabled = true,
                            path = uri.toString(),
                        ),
                    )
                }
            },
        )

    // Google Drive authorization launcher (only for Google Play builds)
    val googleAuthLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                scope.launch {
                    val success = googleDriveBackupService.handleAuthResult(result.data)
                    if (success) {
                        viewModel.setCloudConnected(true)
                        // Retry the pending operation
                        when (pendingCloudOperation) {
                            CloudOperation.ENABLE_AUTO_BACKUP -> {
                                viewModel.toggleCloudAutoBackup(true)
                            }
                            CloudOperation.BACKUP_NOW -> {
                                viewModel.performCloudBackup()
                            }
                            CloudOperation.RESTORE -> {
                                viewModel.performCloudRestore()
                            }
                            null -> {}
                        }
                    } else {
                        // Show the specific error message from auth state if available
                        val errorMessage =
                            (googleDriveBackupService.authState.value as? GoogleDriveAuthState.Failed)?.message
                                ?: getString(Res.string.backup_google_drive_auth_failed)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    pendingCloudOperation = null
                    googleDriveBackupService.resetAuthState()
                }
            } else {
                scope.launch {
                    Toast.makeText(context, getString(Res.string.backup_google_drive_auth_cancelled), Toast.LENGTH_SHORT).show()
                }
                pendingCloudOperation = null
                googleDriveBackupService.resetAuthState()
            }
        }

    // Launch auth when needed (Google Play only)
    LaunchedEffect(googleDriveAuthState) {
        if (googleDriveAuthState is GoogleDriveAuthState.NeedsConsent) {
            googleDriveBackupService.getPendingIntentForConsent()?.let { pendingIntent ->
                googleAuthLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            }
        }
    }

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
                val isExport = uiState.backupResultKind == BackupResultKind.EXPORT
                Toast
                    .makeText(
                        context,
                        if (it == BackupPromptResult.SUCCESS) {
                            if (isExport) {
                                getString(Res.string.backup_export_completed_successfully)
                            } else {
                                getString(Res.string.backup_completed_successfully)
                            }
                        } else {
                            getString(
                                if (isExport) {
                                    Res.string.backup_export_failed_please_try_again
                                } else {
                                    Res.string.backup_failed_please_try_again
                                },
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
                val message =
                    when (it) {
                        BackupPromptResult.SUCCESS -> getString(Res.string.backup_restore_completed_successfully)
                        BackupPromptResult.NO_BACKUPS_FOUND -> getString(Res.string.backup_no_backups_found)
                        else -> getString(Res.string.backup_restore_failed_please_try_again)
                    }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            if (it == BackupPromptResult.SUCCESS) {
                onNavigateToMainAndReset()
            }
            viewModel.clearRestoreError()
        }
    }

    // Handle cloud backup issues
    LaunchedEffect(uiState.cloudIssue) {
        uiState.cloudIssue?.let { issue ->
            // AUTH_REQUIRED is handled via authState, don't show toast for it
            if (issue != CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_REQUIRED) {
                val msg =
                    when (issue) {
                        CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_FAILED ->
                            getString(Res.string.backup_google_drive_auth_failed)
                        CloudAutoBackupIssue.GOOGLE_DRIVE_UNAVAILABLE ->
                            getString(Res.string.backup_google_drive_unavailable)
                        else ->
                            getString(Res.string.backup_failed_please_try_again)
                    }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearCloudIssue()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.backupSettings.autoBackupEnabled && !context.isUriPersisted(uiState.backupSettings.path.toUri())) {
            viewModel.setBackupSettings(uiState.backupSettings.copy(autoBackupEnabled = false, path = ""))
        }
    }

    val cloudProviderName = stringResource(Res.string.backup_actions_provider_google_drive)

    BackupScreenContent(
        uiState = uiState,
        onNavigateToPro = onNavigateToPro,
        onNavigateBack = onNavigateBack,
        cloudBackupSection = {
            CloudBackupSection(
                uiState = uiState,
                enabled = uiState.isPro,
                onConnect = {
                    if (uiState.isPro && googleDriveBackupService != null) {
                        pendingCloudOperation = CloudOperation.ENABLE_AUTO_BACKUP
                        scope.launch {
                            val issue = googleDriveBackupService.reconnect()
                            if (issue == null) {
                                viewModel.setCloudConnected(true)
                                viewModel.toggleCloudAutoBackup(true)
                            }
                            // AUTH_REQUIRED will trigger consent UI via authState
                        }
                    }
                },
                onAutoBackupToggle = { isEnabled ->
                    if (uiState.isPro) {
                        if (isEnabled && googleDriveBackupService != null) {
                            pendingCloudOperation = CloudOperation.ENABLE_AUTO_BACKUP
                            scope.launch {
                                val issue = googleDriveBackupService.reconnect()
                                if (issue == null) {
                                    viewModel.toggleCloudAutoBackup(true)
                                }
                            }
                        } else {
                            viewModel.toggleCloudAutoBackup(false)
                        }
                    }
                },
                onBackup = {
                    pendingCloudOperation = CloudOperation.BACKUP_NOW
                    viewModel.performCloudBackup()
                },
                onRestore = {
                    pendingCloudOperation = CloudOperation.RESTORE
                    viewModel.performCloudRestore()
                },
                onDisconnect = {
                    googleDriveBackupService.disconnect()
                    viewModel.setCloudConnected(false)
                    if (uiState.backupSettings.cloudAutoBackupEnabled) {
                        viewModel.toggleCloudAutoBackup(false)
                    }
                },
            )
        },
        onLocalAutoBackupToggle = {
            if (uiState.isPro) {
                if (uiState.backupSettings.autoBackupEnabled) {
                    context.releasePersistableUriPermission(uiState.backupSettings.path.toUri())
                    viewModel.setBackupSettings(
                        uiState.backupSettings.copy(
                            autoBackupEnabled = false,
                            path = "",
                        ),
                    )
                } else {
                    autoExportDirLauncher.launch(Uri.EMPTY)
                }
            }
        },
        onLocalBackup = { viewModel.backup() },
        onLocalRestore = { viewModel.restore() },
        onExportCsv = { viewModel.backupToCsv() },
        onExportJson = { viewModel.backupToJson() },
        cloudProviderName = cloudProviderName,
        onCloudRestoreDismiss = { viewModel.dismissCloudRestoreDialog() },
        onCloudBackupSelected = { fileName -> viewModel.restoreSelectedCloudBackup(fileName) },
    )
}

/**
 * Tracks which cloud operation is pending auth completion.
 */
private enum class CloudOperation {
    ENABLE_AUTO_BACKUP,
    BACKUP_NOW,
    RESTORE,
}
