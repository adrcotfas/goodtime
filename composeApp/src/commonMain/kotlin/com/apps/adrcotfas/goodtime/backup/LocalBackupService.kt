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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocalBackupService(
    private val backupManager: BackupManager,
) {
    suspend fun backup(): BackupPromptResult =
        suspendCancellableCoroutine { cont ->
            CoroutineScope(cont.context).launch {
                try {
                    backupManager.backup { cont.resume(it) }
                } catch (_: Throwable) {
                    cont.resume(BackupPromptResult.FAILED)
                }
            }
        }

    suspend fun restore(): BackupPromptResult =
        suspendCancellableCoroutine { cont ->
            CoroutineScope(cont.context).launch {
                try {
                    backupManager.restore { cont.resume(it) }
                } catch (_: Throwable) {
                    cont.resume(BackupPromptResult.FAILED)
                }
            }
        }

    suspend fun exportCsv(): BackupPromptResult =
        suspendCancellableCoroutine { cont ->
            CoroutineScope(cont.context).launch {
                try {
                    backupManager.exportCsv { cont.resume(it) }
                } catch (_: Throwable) {
                    cont.resume(BackupPromptResult.FAILED)
                }
            }
        }

    suspend fun exportJson(): BackupPromptResult =
        suspendCancellableCoroutine { cont ->
            CoroutineScope(cont.context).launch {
                try {
                    backupManager.exportJson { cont.resume(it) }
                } catch (_: Throwable) {
                    cont.resume(BackupPromptResult.FAILED)
                }
            }
        }
}
