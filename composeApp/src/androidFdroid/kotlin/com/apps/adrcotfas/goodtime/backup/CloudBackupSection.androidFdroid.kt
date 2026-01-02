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
package com.apps.adrcotfas.goodtime.settings.backup

import androidx.compose.runtime.Composable
import com.apps.adrcotfas.goodtime.backup.BackupUiState

/**
 * Cloud backup section for F-Droid builds.
 * Empty implementation - cloud backup is not available on F-Droid
 * as it requires Google Play Services.
 */
@Composable
fun CloudBackupSection(
    uiState: BackupUiState,
    enabled: Boolean,
    onConnect: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDisconnect: () -> Unit,
) {
    // No cloud backup on F-Droid
}
