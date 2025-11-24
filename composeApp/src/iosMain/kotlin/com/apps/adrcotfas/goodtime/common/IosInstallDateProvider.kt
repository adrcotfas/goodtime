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
package com.apps.adrcotfas.goodtime.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileCreationDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSinceNow
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class IosInstallDateProvider : InstallDateProvider {
    @OptIn(ExperimentalForeignApi::class)
    override fun isInstallOlderThan10Days(): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager
            val urlToDocumentsFolder =
                fileManager
                    .URLsForDirectory(
                        directory = NSDocumentDirectory,
                        inDomains = NSUserDomainMask,
                    ).lastOrNull() ?: return false

            val path = urlToDocumentsFolder.path ?: return false
            val attributes = fileManager.attributesOfItemAtPath(path, error = null) ?: return false
            val installDate = attributes[NSFileCreationDate] as? NSDate ?: return false

            // timeIntervalSinceNow returns negative seconds if the date is in the past
            val secondsSinceInstall = -installDate.timeIntervalSinceNow
            secondsSinceInstall.seconds > 10.days
        } catch (_: Exception) {
            false
        }
    }
}
