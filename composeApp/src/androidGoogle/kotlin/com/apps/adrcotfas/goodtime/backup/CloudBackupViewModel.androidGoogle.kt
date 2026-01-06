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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private enum class PendingOperation {
    CONNECT,
    TOGGLE_AUTO_BACKUP,
    BACKUP,
    RESTORE,
}

class CloudBackupViewModel(
    private val googleDriveBackupService: GoogleDriveBackupService,
    private val googleDriveAuthManager: GoogleDriveAuthManager,
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
            _uiState.update { it.copy(isConnected = token != null) }
        }
    }

    fun connect() {
        viewModelScope.launch {
            pendingOperation = PendingOperation.CONNECT
            when (val result = googleDriveAuthManager.authorize()) {
                is GoogleDriveAuthResult.Success -> {
                    _uiState.update { it.copy(isConnected = true) }
                    pendingOperation = null
                }
                is GoogleDriveAuthResult.NeedsUserConsent -> {
                    _pendingAuthIntent.value = result.pendingIntent
                }
                is GoogleDriveAuthResult.Error -> {
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
                when (val result = googleDriveAuthManager.authorize()) {
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
            when (val result = googleDriveAuthManager.authorize()) {
                is GoogleDriveAuthResult.Success -> {
                    googleDriveBackupService.backup()
                    _uiState.update {
                        it.copy(
                            isBackupInProgress = false,
                            backupResult = BackupPromptResult.SUCCESS,
                        )
                    }
                    pendingOperation = null
                }
                is GoogleDriveAuthResult.NeedsUserConsent -> {
                    _uiState.update { it.copy(isBackupInProgress = false) }
                    _pendingAuthIntent.value = result.pendingIntent
                }
                is GoogleDriveAuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isBackupInProgress = false,
                            backupResult = BackupPromptResult.FAILED,
                        )
                    }
                    pendingOperation = null
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }

            pendingOperation = PendingOperation.RESTORE
            when (val result = googleDriveAuthManager.authorize()) {
                is GoogleDriveAuthResult.Success -> {
                    val backups = googleDriveBackupService.listAvailableBackups()
                    if (backups.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isRestoreInProgress = false,
                                restoreResult = BackupPromptResult.NO_BACKUPS_FOUND,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isRestoreInProgress = false,
                                showRestoreDialog = true,
                                availableBackups = backups,
                            )
                        }
                    }
                    pendingOperation = null
                }
                is GoogleDriveAuthResult.NeedsUserConsent -> {
                    _uiState.update { it.copy(isRestoreInProgress = false) }
                    _pendingAuthIntent.value = result.pendingIntent
                }
                is GoogleDriveAuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRestoreInProgress = false,
                            restoreResult = BackupPromptResult.FAILED,
                        )
                    }
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
            _uiState.update {
                it.copy(
                    isRestoreInProgress = false,
                    restoreResult = result,
                )
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false, availableBackups = emptyList()) }
    }

    fun handleAuthResult(data: Intent?) {
        val token = googleDriveAuthManager.getAuthorizationResultFromIntent(data)
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

    fun clearBackupResult() {
        _uiState.update { it.copy(backupResult = null) }
    }

    fun clearRestoreResult() {
        _uiState.update { it.copy(restoreResult = null) }
    }
}
