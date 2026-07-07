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

import kotlin.concurrent.Volatile

actual class PlatformContext

actual fun PlatformContext.setFullscreen(enabled: Boolean) {
    // Control iOS status bar visibility via the global StatusBarState
    StatusBarState.isHidden = enabled
}

actual fun PlatformContext.setShowWhenLocked(enabled: Boolean) {
    // iOS handles this differently - would need UIKit configuration
    // Not implemented for iOS yet
}

actual fun PlatformContext.configureSystemBars(isDarkTheme: Boolean) {
    // iOS system bars (status bar style) are managed automatically by SwiftUI
    // based on the color scheme of the app
}

/**
 * Global state for iOS status bar visibility.
 * This is accessed from both Kotlin and Swift (via ObjC interop).
 */
object StatusBarState {
    @Volatile
    var isHidden: Boolean = false
        set(value) {
            field = value
            // Notify Swift observers
            statusBarDelegate?.onStatusBarVisibilityChanged(value)
        }

    var statusBarDelegate: StatusBarDelegate? = null
}

/**
 * Delegate interface for status bar visibility changes.
 * Implemented by Swift code to receive updates.
 */
interface StatusBarDelegate {
    fun onStatusBarVisibilityChanged(isHidden: Boolean)
}
