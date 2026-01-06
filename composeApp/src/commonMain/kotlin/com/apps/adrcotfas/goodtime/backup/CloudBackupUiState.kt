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

data class CloudBackupUiState(
    val isConnected: Boolean = false,
    val isCloudUnavailable: Boolean = false,
    val isAutoBackupToggleInProgress: Boolean = false,
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val showRestoreDialog: Boolean = false,
    val availableBackups: List<String> = emptyList(),
    val backupResult: BackupPromptResult? = null,
    val restoreResult: BackupPromptResult? = null,
)
