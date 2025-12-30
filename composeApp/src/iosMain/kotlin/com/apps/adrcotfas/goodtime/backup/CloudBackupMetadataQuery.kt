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
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSMetadataItem
import platform.Foundation.NSMetadataItemFSContentChangeDateKey
import platform.Foundation.NSMetadataItemFSNameKey
import platform.Foundation.NSMetadataItemURLKey
import platform.Foundation.NSMetadataQuery
import platform.Foundation.NSMetadataQueryDidFinishGatheringNotification
import platform.Foundation.NSMetadataQueryDidUpdateNotification
import platform.Foundation.NSMetadataQueryUbiquitousDocumentsScope
import platform.Foundation.NSMetadataUbiquitousItemDownloadingStatusCurrent
import platform.Foundation.NSMetadataUbiquitousItemDownloadingStatusDownloaded
import platform.Foundation.NSMetadataUbiquitousItemDownloadingStatusKey
import platform.Foundation.NSMetadataUbiquitousItemIsDownloadedKey
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSPredicate
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import kotlin.coroutines.resume

/**
 * Metadata for an iCloud backup file.
 */
data class BackupMetadata(
    val fileName: String,
    val url: NSURL,
    val modificationDate: Long,
    val isDownloaded: Boolean,
)

/**
 * Helper class that encapsulates NSMetadataQuery operations for iCloud backup discovery.
 *
 * This class solves the "new device" problem where backups exist in iCloud but haven't
 * been downloaded to the local device yet. NSMetadataQuery queries the iCloud metadata
 * database, which knows about files even if they aren't locally cached.
 */
class CloudBackupMetadataQuery(
    private val logger: Logger,
) {
    companion object {
        private const val QUERY_TIMEOUT_MS = 10_000L
        private const val DOWNLOAD_TIMEOUT_MS = 30_000L
    }

    /**
     * Query all backup files in iCloud using NSMetadataQuery.
     * This finds backups even if they haven't been downloaded to the device yet.
     *
     * @param backupsUrl The URL of the Backups directory in iCloud
     * @return List of backup metadata, sorted by modification date (newest first)
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    suspend fun queryAllBackups(backupsUrl: NSURL): List<BackupMetadata> = withContext(Dispatchers.Main) {
        logger.d { "Starting metadata query for backups at: ${backupsUrl.path}" }

        val query = NSMetadataQuery()

        // Create predicate to find backup files
        // We look for files that start with the backup prefix
        @Suppress("UNCHECKED_CAST")
        query.predicate = NSPredicate.predicateWithFormat(
            "%K BEGINSWITH %@",
            argumentArray = listOf(
                NSMetadataItemFSNameKey,
                BackupConstants.DB_BACKUP_PREFIX,
            ) as List<Any?>,
        )

        // Search in the ubiquitous documents scope (iCloud Documents)
        query.setSearchScopes(listOf(NSMetadataQueryUbiquitousDocumentsScope))

        val results = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            executeQuery(query)
        }

        if (results == null) {
            logger.w { "Metadata query timed out after ${QUERY_TIMEOUT_MS}ms" }
            return@withContext emptyList()
        }

        logger.d { "Metadata query returned ${results.size} results" }

        // Parse results into BackupMetadata objects
        val backups = results.mapNotNull { item ->
            parseMetadataItem(item, backupsUrl)
        }.sortedByDescending { it.modificationDate }

        logger.i { "Found ${backups.size} backups via metadata query (${backups.count { it.isDownloaded }} downloaded)" }
        backups
    }

    /**
     * Query a specific backup file by name.
     *
     * @param backupsUrl The URL of the Backups directory in iCloud
     * @param fileName The name of the backup file to find
     * @return The backup metadata, or null if not found
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    suspend fun queryBackupFile(backupsUrl: NSURL, fileName: String): BackupMetadata? =
        withContext(Dispatchers.Main) {
            logger.d { "Querying for specific backup: $fileName" }

            val query = NSMetadataQuery()

            @Suppress("UNCHECKED_CAST")
            query.predicate = NSPredicate.predicateWithFormat(
                "%K == %@",
                argumentArray = listOf(
                    NSMetadataItemFSNameKey,
                    fileName,
                ) as List<Any?>,
            )

            query.setSearchScopes(listOf(NSMetadataQueryUbiquitousDocumentsScope))

            val results = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
                executeQuery(query)
            }

            if (results == null) {
                logger.w { "Metadata query for $fileName timed out" }
                return@withContext null
            }

            results.firstNotNullOfOrNull { parseMetadataItem(it, backupsUrl) }
        }

    /**
     * Ensure a backup file is downloaded to the device.
     * If the file is not downloaded, triggers a download and waits for completion.
     *
     * @param metadata The backup metadata
     * @return The local file path, or throws an exception if download fails
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun ensureBackupDownloaded(metadata: BackupMetadata): String = withContext(Dispatchers.IO) {
        val filePath = metadata.url.path ?: throw Exception("Invalid backup URL")

        if (metadata.isDownloaded) {
            return@withContext filePath
        }

        logger.i { "Downloading backup: ${metadata.fileName}" }
        startDownload(metadata.url)

        val downloaded = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
            waitForDownloadCompletion(metadata.url, metadata.fileName)
        }

        if (downloaded != true) {
            throw Exception("Download timed out for ${metadata.fileName}")
        }

        filePath
    }

    /**
     * Execute a metadata query and return the results.
     */
    @OptIn(ExperimentalForeignApi::class)
    private suspend fun executeQuery(query: NSMetadataQuery): List<NSMetadataItem> =
        suspendCancellableCoroutine { continuation ->
            var observer: Any? = null

            observer = NSNotificationCenter.defaultCenter.addObserverForName(
                name = NSMetadataQueryDidFinishGatheringNotification,
                `object` = query,
                queue = NSOperationQueue.mainQueue,
            ) { _ ->
                query.disableUpdates()

                @Suppress("UNCHECKED_CAST")
                val results = (0 until query.resultCount.toInt()).mapNotNull {
                    query.resultAtIndex(it.toULong()) as? NSMetadataItem
                }

                query.stopQuery()

                observer?.let {
                    NSNotificationCenter.defaultCenter.removeObserver(it)
                }

                if (continuation.isActive) {
                    continuation.resume(results)
                }
            }

            continuation.invokeOnCancellation {
                query.stopQuery()
                observer?.let {
                    NSNotificationCenter.defaultCenter.removeObserver(it)
                }
            }

            query.startQuery()
        }

    /**
     * Parse an NSMetadataItem into a BackupMetadata object.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun parseMetadataItem(item: NSMetadataItem, backupsUrl: NSURL): BackupMetadata? {
        val fileName = item.valueForAttribute(NSMetadataItemFSNameKey) as? String ?: return null
        val url = item.valueForAttribute(NSMetadataItemURLKey) as? NSURL ?: return null

        logger.d { "Parsing metadata item: $fileName, url=${url.path}" }

        // Verify the file is in our backups directory
        val urlPath = url.path ?: return null
        val backupsPath = backupsUrl.path ?: return null
        if (!urlPath.startsWith(backupsPath)) {
            logger.w { "Skipping file outside backups path: $urlPath (expected prefix: $backupsPath)" }
            return null
        }

        val modificationDate = (item.valueForAttribute(NSMetadataItemFSContentChangeDateKey) as? NSDate)
            ?.timeIntervalSince1970
            ?.times(1000)
            ?.toLong()
            ?: 0L

        // Check download status - file is downloaded if either flag indicates so
        val isDownloadedFlag = item.valueForAttribute(NSMetadataUbiquitousItemIsDownloadedKey) as? Boolean ?: false
        val downloadStatus = item.valueForAttribute(NSMetadataUbiquitousItemDownloadingStatusKey) as? String
        val isDownloaded = isDownloadedFlag ||
            downloadStatus == NSMetadataUbiquitousItemDownloadingStatusDownloaded ||
            downloadStatus == NSMetadataUbiquitousItemDownloadingStatusCurrent

        return BackupMetadata(
            fileName = fileName,
            url = url,
            modificationDate = modificationDate,
            isDownloaded = isDownloaded,
        )
    }

    /**
     * Start downloading a file from iCloud.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun startDownload(url: NSURL) {
        val fileManager = NSFileManager.defaultManager

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val success = fileManager.startDownloadingUbiquitousItemAtURL(
                url,
                error = errorPtr.ptr,
            )

            if (!success) {
                val error = errorPtr.value?.localizedDescription ?: "Unknown error"
                throw Exception("Failed to start download: $error")
            }
        }
    }

    /**
     * Wait for a file download to complete using NSMetadataQuery updates.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private suspend fun waitForDownloadCompletion(url: NSURL, fileName: String): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val query = NSMetadataQuery()

                @Suppress("UNCHECKED_CAST")
                query.predicate = NSPredicate.predicateWithFormat(
                    "%K == %@",
                    argumentArray = listOf(
                        NSMetadataItemFSNameKey,
                        fileName,
                    ) as List<Any?>,
                )
                query.setSearchScopes(listOf(NSMetadataQueryUbiquitousDocumentsScope))

                var updateObserver: Any? = null
                var gatheringObserver: Any? = null

                fun checkDownloadStatus(): Boolean {
                    if (query.resultCount.toInt() > 0) {
                        val item = query.resultAtIndex(0u) as? NSMetadataItem
                        val isDownloaded = item?.valueForAttribute(NSMetadataUbiquitousItemIsDownloadedKey) as? Boolean
                            ?: false
                        val downloadStatus = item?.valueForAttribute(NSMetadataUbiquitousItemDownloadingStatusKey) as? String

                        if (isDownloaded ||
                            downloadStatus == NSMetadataUbiquitousItemDownloadingStatusDownloaded ||
                            downloadStatus == NSMetadataUbiquitousItemDownloadingStatusCurrent
                        ) {
                            return true
                        }
                    }
                    return false
                }

                fun cleanup() {
                    query.stopQuery()
                    updateObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                    gatheringObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                }

                fun resumeIfActive(result: Boolean) {
                    cleanup()
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                updateObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = NSMetadataQueryDidUpdateNotification,
                    `object` = query,
                    queue = NSOperationQueue.mainQueue,
                ) { _ ->
                    if (checkDownloadStatus()) {
                        resumeIfActive(true)
                    }
                }

                gatheringObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = NSMetadataQueryDidFinishGatheringNotification,
                    `object` = query,
                    queue = NSOperationQueue.mainQueue,
                ) { _ ->
                    // Check immediately after initial gathering
                    if (checkDownloadStatus()) {
                        resumeIfActive(true)
                    }
                    // Keep listening for updates
                }

                continuation.invokeOnCancellation {
                    cleanup()
                }

                query.startQuery()
            }
        }
}
