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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import org.koin.compose.koinInject

@Composable
actual fun BackupScreen(
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
    onNavigateToMainAndReset: () -> Unit,
) {
    val cloudBackupViewModel: CloudBackupViewModel = koinInject()
    val cloudUiState by cloudBackupViewModel.uiState.collectAsStateWithLifecycle()
    val pendingAuthIntent by cloudBackupViewModel.pendingAuthIntent.collectAsStateWithLifecycle()

    val googleAuthLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                cloudBackupViewModel.handleAuthResult(result.data)
            } else {
                cloudBackupViewModel.handleAuthCancelled()
            }
        }

    LaunchedEffect(pendingAuthIntent) {
        pendingAuthIntent?.let { pendingIntent ->
            googleAuthLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
            )
        }
    }

    AndroidBackupScreenContent(
        onNavigateToPro = onNavigateToPro,
        onNavigateBack = onNavigateBack,
        isCloudLoading = cloudUiState.isLoading,
        cloudSection = { backupUiState ->
            CloudBackupSection(
                enabled = backupUiState.isPro,
                isConnected = cloudUiState.isConnected,
                isCloudUnavailable = cloudUiState.isCloudUnavailable,
                onConnect = { cloudBackupViewModel.connect() },
                cloudAutoBackupEnabled = backupUiState.backupSettings.cloudAutoBackupEnabled,
                onAutoBackupToggle = { cloudBackupViewModel.toggleAutoBackup(it) },
                isAutoBackupInProgress = cloudUiState.isAutoBackupToggleInProgress,
                lastBackupTimestamp = backupUiState.backupSettings.cloudLastBackupTimestamp,
                onBackup = { cloudBackupViewModel.backup() },
                isBackupInProgress = cloudUiState.isBackupInProgress,
                onRestore = { cloudBackupViewModel.restore() },
                isRestoreInProgress = cloudUiState.isRestoreInProgress,
                onDisconnect = { cloudBackupViewModel.disconnect() },
            )
            SubtleHorizontalDivider()
        },
    )

    if (cloudUiState.showRestoreDialog) {
        CloudRestorePickerDialog(
            backups = cloudUiState.availableBackups,
            onDismiss = { cloudBackupViewModel.dismissRestoreDialog() },
            onBackupSelected = { cloudBackupViewModel.selectBackupToRestore(it) },
        )
    }
}
