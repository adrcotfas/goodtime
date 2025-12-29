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
 * Platform-specific configuration and capability detection.
 * Different platforms support different features.
 */
interface PlatformConfiguration {
    /**
     * Platform identity flag intended for UI decisions.
     *
     * Keep this here (instead of ad-hoc expect/actual helpers) so we have a single source of truth.
     */
    val isAndroid: Boolean

    /**
     * Cloud backup provider for this platform.
     */
    val cloudProvider: CloudProvider

    /**
     * Whether the platform supports in-app updates.
     * Android (Google Play): true
     * Android (F-Droid): false
     * iOS (App Store): false
     */
    val supportsInAppUpdates: Boolean

    /**
     * Whether the platform supports dynamic color (Material You).
     * Android 12+ (API 31+): true
     * Older Android: false
     * iOS: false (has its own system)
     */
    val supportsDynamicColor: Boolean

    /**
     * Whether the platform supports showing the app when the device is locked.
     * Android: true (API 27+)
     * iOS: false (not applicable)
     */
    val supportsShowWhenLocked: Boolean
}

/**
 * Gets the current platform's configuration.
 * This is an expect function that will have different implementations on each platform.
 */
expect fun getPlatformConfiguration(): PlatformConfiguration

expect fun isFDroid(): Boolean
