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
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.data.local.backup.BackupType
import okio.Path

class IosBackupPrompter(
    private val logger: Logger,
) : BackupPrompter {
    override suspend fun promptUserForBackup(
        backupType: BackupType,
        fileToSharePath: Path,
        callback: suspend (Boolean) -> Unit,
    ) {
        // TODO: Implement iCloud backup for iOS
        // For iOS, backup should use iCloud Drive or share sheet for seamless sync
        // - Use FileManager to save backups to iCloud container
        // - Access via FileManager.default.url(forUbiquityContainerIdentifier:)
        // - Enable iCloud capability in Xcode project settings
        // - Or use UIActivityViewController for sharing
        logger.w { "Backup is not yet implemented for iOS" }
        callback(false)
    }

    override suspend fun promptUserForRestore(
        importedFilePath: String,
        callback: suspend (Boolean) -> Unit,
    ) {
        // TODO: Implement iCloud restore for iOS
        // - Use UIDocumentPickerViewController to let user pick a backup file
        // - Or restore from iCloud Drive if auto-backup is enabled
        logger.w { "Restore is not yet implemented for iOS" }
        callback(false)
    }
}