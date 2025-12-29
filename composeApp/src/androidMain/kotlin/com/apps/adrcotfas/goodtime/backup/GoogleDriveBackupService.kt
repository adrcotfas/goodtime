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

/**
 * Placeholder for future Google Drive backup implementation.
 */
class GoogleDriveBackupService : CloudBackupService {
    override suspend fun preflightBackup(): CloudAutoBackupIssue? = CloudAutoBackupIssue.UNKNOWN

    override fun setAutoBackupEnabled(enabled: Boolean) {
        // no-op until Google Drive integration exists
    }

    override suspend fun backupNow(): BackupPromptResult = BackupPromptResult.FAILED

    override suspend fun listAvailableBackups(): List<String> = emptyList()

    override suspend fun restoreFromBackup(fileName: String): BackupPromptResult = BackupPromptResult.FAILED

    override suspend fun attemptEnableAutoBackup(): CloudAutoBackupIssue? = CloudAutoBackupIssue.UNKNOWN
}
