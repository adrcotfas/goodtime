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
import com.apps.adrcotfas.goodtime.data.settings.BackupSettings
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.platform.getPlatformConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val isLoading: Boolean = true,
    val isPro: Boolean = false,
    val isBackupInProgress: Boolean = false,
    val isCsvBackupInProgress: Boolean = false,
    val isJsonBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val backupResult: BackupPromptResult? = null,
    val backupResultKind: BackupResultKind = BackupResultKind.BACKUP,
    val restoreResult: BackupPromptResult? = null,
    val backupSettings: BackupSettings = BackupSettings(),
    // Cloud backup state
    val isCloudConnected: Boolean = false,
    val isCloudBackupInProgress: Boolean = false,
    val isCloudRestoreInProgress: Boolean = false,
    val isCloudAutoBackupToggleInProgress: Boolean = false,
    val cloudIssue: CloudAutoBackupIssue? = null,
    // Cloud restore picker
    val showCloudRestoreDialog: Boolean = false,
    val availableCloudBackups: List<String> = emptyList(),
    // Platform-specific UI visibility
    val showLocalAutoBackup: Boolean = false,
    val showCloudBackup: Boolean = true,
)

enum class BackupResultKind {
    BACKUP,
    EXPORT,
}

enum class CloudAutoBackupIssue {
    ICLOUD_UNAVAILABLE,
    ICLOUD_FULL,
    GOOGLE_DRIVE_AUTH_REQUIRED,
    GOOGLE_DRIVE_AUTH_FAILED,
    GOOGLE_DRIVE_UNAVAILABLE,
    UNKNOWN,
}

val BackupUiState.isBusy: Boolean
    get() =
        isBackupInProgress ||
            isRestoreInProgress ||
            isCsvBackupInProgress ||
            isJsonBackupInProgress ||
            isCloudBackupInProgress ||
            isCloudRestoreInProgress ||
            isCloudAutoBackupToggleInProgress

class BackupViewModel(
    private val localBackupService: LocalBackupService,
    private val cloudBackupService: CloudBackupService?,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState =
        _uiState
            .onStart { loadData() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupUiState())

    private fun loadData() {
        viewModelScope.launch {
            settingsRepository.settings
                .distinctUntilChanged { old, new ->
                    old.isPro == new.isPro && old.backupSettings == new.backupSettings
                }.collect { settings ->
                    var backupSettings = settings.backupSettings

                    // If cloud auto-backup is enabled, verify cloud is still available
                    if (backupSettings.cloudAutoBackupEnabled && cloudBackupService != null) {
                        val preflight = cloudBackupService.preflightBackup()
                        if (preflight != null) {
                            // Cloud is unavailable - disable auto-backup
                            backupSettings = backupSettings.copy(cloudAutoBackupEnabled = false)
                            settingsRepository.setBackupSettings(backupSettings)
                            cloudBackupService.setAutoBackupEnabled(false)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPro = settings.isPro,
                            backupSettings = backupSettings,
                            showLocalAutoBackup = getPlatformConfiguration().isAndroid,
                            showCloudBackup = cloudBackupService != null,
                        )
                    }
                }
        }
    }

    fun backup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupInProgress = true) }
            val result = localBackupService.backup()
            _uiState.update {
                if (result == BackupPromptResult.CANCELLED) {
                    it.copy(isBackupInProgress = false)
                } else {
                    it.copy(isBackupInProgress = false, backupResult = result, backupResultKind = BackupResultKind.BACKUP)
                }
            }
        }
    }

    fun backupToCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCsvBackupInProgress = true) }
            val result = localBackupService.exportCsv()
            _uiState.update {
                if (result == BackupPromptResult.CANCELLED) {
                    it.copy(isCsvBackupInProgress = false)
                } else {
                    it.copy(isCsvBackupInProgress = false, backupResult = result, backupResultKind = BackupResultKind.EXPORT)
                }
            }
        }
    }

    fun backupToJson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isJsonBackupInProgress = true) }
            val result = localBackupService.exportJson()
            _uiState.update {
                if (result == BackupPromptResult.CANCELLED) {
                    it.copy(isJsonBackupInProgress = false)
                } else {
                    it.copy(isJsonBackupInProgress = false, backupResult = result, backupResultKind = BackupResultKind.EXPORT)
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }
            val result = localBackupService.restore()
            _uiState.update {
                if (result == BackupPromptResult.CANCELLED) {
                    it.copy(isRestoreInProgress = false)
                } else {
                    it.copy(isRestoreInProgress = false, restoreResult = result)
                }
            }
        }
    }

    fun clearBackupError() = _uiState.update { it.copy(backupResult = null, backupResultKind = BackupResultKind.BACKUP) }

    fun clearRestoreError() = _uiState.update { it.copy(restoreResult = null) }

    fun clearCloudIssue() = _uiState.update { it.copy(cloudIssue = null) }

    fun setCloudConnected(connected: Boolean) {
        _uiState.update { it.copy(isCloudConnected = connected) }
    }

    fun clearProgress() =
        _uiState.update {
            it.copy(
                isBackupInProgress = false,
                isRestoreInProgress = false,
                isCsvBackupInProgress = false,
                isJsonBackupInProgress = false,
                isCloudBackupInProgress = false,
                isCloudRestoreInProgress = false,
                isCloudAutoBackupToggleInProgress = false,
            )
        }

    fun setBackupSettings(settings: BackupSettings) {
        viewModelScope.launch {
            settingsRepository.setBackupSettings(settings)
        }
    }

    /**
     * Enable cloud auto backup when cloud is available and an initial backup succeeds.
     * When enabling fails, we keep the switch OFF and expose [cloudIssue] for the UI.
     */
    fun toggleCloudAutoBackup(enabled: Boolean) {
        val service = cloudBackupService ?: return
        viewModelScope.launch {
            val before = settingsRepository.settings.first().backupSettings

            if (!enabled) {
                settingsRepository.setBackupSettings(before.copy(cloudAutoBackupEnabled = false))
                service.setAutoBackupEnabled(false)
                return@launch
            }

            _uiState.update { it.copy(isCloudAutoBackupToggleInProgress = true) }
            val issue = service.attemptEnableAutoBackup()

            val after = settingsRepository.settings.first().backupSettings
            if (issue == null) {
                settingsRepository.setBackupSettings(after.copy(cloudAutoBackupEnabled = true))
                service.setAutoBackupEnabled(true)
            } else {
                settingsRepository.setBackupSettings(after.copy(cloudAutoBackupEnabled = false))
                _uiState.update { it.copy(cloudIssue = issue) }
                service.setAutoBackupEnabled(false)
            }

            _uiState.update { it.copy(isCloudAutoBackupToggleInProgress = false) }
        }
    }

    fun performCloudBackup() {
        val service = cloudBackupService ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudBackupInProgress = true) }
            val preflight = service.preflightBackup()
            if (preflight != null) {
                _uiState.update {
                    it.copy(
                        isCloudBackupInProgress = false,
                        cloudIssue = preflight,
                    )
                }
                return@launch
            }

            val result = service.backupNow()
            _uiState.update { it.copy(isCloudBackupInProgress = false, backupResult = result) }
        }
    }

    fun performCloudRestore() {
        val service = cloudBackupService ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudRestoreInProgress = true) }

            // Check if cloud is available first
            val preflight = service.preflightBackup()
            if (preflight != null) {
                _uiState.update {
                    it.copy(
                        isCloudRestoreInProgress = false,
                        cloudIssue = preflight,
                    )
                }
                return@launch
            }

            // Fetch available backups
            val backups = service.listAvailableBackups()

            if (backups.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isCloudRestoreInProgress = false,
                        restoreResult = BackupPromptResult.NO_BACKUPS_FOUND,
                    )
                }
                return@launch
            }

            // Show picker dialog
            _uiState.update {
                it.copy(
                    isCloudRestoreInProgress = false,
                    showCloudRestoreDialog = true,
                    availableCloudBackups = backups,
                )
            }
        }
    }

    fun dismissCloudRestoreDialog() {
        _uiState.update {
            it.copy(
                showCloudRestoreDialog = false,
                availableCloudBackups = emptyList(),
            )
        }
    }

    fun restoreSelectedCloudBackup(fileName: String) {
        val service = cloudBackupService ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showCloudRestoreDialog = false,
                    isCloudRestoreInProgress = true,
                )
            }

            val result = service.restoreFromBackup(fileName)
            _uiState.update {
                it.copy(
                    isCloudRestoreInProgress = false,
                    restoreResult = result,
                    availableCloudBackups = emptyList(),
                )
            }
        }
    }
}
