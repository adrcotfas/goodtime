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

/**
 * Platform-specific provider for checking app installation date.
 */
interface InstallDateProvider {
    /**
     * Checks if the app was installed more than 10 days ago.
     * Used to determine if it's appropriate to ask for app reviews.
     * @return true if the installation is older than 10 days, false otherwise
     */
    fun isInstallOlderThan10Days(): Boolean
}
