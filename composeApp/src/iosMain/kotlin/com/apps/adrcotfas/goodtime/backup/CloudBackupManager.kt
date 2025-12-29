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
import okio.buffer
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

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
     * This intentionally does not validate free space; it only checks that the container is accessible.
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun isICloudAvailable(): Boolean =
        withContext(Dispatchers.IO) {
            val fileManager = NSFileManager.defaultManager
            val hasIdentity = fileManager.ubiquityIdentityToken != null
            val containerUrl =
                fileManager.URLForUbiquityContainerIdentifier(null)
                    ?: fileManager.URLForUbiquityContainerIdentifier("iCloud.app.goodtime.productivity")
            hasIdentity && containerUrl != null
        }

    /**
     * Validates that we can write *something* to iCloud Drive without creating a real backup.
     * This is used to detect "iCloud full" (or similar write failures) when enabling auto-backup.
     *
     * It creates and immediately deletes a tiny marker file in `Documents/Goodtime/Backups`.
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun testICloudWriteAccess() {
        // TODO: is there no other way of interrogating iCloud for space?
        withContext(Dispatchers.IO) {
            val fileManager = NSFileManager.defaultManager
            val backupsUrl = getBackupsDirUrl(fileManager, createIfMissing = true)

            val markerName = ".Goodtime-write-test-${NSUUID.UUID().UUIDString}.tmp"
            val markerUrl =
                backupsUrl.URLByAppendingPathComponent(markerName)
                    ?: throw Exception("Failed to create marker file path")
            val markerPath = markerUrl.path ?: throw Exception("Failed to get marker file path string")

            // Write a tiny file, then delete it. If iCloud is out of space, this is expected to throw.
            val sink = fileSystem.sink(markerPath.toPath(), mustCreate = true).buffer()
            try {
                sink.writeUtf8("1")
            } finally {
                sink.close()
            }
            fileSystem.delete(markerPath.toPath())
        }
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
        val dayInMillis = 24 * 60 * 60 * 1000L

        if (lastBackupTime > 0 && (currentTime - lastBackupTime) < dayInMillis) {
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
     * List available iCloud backups.
     * Returns a list of backup file names sorted by date (newest first).
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun listAvailableBackups(): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileManager = NSFileManager.defaultManager
                val backupsUrl = getBackupsDirUrl(fileManager, createIfMissing = false)
                if (!fileManager.fileExistsAtPath(backupsUrl.path ?: "")) {
                    return@withContext emptyList()
                }

                val contents =
                    fileManager.contentsOfDirectoryAtURL(
                        backupsUrl,
                        includingPropertiesForKeys = null,
                        options = 0u,
                        error = null,
                    ) as List<*>? ?: return@withContext emptyList()

                val backupFiles =
                    contents
                        .filterIsInstance<NSURL>()
                        .filter { url ->
                            val fileName = url.lastPathComponent ?: ""
                            fileName.startsWith(BackupConstants.DB_BACKUP_PREFIX)
                        }.sortedByDescending { url ->
                            val attributes = fileManager.attributesOfItemAtPath(url.path ?: "", error = null)
                            (attributes?.get("NSFileModificationDate") as? NSDate)?.timeIntervalSince1970 ?: 0.0
                        }.mapNotNull { it.lastPathComponent }

                logger.i { "Found ${backupFiles.size} iCloud backups" }
                backupFiles
            } catch (e: Exception) {
                logger.e(e) { "Failed to list iCloud backups" }
                emptyList()
            }
        }

    /**
     * Restore from a specific iCloud backup file.
     * Returns the temporary file path where the backup was copied, ready for BackupManager to restore.
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun getBackupFileForRestore(fileName: String): String =
        withContext(Dispatchers.IO) {
            logger.i { "Preparing iCloud backup for restore: $fileName" }

            val fileManager = NSFileManager.defaultManager
            val backupsUrl = getBackupsDirUrl(fileManager, createIfMissing = false)

            val backupFileUrl =
                backupsUrl.URLByAppendingPathComponent(fileName)
                    ?: throw Exception("Failed to get backup file path")

            val backupFilePath =
                backupFileUrl.path
                    ?: throw Exception("Failed to get backup file path string")

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
            val fileName = backupManager.generateDbBackupFileName(BackupConstants.DB_BACKUP_PREFIX)
            val backupFileUrl =
                backupsUrl.URLByAppendingPathComponent(fileName)
                    ?: throw Exception("Failed to create backup file path")

            // Checkpoint database and copy to iCloud
            backupManager.checkpointDatabase()
            val backupFilePath =
                backupFileUrl.path ?: throw Exception("Failed to get backup file path string")

            fileSystem.copy(dbPath.toPath(), backupFilePath.toPath())

            // Clean up old backups - keep only the most recent
            cleanupOldBackups(backupsUrl, fileManager)

            // Update last backup timestamp
            val currentSettings = settingsRepository.settings.first().backupSettings
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
        val iCloudUrl =
            fileManager.URLForUbiquityContainerIdentifier(null)
                ?: fileManager.URLForUbiquityContainerIdentifier("iCloud.app.goodtime.productivity")

        if (iCloudUrl == null) {
            throw Exception("iCloud not available")
        }

        val documentsUrl =
            iCloudUrl.URLByAppendingPathComponent("Documents")
                ?: throw Exception("Failed to get Documents directory in iCloud")

        val backupsUrl =
            documentsUrl.URLByAppendingPathComponent(BackupConstants.IOS_ICLOUD_BACKUP_SUBPATH)
                ?: throw Exception("Failed to create ${BackupConstants.IOS_ICLOUD_BACKUP_SUBPATH} path")

        if (createIfMissing && !fileManager.fileExistsAtPath(backupsUrl.path ?: "")) {
            val success =
                fileManager.createDirectoryAtURL(
                    backupsUrl,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
            if (!success) {
                throw Exception("Failed to create backups directory")
            }
        }

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
                ) as List<*>? ?: return

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

    companion object
}
