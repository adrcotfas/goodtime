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
package com.apps.adrcotfas.goodtime.data.backup

import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPromptResult
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.data.local.backup.BackupType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.Path
import platform.Foundation.NSUUID

class IosBackupPrompter(
    private val logger: Logger,
    private val mainScope: CoroutineScope,
) : BackupPrompter {
    override suspend fun promptUserForBackup(
        backupType: BackupType,
        fileToSharePath: Path,
        callback: suspend (BackupPromptResult) -> Unit,
    ) {
        val ui = BackupUiBridge.delegate
        if (ui == null) {
            logger.w { "Backup UI delegate is not set (iOS export not available)" }
            callback(BackupPromptResult.FAILED)
            return
        }

        val token = NSUUID().UUIDString()
        BackupUiBridge.registerCallback(token) { result ->
            mainScope.launch { callback(result) }
        }

        val mimeType =
            when (backupType) {
                BackupType.DB -> "application/octet-stream"
                BackupType.JSON -> "application/json"
                BackupType.CSV -> "text/csv"
            }
        ui.startExport(token = token, filePath = fileToSharePath.toString(), mimeType = mimeType)
    }

    override suspend fun promptUserForRestore(
        importedFilePath: String,
        callback: suspend (BackupPromptResult) -> Unit,
    ) {
        val ui = BackupUiBridge.delegate
        if (ui == null) {
            logger.w { "Backup UI delegate is not set (iOS restore not available)" }
            callback(BackupPromptResult.FAILED)
            return
        }

        val token = NSUUID().UUIDString()
        BackupUiBridge.registerCallback(token) { result ->
            mainScope.launch { callback(result) }
        }
        ui.startImport(token = token, destinationPath = importedFilePath)
    }
}
