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
package com.apps.adrcotfas.goodtime.data.local.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.data.settings.BackupSettings
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val restoreResult: BackupPromptResult? = null,
    val backupSettings: BackupSettings = BackupSettings(),
)

val BackupUiState.isBusy: Boolean
    get() =
        isBackupInProgress ||
            isRestoreInProgress ||
            isCsvBackupInProgress ||
            isJsonBackupInProgress

class BackupViewModel(
    private val backupManager: BackupManager,
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
                _uiState.update {
                    if (result == BackupPromptResult.CANCELLED) {
                        it.copy(isBackupInProgress = false)
                    } else {
                        it.copy(
                            isBackupInProgress = false,
                            backupResult = result,
                        )
                    }
                }
            }
        }
    }

    fun backupToCsv() {
        coroutineScope.launch {
            _uiState.update { it.copy(isCsvBackupInProgress = true) }
            backupManager.backupToCsv { result ->
                _uiState.update {
                    if (result == BackupPromptResult.CANCELLED) {
                        it.copy(isCsvBackupInProgress = false)
                    } else {
                        it.copy(
                            isCsvBackupInProgress = false,
                            backupResult = result,
                        )
                    }
                }
            }
        }
    }

    fun backupToJson() {
        coroutineScope.launch {
            _uiState.update { it.copy(isJsonBackupInProgress = true) }
            backupManager.backupToJson { result ->
                _uiState.update {
                    if (result == BackupPromptResult.CANCELLED) {
                        it.copy(isJsonBackupInProgress = false)
                    } else {
                        it.copy(
                            isJsonBackupInProgress = false,
                            backupResult = result,
                        )
                    }
                }
            }
        }
    }

    fun restore() {
        coroutineScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }
            backupManager.restore { result ->
                _uiState.update {
                    if (result == BackupPromptResult.CANCELLED) {
                        it.copy(isRestoreInProgress = false)
                    } else {
                        it.copy(
                            isRestoreInProgress = false,
                            restoreResult = result,
                        )
                    }
                }
            }
        }
    }

    fun clearBackupError() = _uiState.update { it.copy(backupResult = null) }

    fun clearRestoreError() = _uiState.update { it.copy(restoreResult = null) }

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
