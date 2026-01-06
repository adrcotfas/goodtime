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
import com.apps.adrcotfas.goodtime.ui.SnackbarController
import com.apps.adrcotfas.goodtime.ui.SnackbarEvent
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_failed_please_try_again
import goodtime_productivity.composeapp.generated.resources.backup_no_backups_found
import goodtime_productivity.composeapp.generated.resources.backup_restore_completed_successfully
import goodtime_productivity.composeapp.generated.resources.backup_restore_failed_please_try_again
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

data class BackupUiState(
    val isLoading: Boolean = true,
    val isPro: Boolean = false,
    val isBackupInProgress: Boolean = false,
    val isCsvBackupInProgress: Boolean = false,
    val isJsonBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val backupSettings: BackupSettings = BackupSettings(),
)

class BackupViewModel(
    private val backupManager: BackupFileManager,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope,
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
                        )
                    }
                }
        }
    }

    fun backup() {
        coroutineScope.launch {
            _uiState.update { it.copy(isBackupInProgress = true) }
            backupManager.backup { result ->
                _uiState.update { it.copy(isBackupInProgress = false) }
                if (result != BackupPromptResult.CANCELLED) {
                    coroutineScope.launch {
                        val message =
                            if (result == BackupPromptResult.SUCCESS) {
                                getString(Res.string.backup_completed_successfully)
                            } else {
                                getString(Res.string.backup_failed_please_try_again)
                            }
                        SnackbarController.sendEvent(SnackbarEvent(message = message))
                    }
                }
            }
        }
    }

    fun exportCsv() {
        coroutineScope.launch {
            _uiState.update { it.copy(isCsvBackupInProgress = true) }
            backupManager.exportCsv { result ->
                _uiState.update { it.copy(isCsvBackupInProgress = false) }
                if (result != BackupPromptResult.CANCELLED) {
                    coroutineScope.launch {
                        val message =
                            if (result == BackupPromptResult.SUCCESS) {
                                getString(Res.string.backup_completed_successfully)
                            } else {
                                getString(Res.string.backup_failed_please_try_again)
                            }
                        SnackbarController.sendEvent(SnackbarEvent(message = message))
                    }
                }
            }
        }
    }

    fun exportJson() {
        coroutineScope.launch {
            _uiState.update { it.copy(isJsonBackupInProgress = true) }
            backupManager.exportJson { result ->
                _uiState.update { it.copy(isJsonBackupInProgress = false) }
                if (result != BackupPromptResult.CANCELLED) {
                    coroutineScope.launch {
                        val message =
                            if (result == BackupPromptResult.SUCCESS) {
                                getString(Res.string.backup_completed_successfully)
                            } else {
                                getString(Res.string.backup_failed_please_try_again)
                            }
                        SnackbarController.sendEvent(SnackbarEvent(message = message))
                    }
                }
            }
        }
    }

    fun restore() {
        coroutineScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }
            backupManager.restore { result ->
                _uiState.update { it.copy(isRestoreInProgress = false) }
                if (result != BackupPromptResult.CANCELLED) {
                    coroutineScope.launch {
                        val message =
                            when (result) {
                                BackupPromptResult.SUCCESS -> getString(Res.string.backup_restore_completed_successfully)
                                BackupPromptResult.NO_BACKUPS_FOUND -> getString(Res.string.backup_no_backups_found)
                                else -> getString(Res.string.backup_restore_failed_please_try_again)
                            }
                        SnackbarController.sendEvent(SnackbarEvent(message = message))
                    }
                }
            }
        }
    }

    fun clearProgress() =
        _uiState.update {
            it.copy(
                isBackupInProgress = false,
                isRestoreInProgress = false,
                isCsvBackupInProgress = false,
                isJsonBackupInProgress = false,
            )
        }

    fun setBackupSettings(settings: BackupSettings) {
        coroutineScope.launch {
            settingsRepository.setBackupSettings(settings)
        }
    }
}
