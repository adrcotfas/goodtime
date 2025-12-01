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

/**
 * iOS implementation of PlatformContext.
 * TODO: Implement iOS-specific functionality on Mac.
 */
actual class PlatformContext

/**
 * iOS implementation of setFullscreen.
 * TODO: iOS handles fullscreen differently - implement or adapt as needed
 */
actual fun PlatformContext.setFullscreen(enabled: Boolean) {
    // Stub implementation - to be completed on Mac
    // iOS may not have direct fullscreen concept like Android
}

/**
 * iOS implementation of setShowWhenLocked.
 * Not applicable to iOS - this is an Android-specific feature.
 */
actual fun PlatformContext.setShowWhenLocked(enabled: Boolean) {
    // Not applicable to iOS - no-op
}

/**
 * iOS implementation of configureSystemBars.
 * TODO: Implement iOS status bar configuration
 */
actual fun PlatformContext.configureSystemBars(
    isDarkTheme: Boolean,
    isFullscreen: Boolean,
) {
    // Stub implementation - to be completed on Mac
    // Configure iOS status bar appearance based on theme
}
