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

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.common.UnlockFeaturesActionCard
import com.apps.adrcotfas.goodtime.common.isUriPersisted
import com.apps.adrcotfas.goodtime.common.releasePersistableUriPermission
import com.apps.adrcotfas.goodtime.common.takePersistableUriPermission
import com.apps.adrcotfas.goodtime.data.backup.ActivityResultLauncherManager
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.TopBar
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_actions_provider_google_drive
import goodtime_productivity.composeapp.generated.resources.backup_and_restore_title
import goodtime_productivity.composeapp.generated.resources.backup_no_backups_found
import goodtime_productivity.composeapp.generated.resources.backup_restore_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_restore_failed_please_try_again
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

    val backupSettings = uiState.backupSettings

    val autoExportDirLauncher =
        rememberLauncherForActivityResult(
            contract =
                OpenDocumentTreeContract(),
            onResult = { uri ->
                uri?.let {
                    context.takePersistableUriPermission(uri)
                    viewModel.setBackupSettings(
                        backupSettings.copy(
                            autoBackupEnabled = true,
                            path = uri.toString(),
                        ),
                    )
                }
            },
        )

//    val startAuthorizationLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult(),
//        onResult = {
//            try {
//                // extract the result
//                val authorizationResult = Identity.getAuthorizationClient(requireContext())
//                    .getAuthorizationResultFromIntent(activityResult.data)
//                // continue with user action
//                saveToDriveAppFolder(authorizationResult);
//            } catch (ApiException e) {
//                // log exception
//            }
//        })

//    // Google Drive authorization launcher (only for Google Play builds)
//    val googleAuthLauncher =
//        rememberLauncherForActivityResult(
//            contract = ActivityResultContracts.StartIntentSenderForResult(),
//        ) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                scope.launch {
//                    val success = googleDriveBackupService.handleAuthResult(result.data)
//                    if (success) {
//                        viewModel.setCloudConnected(true)
//                        // Retry the pending operation
//                        when (pendingCloudOperation) {
//                            CloudOperation.ENABLE_AUTO_BACKUP -> {
//                                viewModel.toggleCloudAutoBackup(true)
//                            }
//                            CloudOperation.BACKUP_NOW -> {
//                                viewModel.performCloudBackup()
//                            }
//                            CloudOperation.RESTORE -> {
//                                viewModel.performCloudRestore()
//                            }
//                            null -> {}
//                        }
//                    } else {
//                        // Show the specific error message from auth state if available
//                        val errorMessage =
//                            (googleDriveBackupService.authState.value as? GoogleDriveAuthState.Failed)?.message
//                                ?: getString(Res.string.backup_google_drive_auth_failed)
//                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
//                    }
//                    pendingCloudOperation = null
//                    googleDriveBackupService.resetAuthState()
//                }
//            } else {
//                scope.launch {
//                    Toast.makeText(context, getString(Res.string.backup_google_drive_auth_cancelled), Toast.LENGTH_SHORT).show()
//                }
//                pendingCloudOperation = null
//                googleDriveBackupService.resetAuthState()
//            }
//        }
//
//    // Launch auth when needed (Google Play only)
//    LaunchedEffect(googleDriveAuthState) {
//        if (googleDriveAuthState is GoogleDriveAuthState.NeedsConsent) {
//            googleDriveBackupService.getPendingIntentForConsent()?.let { pendingIntent ->
//                googleAuthLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
//            }
//        }
//    }

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

    LaunchedEffect(Unit) {
        if (backupSettings.autoBackupEnabled && !context.isUriPersisted(backupSettings.path.toUri())) {
            viewModel.setBackupSettings(
                backupSettings.copy(
                    autoBackupEnabled = false,
                    path = "",
                ),
            )
        }
    }

    val cloudProviderName = stringResource(Res.string.backup_actions_provider_google_drive)

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
                uiState.isPro,
                onNavigateToPro = onNavigateToPro,
            )
            CloudBackupSection()
            SubtleHorizontalDivider()
            LocalBackupSection(
                uiState.isPro,
                localAutoBackupEnabled = backupSettings.autoBackupEnabled,
                localAutoBackupPath = backupSettings.path,
                onLocalAutoBackupToggle = {
                    if (backupSettings.autoBackupEnabled) {
                        context.releasePersistableUriPermission(backupSettings.path.toUri())
                        viewModel.setBackupSettings(
                            backupSettings.copy(
                                autoBackupEnabled = false,
                                path = "",
                            ),
                        )
                    } else {
                        autoExportDirLauncher.launch(Uri.EMPTY)
                    }
                },
                lastLocalAutoBackupTimestamp = backupSettings.localLastBackupTimestamp,
                backupInProgress = uiState.isBackupInProgress,
                restoreInProgress = uiState.isRestoreInProgress,
                onLocalBackup = {
                    viewModel.backup()
                },
                onLocalRestore = {
                    viewModel.restore()
                },
            )
            SubtleHorizontalDivider()
            ExportCsvJsonSection(
                uiState.isPro,
                isCsvBackupInProgress = uiState.isCsvBackupInProgress,
                isJsonBackupInProgress = uiState.isJsonBackupInProgress,
                onExportCsv = {
                    viewModel.exportCsv()
                },
                onExportJson = {
                    viewModel.exportJson()
                },
            )
        }
    }
}
