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

private enum class PendingOperation {
    CONNECT,
    TOGGLE_AUTO_BACKUP,
    BACKUP,
    RESTORE,
}

class CloudBackupViewModel(
    private val googleDriveBackupService: GoogleDriveBackupService,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CloudBackupUiState())
    val uiState: StateFlow<CloudBackupUiState> = _uiState.asStateFlow()

    private val _pendingAuthIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingAuthIntent: StateFlow<PendingIntent?> = _pendingAuthIntent.asStateFlow()

    private var pendingOperation: PendingOperation? = null

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
            pendingOperation = PendingOperation.CONNECT
            when (val result = googleDriveBackupService.authorize()) {
                is GoogleDriveAuthResult.Success -> {
                    _uiState.update { it.copy(isConnected = true) }
                    pendingOperation = null
                }
                is GoogleDriveAuthResult.NeedsUserConsent -> {
                    _pendingAuthIntent.value = result.pendingIntent
                }
                is GoogleDriveAuthResult.Error -> {
                    SnackbarController.sendEvent(
                        SnackbarEvent(message = getString(Res.string.backup_failed_please_try_again)),
                    )
                    pendingOperation = null
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
                pendingOperation = PendingOperation.TOGGLE_AUTO_BACKUP
                when (val result = googleDriveBackupService.authorize()) {
                    is GoogleDriveAuthResult.Success -> {
                        googleDriveBackupService.setAutoBackupEnabled(true)
                        val currentSettings = settingsRepository.settings.first()
                        settingsRepository.setBackupSettings(
                            currentSettings.backupSettings.copy(cloudAutoBackupEnabled = true),
                        )
                        _uiState.update { it.copy(isAutoBackupToggleInProgress = false) }
                        pendingOperation = null
                    }
                    is GoogleDriveAuthResult.NeedsUserConsent -> {
                        _uiState.update { it.copy(isAutoBackupToggleInProgress = false) }
                        _pendingAuthIntent.value = result.pendingIntent
                    }
                    is GoogleDriveAuthResult.Error -> {
                        _uiState.update { it.copy(isAutoBackupToggleInProgress = false) }
                        pendingOperation = null
                    }
                }
            } else {
                googleDriveBackupService.setAutoBackupEnabled(false)
                val currentSettings = settingsRepository.settings.first()
                settingsRepository.setBackupSettings(
                    currentSettings.backupSettings.copy(cloudAutoBackupEnabled = false),
                )
                _uiState.update { it.copy(isAutoBackupToggleInProgress = false) }
            }
        }
    }

    fun backup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupInProgress = true) }

            pendingOperation = PendingOperation.BACKUP
            when (val result = googleDriveBackupService.authorize()) {
                is GoogleDriveAuthResult.Success -> {
                    val token = result.authResult.accessToken
                    if (token != null) {
                        val backupResult = googleDriveBackupService.backupNow(token)
                        _uiState.update { it.copy(isBackupInProgress = false) }
                        val message =
                            if (backupResult == BackupPromptResult.SUCCESS) {
                                getString(Res.string.backup_completed_successfully)
                            } else {
                                getString(Res.string.backup_failed_please_try_again)
                            }
                        SnackbarController.sendEvent(SnackbarEvent(message = message))
                    } else {
                        _uiState.update { it.copy(isBackupInProgress = false) }
                        SnackbarController.sendEvent(
                            SnackbarEvent(message = getString(Res.string.backup_failed_please_try_again)),
                        )
                    }
                    pendingOperation = null
                }
                is GoogleDriveAuthResult.NeedsUserConsent -> {
                    _uiState.update { it.copy(isBackupInProgress = false) }
                    _pendingAuthIntent.value = result.pendingIntent
                }
                is GoogleDriveAuthResult.Error -> {
                    _uiState.update { it.copy(isBackupInProgress = false) }
                    SnackbarController.sendEvent(
                        SnackbarEvent(message = getString(Res.string.backup_failed_please_try_again)),
                    )
                    pendingOperation = null
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }

            pendingOperation = PendingOperation.RESTORE
            when (val result = googleDriveBackupService.authorize()) {
                is GoogleDriveAuthResult.Success -> {
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
                    pendingOperation = null
                }
                is GoogleDriveAuthResult.NeedsUserConsent -> {
                    _uiState.update { it.copy(isRestoreInProgress = false) }
                    _pendingAuthIntent.value = result.pendingIntent
                }
                is GoogleDriveAuthResult.Error -> {
                    _uiState.update { it.copy(isRestoreInProgress = false) }
                    SnackbarController.sendEvent(
                        SnackbarEvent(message = getString(Res.string.backup_restore_failed_please_try_again)),
                    )
                    pendingOperation = null
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
        val token = googleDriveBackupService.getAuthorizationResultFromIntent(data)
        _pendingAuthIntent.value = null

        if (token != null) {
            _uiState.update { it.copy(isConnected = true) }
            when (pendingOperation) {
                PendingOperation.CONNECT -> { /* Already connected */ }
                PendingOperation.TOGGLE_AUTO_BACKUP -> toggleAutoBackup(true)
                PendingOperation.BACKUP -> backup()
                PendingOperation.RESTORE -> restore()
                null -> { }
            }
        }
        pendingOperation = null
    }

    fun handleAuthCancelled() {
        _pendingAuthIntent.value = null
        pendingOperation = null
    }
}
