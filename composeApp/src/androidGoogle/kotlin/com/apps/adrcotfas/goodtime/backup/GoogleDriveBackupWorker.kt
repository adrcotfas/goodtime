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
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.days

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
    private val googleDriveAuthManager: GoogleDriveAuthManager,
    private val googleDriveManager: GoogleDriveManager,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        logger.i { "GoogleDriveBackupWorker - starting" }

        try {
            val settings = settingsRepository.settings.first()

            // Check if user is Pro
            if (!settings.isPro) {
                logger.w { "GoogleDriveBackupWorker - user is not Pro, skipping" }
                return Result.failure()
            }

            // Check if cloud auto-backup is enabled
            if (!settings.backupSettings.cloudAutoBackupEnabled) {
                logger.w { "GoogleDriveBackupWorker - cloud auto-backup disabled, skipping" }
                return Result.failure()
            }

            // Check if enough time has passed since last backup (24h minimum)
            val lastBackupTime = settings.backupSettings.cloudLastBackupTimestamp
            val currentTime = TimeProvider.now()
            if (lastBackupTime > 0 && (currentTime - lastBackupTime) < 1.days.inWholeMilliseconds) {
                logger.d { "GoogleDriveBackupWorker - last backup was less than 24h ago, skipping" }
                return Result.success()
            }

            // Get fresh access token (re-authorization is silent after initial consent)
            logger.d { "GoogleDriveBackupWorker - authorizing" }
            val authResult = googleDriveAuthManager.authorize()

            val accessToken =
                when (authResult) {
                    is GoogleDriveAuthResult.Success -> authResult.accessToken
                    is GoogleDriveAuthResult.NeedsUserConsent -> {
                        // Cannot show UI from worker - user needs to open app
                        logger.w { "GoogleDriveBackupWorker - needs user consent, cannot proceed in background" }
                        return Result.retry()
                    }
                    is GoogleDriveAuthResult.Error -> {
                        logger.e(authResult.exception) { "GoogleDriveBackupWorker - authorization failed" }
                        return Result.retry()
                    }
                }

            // Validate the token before attempting backup (detects server-side revocation)
            try {
                googleDriveManager.validateToken(accessToken)
            } catch (e: TokenRevokedException) {
                logger.w { "GoogleDriveBackupWorker - token revoked, disabling auto-backup" }
                // Disable auto-backup so user sees the issue when they open the app
                settingsRepository.setBackupSettings(
                    settings.backupSettings.copy(cloudAutoBackupEnabled = false),
                )
                return Result.failure()
            }

            // Perform the backup
            logger.d { "GoogleDriveBackupWorker - uploading backup" }
            val fileId =
                try {
                    googleDriveManager.uploadBackup(accessToken)
                } catch (e: TokenRevokedException) {
                    logger.w { "GoogleDriveBackupWorker - token revoked during upload, disabling auto-backup" }
                    settingsRepository.setBackupSettings(
                        settings.backupSettings.copy(cloudAutoBackupEnabled = false),
                    )
                    return Result.failure()
                }

            if (fileId == null) {
                logger.e { "GoogleDriveBackupWorker - backup upload failed" }
                return Result.retry()
            }

            // Update last backup timestamp
            settingsRepository.setBackupSettings(
                settings.backupSettings.copy(
                    cloudLastBackupTimestamp = TimeProvider.now(),
                ),
            )

            logger.i { "GoogleDriveBackupWorker - backup completed successfully" }
            return Result.success()
        } catch (e: Exception) {
            logger.e(e) { "GoogleDriveBackupWorker - failed with exception" }
            return Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "google_drive_backup_work"
    }
}
