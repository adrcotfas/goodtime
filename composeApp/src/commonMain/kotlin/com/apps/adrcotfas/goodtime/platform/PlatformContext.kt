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
 * Platform-specific context for accessing platform APIs.
 * This is an expect class that will have different implementations on each platform.
 */
expect class PlatformContext

/**
 * Enables or disables fullscreen mode.
 * On Android: Uses WindowInsetsController to hide/show system bars
 * On iOS: May have different implementation or be stubbed
 *
 * @param enabled true to enter fullscreen, false to exit
 */
expect fun PlatformContext.setFullscreen(enabled: Boolean)

/**
 * Enables or disables showing the app when the device is locked.
 * On Android: Uses Activity.setShowWhenLocked() (O_MR1+)
 * On iOS: Not applicable
 *
 * @param enabled true to show when locked, false otherwise
 */
expect fun PlatformContext.setShowWhenLocked(enabled: Boolean)

/**
 * Configures system bars (status bar and navigation bar) appearance.
 * On Android: Uses enableEdgeToEdge() with SystemBarStyle
 * On iOS: Configures status bar appearance
 *
 * @param isDarkTheme whether the app is in dark theme mode
 * @param isFullscreen whether the app is in fullscreen mode
 */
expect fun PlatformContext.configureSystemBars(
    isDarkTheme: Boolean,
    isFullscreen: Boolean,
)
