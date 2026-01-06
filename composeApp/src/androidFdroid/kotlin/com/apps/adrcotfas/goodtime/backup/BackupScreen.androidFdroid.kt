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
import goodtime_productivity.composeapp.generated.resources.backup_and_restore_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun BackupScreen(
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
    onNavigateToMainAndReset: () -> Unit,
) {
    // region ViewModels and State
    val backupViewModel: BackupViewModel = koinInject()
    val activityResultLauncherManager: ActivityResultLauncherManager = koinInject()
    val context = LocalContext.current

    val backupUiState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    if (backupUiState.isLoading) return

    val backupSettings = backupUiState.backupSettings
    // endregion

    // region Launchers
    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri -> activityResultLauncherManager.importCallback(uri) },
        )
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            activityResultLauncherManager.exportCallback(result.data?.data)
        }
    val autoExportDirLauncher =
        rememberLauncherForActivityResult(
            contract = OpenDocumentTreeContract(),
            onResult = { uri ->
                uri?.let {
                    context.takePersistableUriPermission(uri)
                    backupViewModel.setBackupSettings(
                        backupSettings.copy(autoBackupEnabled = true, path = uri.toString()),
                    )
                }
            },
        )
    // endregion

    // region LaunchedEffects
    LaunchedEffect(Unit) {
        activityResultLauncherManager.setup(importLauncher, exportLauncher)
    }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED || lifecycleState == Lifecycle.State.CREATED) {
            backupViewModel.clearProgress()
        }
    }

    LaunchedEffect(Unit) {
        if (backupSettings.autoBackupEnabled && !context.isUriPersisted(backupSettings.path.toUri())) {
            backupViewModel.setBackupSettings(
                backupSettings.copy(autoBackupEnabled = false, path = ""),
            )
        }
    }
    // endregion

    // region UI
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
            UnlockFeaturesActionCard(backupUiState.isPro, onNavigateToPro = onNavigateToPro)

            // [Google only] CloudBackupSection + SubtleHorizontalDivider

            LocalBackupSection(
                enabled = backupUiState.isPro,
                localAutoBackupEnabled = backupSettings.autoBackupEnabled,
                localAutoBackupPath = backupSettings.path,
                onLocalAutoBackupToggle = {
                    if (backupSettings.autoBackupEnabled) {
                        context.releasePersistableUriPermission(backupSettings.path.toUri())
                        backupViewModel.setBackupSettings(
                            backupSettings.copy(autoBackupEnabled = false, path = ""),
                        )
                    } else {
                        autoExportDirLauncher.launch(Uri.EMPTY)
                    }
                },
                lastLocalAutoBackupTimestamp = backupSettings.localLastBackupTimestamp,
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
    // endregion

    // [Google only] CloudRestorePickerDialog
}
