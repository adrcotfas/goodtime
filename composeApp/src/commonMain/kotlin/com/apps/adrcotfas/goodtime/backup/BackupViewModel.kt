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
    val cloudProvider: CloudProvider = CloudProvider.GOOGLE_DRIVE,
    val cloudAccountEmail: String? = null,
    val isCloudBackupInProgress: Boolean = false,
    val isCloudRestoreInProgress: Boolean = false,
    val isCloudAutoBackupToggleInProgress: Boolean = false,
    val cloudIssue: CloudAutoBackupIssue? = null,
    // Cloud restore picker
    val showCloudRestoreDialog: Boolean = false,
    val availableCloudBackups: List<String> = emptyList(),
    // UI state
    val showExportSection: Boolean = false,
)

enum class BackupResultKind {
    BACKUP,
    EXPORT,
}

enum class CloudProvider {
    GOOGLE_DRIVE,
    ICLOUD,
}

enum class CloudAutoBackupIssue {
    ICLOUD_UNAVAILABLE,
    ICLOUD_FULL,
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
    private val cloudBackupService: CloudBackupService,
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPro = settings.isPro,
                            backupSettings = settings.backupSettings,
                            cloudProvider = getPlatformConfiguration().cloudProvider,
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
     * iOS: only enable cloud auto backup when iCloud is available and an initial backup succeeds.
     * When enabling fails, we keep the switch OFF and expose [cloudIssue] for the UI.
     */
    fun toggleCloudAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            val before = settingsRepository.settings.first().backupSettings

            if (!enabled) {
                settingsRepository.setBackupSettings(before.copy(cloudAutoBackupEnabled = false))
                cloudBackupService.setAutoBackupEnabled(false)
                return@launch
            }

            _uiState.update { it.copy(isCloudAutoBackupToggleInProgress = true) }
            val issue = cloudBackupService.attemptEnableAutoBackup()

            val after = settingsRepository.settings.first().backupSettings
            if (issue == null) {
                settingsRepository.setBackupSettings(after.copy(cloudAutoBackupEnabled = true))
                cloudBackupService.setAutoBackupEnabled(true)
            } else {
                settingsRepository.setBackupSettings(after.copy(cloudAutoBackupEnabled = false))
                _uiState.update { it.copy(cloudIssue = issue) }
                cloudBackupService.setAutoBackupEnabled(false)
            }

            _uiState.update { it.copy(isCloudAutoBackupToggleInProgress = false) }
        }
    }

    fun toggleExportSection() {
        _uiState.update { it.copy(showExportSection = !it.showExportSection) }
    }

    fun performCloudBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudBackupInProgress = true) }
            val preflight = cloudBackupService.preflightBackup()
            if (preflight != null) {
                _uiState.update {
                    it.copy(
                        isCloudBackupInProgress = false,
                        cloudIssue = preflight,
                    )
                }
                return@launch
            }

            val result = cloudBackupService.backupNow()
            _uiState.update { it.copy(isCloudBackupInProgress = false, backupResult = result) }
        }
    }

    fun performCloudRestore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudRestoreInProgress = true) }

            // Fetch available backups
            val backups = cloudBackupService.listAvailableBackups()

            if (backups.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isCloudRestoreInProgress = false,
                        restoreResult = BackupPromptResult.FAILED,
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
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showCloudRestoreDialog = false,
                    isCloudRestoreInProgress = true,
                )
            }

            val result = cloudBackupService.restoreFromBackup(fileName)
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
