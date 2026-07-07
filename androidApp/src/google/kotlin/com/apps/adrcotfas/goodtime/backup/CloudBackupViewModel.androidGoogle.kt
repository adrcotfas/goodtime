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

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.ui.SnackbarController
import com.apps.adrcotfas.goodtime.ui.SnackbarEvent
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_failed_please_try_again
import goodtime_productivity.composeapp.generated.resources.backup_no_backups_found
import goodtime_productivity.composeapp.generated.resources.backup_restore_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_restore_failed_please_try_again
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class CloudBackupViewModel(
    private val googleDriveBackupService: GoogleDriveBackupService,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CloudBackupUiState())
    val uiState: StateFlow<CloudBackupUiState> = _uiState.asStateFlow()

    private val _pendingAuthIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingAuthIntent: StateFlow<PendingIntent?> = _pendingAuthIntent.asStateFlow()

    init {
        checkConnectionStatus()
    }

    private fun checkConnectionStatus() {
        viewModelScope.launch {
            val token = googleDriveBackupService.getAuthTokenOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isConnected = token != null,
                )
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            when (val result = googleDriveBackupService.authorize()) {
                is GoogleDriveAuthResult.Success -> {
                    _uiState.update { it.copy(isConnected = true) }
                    executeToggleAutoBackup(true)
                }
                is GoogleDriveAuthResult.NeedsUserConsent -> {
                    _pendingAuthIntent.value = result.pendingIntent
                }
                is GoogleDriveAuthResult.Error -> {
                    SnackbarController.sendEvent(
                        SnackbarEvent(message = getString(Res.string.backup_failed_please_try_again)),
                    )
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            googleDriveBackupService.disconnect()
            googleDriveBackupService.cancelAllBackupWork()
            val currentSettings = settingsRepository.settings.first()
            settingsRepository.setBackupSettings(
                currentSettings.backupSettings.copy(cloudAutoBackupEnabled = false),
            )
            _uiState.update { it.copy(isConnected = false) }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAutoBackupToggleInProgress = true) }

            if (enabled) {
                val token = googleDriveBackupService.getAuthTokenOrNull()
                if (token == null) {
                    _uiState.update {
                        it.copy(isAutoBackupToggleInProgress = false, isConnected = false)
                    }
                    return@launch
                }
            }

            executeToggleAutoBackup(enabled)
        }
    }

    private suspend fun executeToggleAutoBackup(enabled: Boolean) {
        googleDriveBackupService.setAutoBackupEnabled(enabled)
        val currentSettings = settingsRepository.settings.first()
        settingsRepository.setBackupSettings(
            currentSettings.backupSettings.copy(cloudAutoBackupEnabled = enabled),
        )
        _uiState.update { it.copy(isAutoBackupToggleInProgress = false) }
    }

    fun backup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupInProgress = true) }

            val token = googleDriveBackupService.getAuthTokenOrNull()
            if (token == null) {
                _uiState.update { it.copy(isBackupInProgress = false, isConnected = false) }
                SnackbarController.sendEvent(
                    SnackbarEvent(message = getString(Res.string.backup_failed_please_try_again)),
                )
                return@launch
            }

            executeBackup(token)
        }
    }

    private suspend fun executeBackup(token: String) {
        val backupResult = googleDriveBackupService.backupNow(token)
        _uiState.update { it.copy(isBackupInProgress = false) }
        val message =
            if (backupResult == BackupPromptResult.SUCCESS) {
                getString(Res.string.backup_completed_successfully)
            } else {
                getString(Res.string.backup_failed_please_try_again)
            }
        SnackbarController.sendEvent(SnackbarEvent(message = message))
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }

            val token = googleDriveBackupService.getAuthTokenOrNull()
            if (token == null) {
                _uiState.update { it.copy(isRestoreInProgress = false, isConnected = false) }
                SnackbarController.sendEvent(
                    SnackbarEvent(message = getString(Res.string.backup_restore_failed_please_try_again)),
                )
                return@launch
            }

            executeRestore()
        }
    }

    private suspend fun executeRestore() {
        val backups = googleDriveBackupService.listAvailableBackups()
        _uiState.update { it.copy(isRestoreInProgress = false) }
        when {
            backups == null -> {
                SnackbarController.sendEvent(
                    SnackbarEvent(message = getString(Res.string.backup_restore_failed_please_try_again)),
                )
            }
            backups.isEmpty() -> {
                SnackbarController.sendEvent(
                    SnackbarEvent(message = getString(Res.string.backup_no_backups_found)),
                )
            }
            else -> {
                _uiState.update {
                    it.copy(
                        showRestoreDialog = true,
                        availableBackups = backups,
                    )
                }
            }
        }
    }

    fun selectBackupToRestore(fileName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showRestoreDialog = false,
                    isRestoreInProgress = true,
                )
            }
            val result = googleDriveBackupService.restoreFromBackup(fileName)
            _uiState.update { it.copy(isRestoreInProgress = false) }
            val message =
                when (result) {
                    BackupPromptResult.SUCCESS -> getString(Res.string.backup_restore_completed_successfully)
                    BackupPromptResult.NO_BACKUPS_FOUND -> getString(Res.string.backup_no_backups_found)
                    else -> getString(Res.string.backup_restore_failed_please_try_again)
                }
            SnackbarController.sendEvent(SnackbarEvent(message = message))
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false, availableBackups = emptyList()) }
    }

    fun handleAuthResult(data: Intent?) {
        viewModelScope.launch {
            val token = googleDriveBackupService.getAuthorizationResultFromIntent(data)
            _pendingAuthIntent.value = null

            if (token != null) {
                _uiState.update { it.copy(isConnected = true) }
                executeToggleAutoBackup(true)
            }
        }
    }

    fun handleAuthCancelled() {
        _pendingAuthIntent.value = null
    }
}
