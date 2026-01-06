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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CloudBackupViewModel(
    private val iCloudBackupService: ICloudBackupService,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CloudBackupUiState())
    val uiState: StateFlow<CloudBackupUiState> = _uiState.asStateFlow()

    init {
        checkICloudAvailability()
    }

    private fun checkICloudAvailability() {
        viewModelScope.launch {
            val available = iCloudBackupService.isICloudAvailable()
            _uiState.update {
                it.copy(
                    isConnected = available,
                    isCloudUnavailable = !available,
                )
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAutoBackupToggleInProgress = true) }

            if (enabled) {
                val issue = iCloudBackupService.attemptEnableAutoBackup()
                if (issue != null) {
                    _uiState.update {
                        it.copy(
                            isAutoBackupToggleInProgress = false,
                            isCloudUnavailable = issue == CloudAutoBackupIssue.ICLOUD_UNAVAILABLE,
                            backupResult = BackupPromptResult.FAILED,
                        )
                    }
                } else {
                    iCloudBackupService.setAutoBackupEnabled(true)
                    val currentSettings = settingsRepository.settings.first()
                    settingsRepository.setBackupSettings(
                        currentSettings.backupSettings.copy(cloudAutoBackupEnabled = true),
                    )
                    _uiState.update { it.copy(isAutoBackupToggleInProgress = false) }
                }
            } else {
                iCloudBackupService.setAutoBackupEnabled(false)
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
            val result = iCloudBackupService.backupNow()
            _uiState.update {
                it.copy(
                    isBackupInProgress = false,
                    backupResult = result,
                )
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }
            val backups = iCloudBackupService.listAvailableBackups()
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
            val result = iCloudBackupService.restoreFromBackup(fileName)
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

    fun clearBackupResult() {
        _uiState.update { it.copy(backupResult = null) }
    }

    fun clearRestoreResult() {
        _uiState.update { it.copy(restoreResult = null) }
    }
}
