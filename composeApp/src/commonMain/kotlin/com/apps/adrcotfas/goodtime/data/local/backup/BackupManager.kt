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
package com.apps.adrcotfas.goodtime.data.local.backup

import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.TimeUtils.formatForBackupFileName
import com.apps.adrcotfas.goodtime.bl.TimeUtils.formatToIso8601
import com.apps.adrcotfas.goodtime.bl.TimerManager
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.local.ProductivityDatabase
import com.apps.adrcotfas.goodtime.data.local.getRoomDatabase
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.di.reinitModulesAtBackupAndRestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class BackupManager(
    private val fileSystem: FileSystem,
    private val dbPath: String,
    private val filesDirPath: String,
    private val database: ProductivityDatabase,
    private val timeProvider: TimeProvider,
    private val backupPrompter: BackupPrompter,
    private val localDataRepository: LocalDataRepository,
    private val logger: Logger,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : KoinComponent {
    private val importedTemporaryFileName = "$filesDirPath/last-import"

    init {
        fileSystem.createDirectory(filesDirPath.toPath())
    }

    suspend fun backup(onComplete: (Boolean) -> Unit) {
        try {
            val tmpFilePath = "$filesDirPath/${generateBackupFileName()}"
            createBackup(tmpFilePath)
            backupPrompter.promptUserForBackup(BackupType.DB, tmpFilePath.toPath()) {
                onComplete(it)
            }
        } catch (e: Exception) {
            logger.e(e) { "Backup failed" }
            onComplete(false)
        }
    }

    suspend fun backupToCsv(onComplete: (Boolean) -> Unit) {
        try {
            val tmpFilePath = "$filesDirPath/${generateBackupFileName(PREFIX)}.csv"
            createCsvBackup(tmpFilePath)
            backupPrompter.promptUserForBackup(BackupType.CSV, tmpFilePath.toPath()) {
                onComplete(it)
            }
        } catch (e: Exception) {
            logger.e(e) { "Backup failed" }
            onComplete(false)
        }
    }

    suspend fun backupToJson(onComplete: (Boolean) -> Unit) {
        try {
            val tmpFilePath = "$filesDirPath/${generateBackupFileName(PREFIX)}.json"
            createJsonBackup(tmpFilePath)
            backupPrompter.promptUserForBackup(BackupType.JSON, tmpFilePath.toPath()) {
                onComplete(it)
            }
        } catch (e: Exception) {
            logger.e(e) { "Backup failed" }
            onComplete(false)
        }
    }

    suspend fun restore(onComplete: (Boolean) -> Unit) {
        try {
            backupPrompter.promptUserForRestore(importedTemporaryFileName) {
                if (!isSQLite3File(importedTemporaryFileName.toPath())) {
                    logger.e { "Invalid backup file" }
                    onComplete(false)
                } else {
                    restoreBackup()
                    onComplete(true)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Restore backup failed" }
            onComplete(false)
        }
    }

    suspend fun checkpointDatabase() {
        database.sessionsDao().checkpoint()
    }

    fun generateBackupFileName(prefix: String = DB_BACKUP_PREFIX): String = "${prefix}${timeProvider.now().formatForBackupFileName()}"

    private suspend fun createBackup(tmpFilePath: String) {
        withContext(defaultDispatcher) {
            checkpointDatabase()
            fileSystem.copy(dbPath.toPath(), tmpFilePath.toPath())
        }
    }

    private suspend fun createCsvBackup(tmpFilePath: String) {
        withContext(defaultDispatcher) {
            fileSystem.sink(tmpFilePath.toPath()).buffer().use { sink ->
                sink.writeUtf8("end,duration,interruptions,label,notes,is_break,is_archived\n")
                localDataRepository.selectAllSessions().first().forEach { session ->
                    val labelName =
                        if (session.label == Label.DEFAULT_LABEL_NAME) "" else session.label
                    sink.writeUtf8(
                        "${session.timestamp.formatToIso8601()}," +
                            "${session.duration}," +
                            "${session.interruptions}," +
                            "$labelName," +
                            "${session.notes}," +
                            "${!session.isWork}," +
                            "${session.isArchived}\n",
                    )
                }
            }
        }
    }

    private suspend fun createJsonBackup(tmpFilePath: String) {
        withContext(defaultDispatcher) {
            fileSystem.sink(tmpFilePath.toPath()).buffer().use { sink ->
                sink.writeUtf8("[\n")
                localDataRepository
                    .selectAllSessions()
                    .first()
                    .forEachIndexed { index, session ->
                        val labelName =
                            if (session.label == Label.DEFAULT_LABEL_NAME) "" else session.label
                        sink.writeUtf8(
                            "{" +
                                "\"end\":${session.timestamp.formatToIso8601()}," +
                                "\"duration\":${session.duration}," +
                                "\"interruptions\":${session.interruptions}," +
                                "\"label\":\"${labelName}\"," +
                                "\"notes\":\"${session.notes}\"," +
                                "\"is_break\":${!session.isWork}," +
                                "\"archived\":${session.isArchived}}",
                        )
                        if (index < localDataRepository.selectAllSessions().first().size - 1) {
                            sink.writeUtf8(",\n")
                        }
                    }
                sink.writeUtf8("\n]")
            }
        }
    }

    private suspend fun restoreBackup() {
        withContext(defaultDispatcher) {
            try {
                checkpointDatabase()
                fileSystem.copy(importedTemporaryFileName.toPath(), dbPath.toPath())
            } finally {
                afterOperation()
            }
        }
    }

    private fun afterOperation() {
        reinitModulesAtBackupAndRestore()
        get<LocalDataRepository>().reinitDatabase(getRoomDatabase(get()))
        get<TimerManager>().restart()
    }

    private fun isSQLite3File(filePath: Path): Boolean {
        val header = ("SQLite format 3").encodeUtf8().toByteArray()
        val buffer = ByteArray(header.size)
        fileSystem.source(filePath).buffer().use { source ->
            val count = source.read(buffer)
            return if (count < header.size) false else buffer.contentEquals(header)
        }
    }

    companion object {
        private const val PREFIX = "GT"
        private const val DB_BACKUP_PREFIX = "$PREFIX-Backup-"
    }
}
