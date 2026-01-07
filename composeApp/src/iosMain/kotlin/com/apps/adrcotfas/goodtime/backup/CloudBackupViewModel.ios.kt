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
                    isLoading = false,
                    isConnected = available,
                    isCloudUnavailable = !available,
                )
            }
        }
    }

    private suspend fun checkAvailabilityOrFail(errorMessage: String): Boolean {
        if (!iCloudBackupService.isICloudAvailable()) {
            _uiState.update {
                it.copy(isCloudUnavailable = true, isConnected = false)
            }
            SnackbarController.sendEvent(SnackbarEvent(message = errorMessage))
            return false
        }
        return true
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
                        )
                    }
                    SnackbarController.sendEvent(
                        SnackbarEvent(message = getString(Res.string.backup_failed_please_try_again)),
                    )
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

            if (!checkAvailabilityOrFail(getString(Res.string.backup_failed_please_try_again))) {
                _uiState.update { it.copy(isBackupInProgress = false) }
                return@launch
            }

            val result = iCloudBackupService.backupNow()
            _uiState.update { it.copy(isBackupInProgress = false) }
            val message =
                if (result == BackupPromptResult.SUCCESS) {
                    getString(Res.string.backup_completed_successfully)
                } else {
                    getString(Res.string.backup_failed_please_try_again)
                }
            SnackbarController.sendEvent(SnackbarEvent(message = message))
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }

            if (!checkAvailabilityOrFail(getString(Res.string.backup_restore_failed_please_try_again))) {
                _uiState.update { it.copy(isRestoreInProgress = false) }
                return@launch
            }

            val backups = iCloudBackupService.listAvailableBackups()
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
}
