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

import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import kotlin.time.Duration.Companion.days

/**
 * Manager for iOS cloud backup to iCloud Drive.
 * Performs backups when the app becomes active if more than 24 hours have passed since last backup.
 */
class CloudBackupManager(
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository,
    private val fileSystem: FileSystem,
    private val dbPath: String,
    private val logger: Logger,
) {
    private val metadataQuery = CloudBackupMetadataQuery(logger)

    init {
        logger.i { "iOS CloudBackupManager initialized" }
    }

    /**
     * Push-style scheduling: callers explicitly enable/disable background scheduling when the user toggles the
     * switch (and when Pro is revoked).
     *
     * The toggle does not change outside the app, so there's no need to observe settings continuously.
     */
    fun setAutoBackupSchedulingEnabled(enabled: Boolean) {
        if (enabled) {
            // Enabling auto backup triggers an immediate backup via the UI flow.
            // This call only schedules the periodic background task.
            scheduleCloudBackupTask()
            logger.i { "Cloud backup background task scheduled" }
        } else {
            cancelCloudBackupTask()
            logger.i { "Cloud backup background task cancelled" }
        }
    }

    /**
     * Fast availability check for iCloud Drive.
     * Returns false if user is signed out of iCloud or has disabled iCloud Drive for this app.
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun isICloudAvailable(): Boolean =
        withContext(Dispatchers.IO) {
            logger.d { "isICloudAvailable() - checking token..." }
            val fileManager = NSFileManager.defaultManager
            val hasToken = fileManager.ubiquityIdentityToken != null
            logger.d { "isICloudAvailable() - hasToken=$hasToken, checking container URL..." }
            val containerUrl = fileManager.URLForUbiquityContainerIdentifier(null)
            logger.d { "isICloudAvailable() - containerUrl=${containerUrl?.path}" }
            hasToken && containerUrl != null
        }

    /**
     * Check if backup is needed and perform it if necessary.
     * Should be called when app becomes active.
     */
    suspend fun checkAndPerformBackup() {
        val settings = settingsRepository.settings.first()
        val backupSettings = settings.backupSettings

        if (!settings.isPro) {
            logger.d { "Auto backup skipped: user is not pro" }
            return
        }

        if (!backupSettings.cloudAutoBackupEnabled) {
            logger.d { "Auto backup is disabled" }
            return
        }

        val lastBackupTime = backupSettings.lastBackupTimestamp
        val currentTime = TimeProvider.now()

        if (lastBackupTime > 0 && (currentTime - lastBackupTime) < 1.days.inWholeMilliseconds) {
            logger.d { "Last backup was less than 24 hours ago, skipping" }
            return
        }

        performBackup()
    }

    /**
     * iOS convenience: enable iCloud auto-backup for Pro users if it was never enabled.
     *
     * This is intended to be called when Pro becomes active (no permissions are needed on iOS),
     * and it is safe to call repeatedly.
     */
    suspend fun autoEnableCloudAutoBackupIfEligible() {
        val settings = settingsRepository.settings.first()
        if (!settings.isPro) return

        val backupSettings = settings.backupSettings
        if (backupSettings.cloudAutoBackupEnabled) return

        if (!isICloudAvailable()) {
            logger.i { "Not auto-enabling iCloud auto-backup: iCloud Drive unavailable" }
            return
        }

        logger.i { "Auto-enabling iCloud auto-backup for Pro user" }
        settingsRepository.setBackupSettings(backupSettings.copy(cloudAutoBackupEnabled = true))
        setAutoBackupSchedulingEnabled(true)

        try {
            performBackup()
            logger.i { "Initial iCloud backup completed successfully" }
        } catch (e: Exception) {
            logger.e(e) { "Initial iCloud backup failed" }
        }
    }

    /**
     * Whether background auto-backup should run/schedule (iOS).
     *
     * This is used by the BGTask handler to avoid rescheduling when:
     * - the user disabled auto-backup, or
     * - the user is no longer Pro (refund/revoke).
     */
    suspend fun isAutoBackupEnabledForProUser(): Boolean {
        val settings = settingsRepository.settings.first()
        return settings.isPro && settings.backupSettings.cloudAutoBackupEnabled
    }

    /**
     * Perform a manual backup to iCloud.
     * Does not check the 24-hour interval - forces a backup immediately.
     */
    suspend fun performManualBackup() {
        logger.i { "Manual cloud backup requested" }
        performBackup()
    }

    /**
     * List available iCloud backups using NSMetadataQuery.
     * This finds backups even on a new device where files exist in iCloud but aren't downloaded yet.
     * Returns a list of backup file names sorted by date (newest first).
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun listAvailableBackups(): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileManager = NSFileManager.defaultManager
                val backupsUrl = getBackupsDirUrl(fileManager, createIfMissing = false)
                val backups = metadataQuery.queryAllBackups(backupsUrl)
                backups.map { it.fileName }
            } catch (e: Exception) {
                logger.e(e) { "Failed to list iCloud backups" }
                emptyList()
            }
        }

    /**
     * Restore from a specific iCloud backup file.
     * Handles both downloaded and cloud-only files by downloading if needed.
     * Returns the temporary file path where the backup was copied, ready for BackupManager to restore.
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun getBackupFileForRestore(fileName: String): String =
        withContext(Dispatchers.IO) {
            logger.i { "Preparing iCloud backup for restore: $fileName" }

            val fileManager = NSFileManager.defaultManager
            val backupsUrl = getBackupsDirUrl(fileManager, createIfMissing = false)

            // Query for the backup file to check if it needs to be downloaded
            val backupMetadata = metadataQuery.queryBackupFile(backupsUrl, fileName)

            val backupFilePath: String = if (backupMetadata != null) {
                metadataQuery.ensureBackupDownloaded(backupMetadata)
            } else {
                // Fallback to direct file access if metadata query fails
                val backupFileUrl = backupsUrl.URLByAppendingPathComponent(fileName)
                    ?: throw Exception("Failed to get backup file path")
                backupFileUrl.path ?: throw Exception("Failed to get backup file path string")
            }

            // Verify the file exists locally after download
            if (!fileManager.fileExistsAtPath(backupFilePath)) {
                logger.e { "Backup file does not exist at: $backupFilePath" }
                throw Exception("Backup file does not exist: $fileName")
            }

            // Copy to temporary location for BackupManager to restore
            val tempDir = platform.Foundation.NSTemporaryDirectory()
            val tempFilePath = "${tempDir}goodtime_cloud_restore.db"

            logger.i { "Copying from $backupFilePath to $tempFilePath" }

            // Remove old temp file if exists
            if (fileManager.fileExistsAtPath(tempFilePath)) {
                fileManager.removeItemAtPath(tempFilePath, error = null)
            }

            // Copy backup to temp location
            fileSystem.copy(backupFilePath.toPath(), tempFilePath.toPath())

            // Verify the copy succeeded
            if (!fileManager.fileExistsAtPath(tempFilePath)) {
                logger.e { "Failed to copy backup to temp location" }
                throw Exception("Failed to copy backup file")
            }

            logger.i { "iCloud backup copied to temp location for restore: $tempFilePath" }
            tempFilePath
        }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun performBackup() {
        withContext(Dispatchers.IO) {
            logger.i { "Starting iOS cloud backup to iCloud" }

            // Get iCloud container URL
            val fileManager = NSFileManager.defaultManager

            val backupsUrl = getBackupsDirUrl(fileManager, createIfMissing = true)

            // Generate backup filename with formatted date time
            logger.d { "performBackup() - generating filename..." }
            val fileName = backupManager.generateDbBackupFileName(BackupConstants.DB_BACKUP_PREFIX)
            val backupFileUrl =
                backupsUrl.URLByAppendingPathComponent(fileName)
                    ?: throw Exception("Failed to create backup file path")

            // Checkpoint database and copy to iCloud
            logger.d { "performBackup() - checkpointing database..." }
            backupManager.checkpointDatabase()
            val backupFilePath =
                backupFileUrl.path ?: throw Exception("Failed to get backup file path string")

            logger.d { "performBackup() - copying file to $backupFilePath..." }
            fileSystem.copy(dbPath.toPath(), backupFilePath.toPath())

            // Clean up old backups - keep only the most recent
            logger.d { "performBackup() - cleaning up old backups..." }
            cleanupOldBackups(backupsUrl, fileManager)

            // Update last backup timestamp
            logger.d { "performBackup() - getting current settings..." }
            val currentSettings = settingsRepository.settings.first().backupSettings
            logger.d { "performBackup() - updating backup timestamp..." }
            settingsRepository.setBackupSettings(
                currentSettings.copy(
                    lastBackupTimestamp = TimeProvider.now(),
                ),
            )

            logger.i { "iOS cloud backup completed successfully to iCloud" }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getBackupsDirUrl(
        fileManager: NSFileManager,
        createIfMissing: Boolean,
    ): NSURL {
        val iCloudUrl = fileManager.URLForUbiquityContainerIdentifier(null)
            ?: throw Exception("iCloud not available")

        logger.d { "iCloud container URL: ${iCloudUrl.path}" }

        val documentsUrl =
            iCloudUrl.URLByAppendingPathComponent("Documents")
                ?: throw Exception("Failed to get Documents directory in iCloud")

        val backupsUrl =
            documentsUrl.URLByAppendingPathComponent(BackupConstants.IOS_ICLOUD_BACKUP_SUBPATH)
                ?: throw Exception("Failed to create ${BackupConstants.IOS_ICLOUD_BACKUP_SUBPATH} path")

        logger.d { "Backups directory URL: ${backupsUrl.path}" }
        logger.d { "getBackupsDirUrl() - checking if directory exists at: ${backupsUrl.path}" }
        val pathToCheck = backupsUrl.path ?: ""
        logger.d { "getBackupsDirUrl() - calling fileExistsAtPath..." }
        val exists = fileManager.fileExistsAtPath(pathToCheck)
        logger.d { "getBackupsDirUrl() - fileExistsAtPath returned: $exists" }

        if (createIfMissing && !exists) {
            logger.d { "getBackupsDirUrl() - creating directory..." }
            val success =
                fileManager.createDirectoryAtURL(
                    backupsUrl,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
            logger.d { "getBackupsDirUrl() - directory creation result: $success" }
            if (!success) {
                throw Exception("Failed to create backups directory")
            }
        } else {
            logger.d { "getBackupsDirUrl() - directory already exists or createIfMissing=false" }
        }

        logger.d { "getBackupsDirUrl() - returning" }
        return backupsUrl
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun cleanupOldBackups(
        backupsUrl: NSURL,
        fileManager: NSFileManager,
    ) {
        try {
            val contents =
                fileManager.contentsOfDirectoryAtURL(
                    backupsUrl,
                    includingPropertiesForKeys = null,
                    options = 0u,
                    error = null,
                ) ?: return

            val backupFiles =
                contents
                    .filterIsInstance<NSURL>()
                    .filter { url ->
                        val fileName = url.lastPathComponent ?: ""
                        fileName.startsWith(BackupConstants.DB_BACKUP_PREFIX)
                    }.sortedByDescending { url ->
                        // Get file modification date
                        val attributes = fileManager.attributesOfItemAtPath(url.path ?: "", error = null)
                        (attributes?.get("NSFileModificationDate") as? NSDate)?.timeIntervalSince1970 ?: 0.0
                    }

            // Delete all but the most recent backups
            itemsToDeleteForRetention(backupFiles).forEach { url ->
                fileManager.removeItemAtURL(url, error = null)
                logger.d { "Deleted old backup: ${url.lastPathComponent}" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to cleanup old backups" }
        }
    }
}
