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
import co.touchlab.kermit.Logger
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

/**
 * Manages Google Drive operations for backup and restore.
 *
 * Uses the Drive REST API with appDataFolder scope, which stores files
 * in a hidden app-specific folder that users cannot see directly.
 */
class GoogleDriveManager(
    private val context: Context,
    private val backupManager: BackupManager,
    private val dbPath: String,
    private val cacheDir: String,
    private val logger: Logger,
) {
    /**
     * Upload the current database to Google Drive.
     *
     * @param accessToken OAuth access token with Drive appDataFolder scope
     * @return The uploaded file's ID, or null if upload failed
     */
    suspend fun uploadBackup(accessToken: String): String? =
        withContext(Dispatchers.IO) {
            logger.i { "uploadBackup() - starting" }

            try {
                val driveService = createDriveService(accessToken)

                // Checkpoint database before backing up
                logger.d { "uploadBackup() - checkpointing database" }
                backupManager.checkpointDatabase()

                // Create backup file with timestamp
                val fileName = backupManager.generateDbBackupFileName(BackupConstants.DB_BACKUP_PREFIX)
                val dbFile = java.io.File(dbPath)

                if (!dbFile.exists() || !dbFile.canRead()) {
                    logger.e { "uploadBackup() - database file not accessible: $dbPath" }
                    return@withContext null
                }

                // Delete existing file with same name (overwrites backup done in same minute)
                deleteFileByName(driveService, fileName)

                // Create file metadata for appDataFolder
                val fileMetadata =
                    File().apply {
                        name = fileName
                        parents = listOf("appDataFolder")
                    }

                val mediaContent = FileContent("application/octet-stream", dbFile)

                logger.d { "uploadBackup() - uploading $fileName (${dbFile.length()} bytes)" }
                val uploadedFile =
                    driveService
                        .files()
                        .create(fileMetadata, mediaContent)
                        .setFields("id, name, modifiedTime")
                        .execute()

                logger.i { "uploadBackup() - uploaded successfully: ${uploadedFile.id}" }

                // Clean up old backups
                cleanupOldBackups(driveService)

                uploadedFile.id
            } catch (e: Exception) {
                logger.e(e) { "uploadBackup() - failed" }
                null
            }
        }

    /**
     * List available backups from Google Drive.
     *
     * @param accessToken OAuth access token with Drive appDataFolder scope
     * @return List of backup file names, sorted newest first
     */
    suspend fun listBackups(accessToken: String): List<String> =
        withContext(Dispatchers.IO) {
            logger.d { "listBackups() - fetching" }

            try {
                val driveService = createDriveService(accessToken)

                val result =
                    driveService
                        .files()
                        .list()
                        .setSpaces("appDataFolder")
                        .setFields("files(id, name, modifiedTime)")
                        .setOrderBy("modifiedTime desc")
                        .setPageSize(BackupConstants.MAX_BACKUPS_TO_KEEP)
                        .execute()

                val backups =
                    result.files
                        ?.filter { it.name?.startsWith(BackupConstants.DB_BACKUP_PREFIX) == true }
                        ?.mapNotNull { it.name }
                        ?: emptyList()

                logger.d { "listBackups() - found ${backups.size} backups" }
                backups
            } catch (e: Exception) {
                logger.e(e) { "listBackups() - failed" }
                emptyList()
            }
        }

    /**
     * Download a backup file from Google Drive for restore.
     *
     * @param accessToken OAuth access token with Drive appDataFolder scope
     * @param fileName The name of the backup file to download
     * @return Path to the downloaded file in cache, or null if download failed
     */
    suspend fun downloadBackup(
        accessToken: String,
        fileName: String,
    ): String? =
        withContext(Dispatchers.IO) {
            logger.i { "downloadBackup() - downloading $fileName" }

            try {
                val driveService = createDriveService(accessToken)

                // Find the file by name
                val result =
                    driveService
                        .files()
                        .list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '${fileName.replace("'", "\\'")}'")
                        .setFields("files(id, name)")
                        .execute()

                val file = result.files?.firstOrNull()
                if (file == null) {
                    logger.e { "downloadBackup() - file not found: $fileName" }
                    return@withContext null
                }

                // Download to cache directory
                val tempFile = java.io.File(cacheDir, "goodtime_cloud_restore.db")
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                FileOutputStream(tempFile).use { outputStream ->
                    driveService
                        .files()
                        .get(file.id)
                        .executeMediaAndDownloadTo(outputStream)
                }

                logger.i { "downloadBackup() - downloaded to ${tempFile.absolutePath}" }
                tempFile.absolutePath
            } catch (e: Exception) {
                logger.e(e) { "downloadBackup() - failed" }
                null
            }
        }

    /**
     * Delete a file by name if it exists.
     * Used to overwrite backups with the same timestamp (same minute).
     */
    private fun deleteFileByName(
        driveService: Drive,
        fileName: String,
    ) {
        try {
            val result =
                driveService
                    .files()
                    .list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '${fileName.replace("'", "\\'")}'")
                    .setFields("files(id, name)")
                    .execute()

            result.files?.forEach { file ->
                logger.d { "deleteFileByName() - deleting existing file: ${file.name}" }
                driveService.files().delete(file.id).execute()
            }
        } catch (e: Exception) {
            logger.e(e) { "deleteFileByName() - failed to delete $fileName" }
        }
    }

    /**
     * Clean up old backups keeping only the most recent ones.
     */
    private fun cleanupOldBackups(driveService: Drive) {
        try {
            val result =
                driveService
                    .files()
                    .list()
                    .setSpaces("appDataFolder")
                    .setFields("files(id, name, modifiedTime)")
                    .setOrderBy("modifiedTime desc")
                    .execute()

            val backupFiles =
                result.files
                    ?.filter { it.name?.startsWith(BackupConstants.DB_BACKUP_PREFIX) == true }
                    ?: emptyList()

            val filesToDelete = itemsToDeleteForRetention(backupFiles)
            filesToDelete.forEach { file ->
                logger.d { "cleanupOldBackups() - deleting ${file.name}" }
                driveService.files().delete(file.id).execute()
            }

            if (filesToDelete.isNotEmpty()) {
                logger.i { "cleanupOldBackups() - deleted ${filesToDelete.size} old backups" }
            }
        } catch (e: Exception) {
            logger.e(e) { "cleanupOldBackups() - failed" }
        }
    }

    /**
     * Create an authenticated Drive service instance.
     */
    private fun createDriveService(accessToken: String): Drive {
        val httpTransport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        return Drive
            .Builder(httpTransport, jsonFactory) { request ->
                request.headers["Authorization"] = "Bearer $accessToken"
            }.setApplicationName("Goodtime Productivity")
            .build()
    }
}
