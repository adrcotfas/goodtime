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
import com.apps.adrcotfas.goodtime.platform.getPlatformConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Handles restoration of timer state after app termination (iOS only).
 * Checks if timer expired, handles device reboot, and restores state.
 */
class TimerStateRestoration(
    private val settingsRepo: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val log: Logger,
    private val coroutineScope: CoroutineScope,
) {
    fun restoreTimerState(updateTimerData: (TimerRuntimeState) -> Unit) {
        // Only restore state on iOS
        if (getPlatformConfiguration().isAndroid) {
            return
        }

        coroutineScope.launch {
            val persistedState =
                settingsRepo.settings
                    .map { it.persistedTimerState }
                    .first() ?: return@launch

            val now = timeProvider.now()
            val elapsedRealtime = timeProvider.elapsedRealtime()

            log.i { "Attempting to restore timer state: $persistedState" }

            // Check if timer expired while app was killed
            if (now >= persistedState.endTimeWallClock && persistedState.state == TimerState.RUNNING.ordinal) {
                log.i { "Timer already expired, not restoring state" }
                settingsRepo.clearPersistedTimerState()
                return@launch
            }

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
}
