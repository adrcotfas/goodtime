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

import okio.Path

enum class BackupType {
    DB,
    JSON,
    CSV,
}

enum class BackupPromptResult {
    SUCCESS,
    CANCELLED,
    FAILED,
    NO_BACKUPS_FOUND,
}

interface BackupPrompter {
    suspend fun promptUserForBackup(
        backupType: BackupType,
        fileToSharePath: Path,
        callback: suspend (BackupPromptResult) -> Unit,
    )

    suspend fun promptUserForRestore(
        importedFilePath: String,
        callback: suspend (BackupPromptResult) -> Unit,
    )
}
