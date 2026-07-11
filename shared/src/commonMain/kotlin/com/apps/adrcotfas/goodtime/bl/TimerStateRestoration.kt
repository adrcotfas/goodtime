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

import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Handles restoration of timer state after app termination.
 * Handles device reboot and restores state.
 * An expired timer is restored as-is: whoever comes next (the alarm via
 * [TimerManager.finish] or the foreground monitor's force finish) finishes it
 * and saves the session; discarding it here would lose the session.
 */
class TimerStateRestoration(
    private val settingsRepo: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val log: Logger,
) {
    suspend fun restoreTimerState(updateTimerData: (TimerRuntimeState) -> Unit) {
        val persistedState =
            settingsRepo.settings
                .map { it.persistedTimerState }
                .first() ?: return

        val now = timeProvider.now()
        val elapsedRealtime = timeProvider.elapsedRealtime()

        log.i { "Attempting to restore timer state: $persistedState" }

        // Check if device rebooted
        val deviceRebooted = elapsedRealtime < persistedState.startTime

        if (deviceRebooted) {
            log.i { "Device was rebooted, recalculating times" }
            // Recalculate times based on wall-clock
            val remainingTime = persistedState.endTimeWallClock - now
            val newEndTime = elapsedRealtime + remainingTime
            val duration = persistedState.endTime - persistedState.startTime
            val newStartTime = newEndTime - duration

            val timeSinceSave = now - persistedState.savedAtWallClock
            val runtime = persistedState.toRuntimeState()
            val newLastStartTime =
                if (runtime.state == TimerState.PAUSED) {
                    0
                } else {
                    (elapsedRealtime - timeSinceSave).coerceAtLeast(0)
                }

            updateTimerData(
                TimerRuntimeState(
                    state = runtime.state,
                    type = runtime.type,
                    startTime = newStartTime,
                    lastStartTime = newLastStartTime,
                    endTime = newEndTime,
                    timeSpentPaused = runtime.timeSpentPaused,
                    timeAtPause = runtime.timeAtPause,
                    lastPauseTime = 0,
                ),
            )
            log.i { "Timer state restored after reboot" }
        } else {
            log.i { "Device not rebooted, restoring times directly" }
            // Normal case - elapsedRealtime values still valid
            updateTimerData(persistedState.toRuntimeState())
            log.i { "Timer state restored" }
        }
    }
}
