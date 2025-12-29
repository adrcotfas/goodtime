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

/**
 * Platform-specific cloud backup operations (iCloud Drive on iOS, Google Drive on Android later).
 *
 * Keeping this as an interface (instead of expect/actual top-level functions) helps:
 * - reduce duplication and platform branching in view models
 * - make testing easier via fakes
 * - prepare for multi-provider implementations
 */
interface CloudBackupService {
    /**
     * Preflight check used by UX flows that need a clear "cloud not usable" message.
     *
     * Return null when cloud operations are expected to work.
     */
    suspend fun preflightBackup(): CloudAutoBackupIssue?

    /**
     * Push-style scheduling toggle.
     *
     * iOS: schedules/cancels the BGTask-based periodic cloud backup.
     * Android (for now): no-op (Google Drive not implemented yet).
     */
    fun setAutoBackupEnabled(enabled: Boolean)

    suspend fun backupNow(): BackupPromptResult

    suspend fun listAvailableBackups(): List<String>

    suspend fun restoreFromBackup(fileName: String): BackupPromptResult

    /**
     * Called when the user tries to enable cloud auto backup.
     *
     * - Return null: enabling is possible (and any required initial backup succeeded)
     * - Return non-null: enabling failed and the UI should show a message.
     */
    suspend fun attemptEnableAutoBackup(): CloudAutoBackupIssue?
}
