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
package com.apps.adrcotfas.goodtime.data.backup

import com.apps.adrcotfas.goodtime.backup.BackupPromptResult
import kotlin.concurrent.Volatile

/**
 * Swift-side UI delegate for presenting export (share sheet) and import (document picker).
 *
 * We use a token-based callback registry because the presentation is async and is handled on iOS
 * by Swift/SwiftUI. Swift calls back into Kotlin via [BackupUiBridge.complete].
 */
interface BackupUiDelegate {
    /**
     * Present UI to export/share the file at [filePath].
     *
     * Swift must eventually call [BackupUiBridge.complete] with the same [token].
     */
    fun startExport(
        token: String,
        filePath: String,
        mimeType: String,
    )

    /**
     * Present UI to import a file and copy it to [destinationPath].
     *
     * Swift must copy the picked file into [destinationPath] and then call [BackupUiBridge.complete].
     */
    fun startImport(
        token: String,
        destinationPath: String,
    )
}

object BackupUiBridge {
    @Volatile
    var delegate: BackupUiDelegate? = null

    // Token -> completion callback
    private val callbacks = mutableMapOf<String, (BackupPromptResult) -> Unit>()

    fun registerCallback(
        token: String,
        callback: (BackupPromptResult) -> Unit,
    ) {
        callbacks[token] = callback
    }

    fun complete(
        token: String,
        result: BackupPromptResult,
    ) {
        val cb = callbacks.remove(token) ?: return
        cb(result)
    }
}
