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
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

/**
 * Android implementation of PlatformContext.
 * Wraps a ComponentActivity to provide access to window and activity APIs.
 */
actual class PlatformContext(
    internal val activity: ComponentActivity,
) {
    val window get() = activity.window
}

/**
 * Enables or disables fullscreen mode on Android.
 * Hides or shows system bars (status bar and navigation bar).
 */
actual fun PlatformContext.setFullscreen(enabled: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

    if (enabled) {
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Enables or disables showing the app when the device is locked.
 * Only available on Android O_MR1 (API 27) and above.
 */
actual fun PlatformContext.setShowWhenLocked(enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        activity.setShowWhenLocked(enabled)
    }
}

/**
 * Configures system bars (status bar and navigation bar) appearance.
 * Uses edge-to-edge mode with appropriate scrim colors for light/dark themes.
 */
actual fun PlatformContext.configureSystemBars(
    isDarkTheme: Boolean,
    isFullscreen: Boolean,
) {
    activity.enableEdgeToEdge(
        statusBarStyle =
            SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ) { isDarkTheme },
        navigationBarStyle =
            SystemBarStyle.auto(
                lightScrim = lightScrim,
                darkScrim = darkScrim,
                detectDarkMode = { isDarkTheme },
            ),
    )
}

// Scrim colors for navigation bar
private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
