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

import android.os.Build
import com.apps.adrcotfas.goodtime.BuildConfig.IS_FDROID
import com.apps.adrcotfas.goodtime.backup.CloudProvider

/**
 * Android implementation of PlatformConfiguration.
 */
private class AndroidPlatformConfiguration : PlatformConfiguration {
    override val isAndroid: Boolean = true

    override val cloudProvider: CloudProvider = CloudProvider.GOOGLE_DRIVE

    /**
     * In-app updates are supported on Android (Google Play flavor).
     * The actual availability is controlled by passing onUpdateClicked callback in GoodtimeApp.
     * F-Droid flavor will pass null for this callback.
     */
    override val supportsInAppUpdates: Boolean = true

    /**
     * Dynamic color (Material You) is supported on Android 12+ (API 31+).
     */
    override val supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Show when locked is supported on Android O_MR1+ (API 27+).
     */
    override val supportsShowWhenLocked: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
}

/**
 * Returns the Android platform configuration.
 */
actual fun getPlatformConfiguration(): PlatformConfiguration = AndroidPlatformConfiguration()

actual fun isFDroid(): Boolean = IS_FDROID
