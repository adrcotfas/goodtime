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
import com.apps.adrcotfas.goodtime.data.settings.PersistedTimerState
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * iOS-only EventListener that persists timer state for restoration after app termination.
 * Listens to timer events and saves/clears state accordingly.
 */
class IosTimerStatePersistenceListener(
    private val settingsRepo: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val coroutineScope: CoroutineScope,
    private val log: Logger,
) : EventListener {
    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start -> {
                persistTimerState(event.runtimeState)
            }
            is Event.Pause -> {
                persistTimerState(event.runtimeState)
            }
            is Event.Reset, is Event.Finished -> {
                clearPersistedTimerState()
            }
            else -> {
                // Other events don't affect persistence
            }
        }
    }

    private fun persistTimerState(runtimeState: TimerRuntimeState) {
        if (!runtimeState.state.isActive) {
            return
        }

        val now = timeProvider.now()
        val elapsedRealtime = timeProvider.elapsedRealtime()

        val state =
            PersistedTimerState.from(
                runtime = runtimeState,
                savedAtWallClock = now,
                endTimeWallClock = now - elapsedRealtime + runtimeState.endTime,
            )

        coroutineScope.launch {
            settingsRepo.setPersistedTimerState(state)
            log.v { "Timer state persisted: $state" }
        }
    }

    private fun clearPersistedTimerState() {
        coroutineScope.launch {
            settingsRepo.clearPersistedTimerState()
            log.v { "Timer state cleared" }
        }
    }
}
