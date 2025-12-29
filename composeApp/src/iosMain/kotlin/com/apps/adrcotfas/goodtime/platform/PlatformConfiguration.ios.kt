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
package com.apps.adrcotfas.goodtime.platform

import com.apps.adrcotfas.goodtime.backup.CloudProvider

/**
 * iOS implementation of PlatformConfiguration.
 */
private class IosPlatformConfiguration : PlatformConfiguration {
    override val isAndroid: Boolean = false

    override val cloudProvider: CloudProvider = CloudProvider.ICLOUD

    /**
     * In-app updates are not supported on iOS App Store.
     * iOS uses a different update mechanism through the App Store.
     */
    override val supportsInAppUpdates: Boolean = false

    /**
     * Dynamic color (Material You) is an Android concept.
     * iOS has its own system color adaptation.
     */
    override val supportsDynamicColor: Boolean = false

    /**
     * Show when locked is not applicable to iOS.
     */
    override val supportsShowWhenLocked: Boolean = false
}

/**
 * Returns the iOS platform configuration.
 */
actual fun getPlatformConfiguration(): PlatformConfiguration = IosPlatformConfiguration()

actual fun isFDroid(): Boolean = false
