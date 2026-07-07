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

import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DndModeManager(
    private val notificationManager: NotificationArchManager,
    settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope,
) : EventListener {
    private val isDndDuringWorkFlow =
        settingsRepository.settings.map { it.uiSettings.dndDuringWork }

    private var job: Job? = null

    private var dndEnabledBeforeStart = false
    private var wasPaused = false

    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start -> {
                if (event.isFocus) {
                    if (!wasPaused) {
                        maybeEnterDndMode()
                    }
                } else { // break session
                    maybeExitDndMode()
                }
                wasPaused = false
            }

            is Event.Pause -> {
                wasPaused = true
            }

            is Event.Reset, is Event.Finished -> {
                maybeExitDndMode()
                wasPaused = false
            }

            else -> { // do nothing
            }
        }
    }

    private fun maybeEnterDndMode() {
        job?.cancel()
        job =
            coroutineScope.launch {
                val isDndDuringWork = isDndDuringWorkFlow.first()
                if (!isDndDuringWork) return@launch

                val wasDndEnabled = notificationManager.isDndModeEnabled()
                dndEnabledBeforeStart = wasDndEnabled
                if (!wasDndEnabled) {
                    withContext(Dispatchers.Main) {
                        delay(2500) // don't enter immediately to not affect ongoing sounds/vibrations
                        notificationManager.toggleDndMode(true)
                    }
                }
            }
    }

    private fun maybeExitDndMode() {
        job?.cancel()
        job =
            coroutineScope.launch {
                val isDndDuringWork = isDndDuringWorkFlow.first()
                if (!isDndDuringWork) return@launch

                if (!dndEnabledBeforeStart && notificationManager.isDndModeEnabled()) {
                    withContext(Dispatchers.Main) {
                        notificationManager.toggleDndMode(false)
                    }
                }
            }
    }
}
