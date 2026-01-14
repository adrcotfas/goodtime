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
package com.apps.adrcotfas.goodtime.bl

/**
 * Bridge object that allows Swift code to access the TimerManager.
 * Stores a direct reference to TimerManager instead of using Koin DI,
 * because Live Activities run in a separate process where Koin context
 * may not be available.
 */
object LiveActivityIntentBridge {
    private var timerManager: TimerManager? = null

    fun setTimerManager(manager: TimerManager) {
        timerManager = manager
    }

    fun getTimerManager(): TimerManager? = timerManager
}
