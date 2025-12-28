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

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.local.backup.BackupManager
import com.apps.adrcotfas.goodtime.data.settings.AppSettings
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import java.io.FileInputStream

/**
 * WorkManager worker that performs the auto backup operation.
 * It retrieves the backup path from settings and calls BackupManager to perform the backup.
 */
class AutoBackupWorker(
    private val context: Context,
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
    private val dbPath: String,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    override suspend fun doWork(): Result {
        logger.i { "Starting auto backup worker" }

        try {
            val settings: AppSettings = settingsRepository.settings.first()
            val backupPath = settings.backupSettings.path

            if (!settings.backupSettings.autoBackupEnabled || backupPath.isBlank()) {
                logger.w { "Auto backup is disabled or path is invalid, skipping backup" }
                return Result.failure()
            }

            backupManager.checkpointDatabase()
            val fileName = backupManager.generateBackupFileName(DB_AUTO_BACKUP_PREFIX)

            val backupDirUri = backupPath.toUri()
            val backupDir = DocumentFile.fromTreeUri(context, backupDirUri)
            if (backupDir == null || !backupDir.isDirectory) {
                logger.w { "Backup directory is invalid" }
                // the directory was probably deleted
                return Result.failure()
            }

            backupDir.findFile(fileName)?.delete()

            val backupFile = backupDir.createFile("application/octet-stream", fileName)
            if (backupFile == null) {
                logger.w { "Failed to create backup file" }
                return Result.failure()
            }

            FileInputStream(dbPath).use { input ->
                context.contentResolver.openOutputStream(backupFile.uri)?.use { output ->
                    input.copyTo(output)
                } ?: run {
                    logger.w { "Failed to open output stream for backup file" }
                    return Result.failure()
                }
            }

            backupDir
                .listFiles()
                .filter { it.isFile && it.name?.startsWith(DB_AUTO_BACKUP_PREFIX) == true }
                .sortedByDescending { it.lastModified() }
                .drop(7)
                .forEach { it.delete() }

            // Update last backup timestamp
            settingsRepository.setBackupSettings(
                settings.backupSettings.copy(
                    lastBackupTimestamp = System.currentTimeMillis(),
                ),
            )

            logger.i { "Auto backup completed successfully" }
            return Result.success()
        } catch (e: Exception) {
            logger.e(e) { "Auto backup failed" }
            return Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "auto_backup_work"
        private const val DB_AUTO_BACKUP_PREFIX = "GT-AutoBackup-"
    }
}
