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

class ICloudBackupService(
    private val cloudBackupManager: CloudBackupManager,
    private val backupManager: BackupManager,
) : CloudBackupService {
    override suspend fun preflightBackup(): CloudAutoBackupIssue? {
        if (!cloudBackupManager.isICloudAvailable()) {
            return CloudAutoBackupIssue.ICLOUD_UNAVAILABLE
        }
        return try {
            cloudBackupManager.testICloudWriteAccess()
            null
        } catch (e: Exception) {
            e.toCloudAutoBackupIssue()
        }
    }

    override fun setAutoBackupEnabled(enabled: Boolean) {
        cloudBackupManager.setAutoBackupSchedulingEnabled(enabled)
    }

    override suspend fun backupNow(): BackupPromptResult =
        try {
            cloudBackupManager.performManualBackup()
            BackupPromptResult.SUCCESS
        } catch (_: Exception) {
            BackupPromptResult.FAILED
        }

    override suspend fun listAvailableBackups(): List<String> =
        try {
            cloudBackupManager.listAvailableBackups()
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun restoreFromBackup(fileName: String): BackupPromptResult =
        try {
            val tempFilePath = cloudBackupManager.getBackupFileForRestore(fileName)
            backupManager.restoreFromFile(tempFilePath)
        } catch (_: Exception) {
            BackupPromptResult.FAILED
        }

    override suspend fun attemptEnableAutoBackup(): CloudAutoBackupIssue? {
        val preflight = preflightBackup()
        if (preflight != null) return preflight

        return try {
            cloudBackupManager.performManualBackup()
            null
        } catch (e: Exception) {
            e.toCloudAutoBackupIssue()
        }
    }

    private fun Exception.toCloudAutoBackupIssue(): CloudAutoBackupIssue {
        val msg =
            buildString {
                append(this@toCloudAutoBackupIssue.toString())
                cause?.let {
                    append(" | ")
                    append(it.toString())
                }
            }.lowercase()

        return when {
            msg.contains("icloud not available") ||
                msg.contains("icloud container is not available") ||
                msg.contains("container is not available") -> CloudAutoBackupIssue.ICLOUD_UNAVAILABLE

            msg.contains("out of space") ||
                msg.contains("no space") ||
                msg.contains("code=28") ||
                msg.contains("code=640") ||
                msg.contains("error 640") ||
                msg.contains("cocoa error 640") ||
                msg.contains("nsposixerrordomain") -> CloudAutoBackupIssue.ICLOUD_FULL

            else -> CloudAutoBackupIssue.UNKNOWN
        }
    }
}
