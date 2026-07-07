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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that performs scheduled Google Drive backups.
 *
 * This worker:
 * - Checks if user is Pro and cloud auto-backup is enabled
 * - Checks if enough time has passed since the last backup (24h)
 * - Re-authorizes with Google (tokens are short-lived, but re-auth is silent after initial consent)
 * - Uploads the database to Google Drive appDataFolder
 * - Updates the last backup timestamp
 */
class GoogleDriveBackupWorker(
    context: Context,
    private val backupService: GoogleDriveBackupService,
    private val googleDriveManager: GoogleDriveManager,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        logger.i { "GoogleDriveBackupWorker - starting" }

        return try {
            val settings = settingsRepository.settings.first()
            val result = backupService.getAuthTokenOrNull()
            if (result != null) {
                googleDriveManager.uploadBackup(result)
                logger.i { "GoogleDriveBackupWorker - backup completed successfully" }
            } else {
                settingsRepository.setBackupSettings(
                    settings.backupSettings.copy(cloudAutoBackupEnabled = false),
                )
                logger.e { "missing Drive permission, disabling auto-backup" }
            }
            Result.success()
        } catch (e: Exception) {
            logger.e(e) { "failed" }
            Result.failure()
        }
    }

    companion object {
        const val AUTO_BACKUP = "auto_google_drive_backup_work"
    }
}
