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

class IosLiveActivityListener(
    private val liveActivityBridge: LiveActivityBridge,
    private val timeProvider: TimeProvider,
    private val log: Logger,
) : EventListener {
    override fun onEvent(event: Event) {
        if (!liveActivityBridge.isSupported()) {
            return
        }

        when (event) {
            is Event.Start -> {
                log.v { "IosLiveActivityListener: Starting Live Activity (isFocus=${event.isFocus}, countdown=${event.endTime != 0L})" }

                val isCountdown = event.isCountdown
                // Calculate duration from current time to end time
                val durationSeconds = if (isCountdown) {
                    val currentTime = timeProvider.elapsedRealtime()
                    val durationMillis = event.endTime - currentTime
                    durationMillis / 1000 // Convert to seconds
                } else {
                    0L // For count-up, duration doesn't matter
                }

                liveActivityBridge.start(
                    isFocus = event.isFocus,
                    isCountdown = isCountdown,
                    durationSeconds = durationSeconds,
                    labelName = event.labelName,
                    isDefaultLabel = event.isDefaultLabel,
                )
            }

            is Event.Pause -> {
                log.v { "IosLiveActivityListener: Pausing Live Activity" }
                liveActivityBridge.pause()
            }

            is Event.AddOneMinute -> {
                log.v { "IosLiveActivityListener: Adding one minute to Live Activity" }
                liveActivityBridge.addOneMinute()
            }

            is Event.Reset, is Event.Finished -> {
                log.v { "IosLiveActivityListener: Ending Live Activity" }
                liveActivityBridge.end()
            }

            else -> {
                // Ignore other events
            }
        }
    }
}

val EventListener.Companion.IOS_LIVE_ACTIVITY_LISTENER: String
    get() = "IosLiveActivityListener"
