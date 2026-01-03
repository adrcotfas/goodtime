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
package com.apps.adrcotfas.goodtime.bl.notifications

import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.Event
import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import kotlin.math.abs

class SoundVibrationAndTorchPlayer(
    private val soundPlayer: SoundPlayer,
    private val vibrationPlayer: VibrationPlayer,
    private val torchManager: TorchManager,
    private val timeProvider: TimeProvider,
    private val logger: Logger,
) : EventListener {
    // Using this expected endTime logic because on iOS, if the app is in the foreground while the timer finishes,
    // the finish event is not triggered in the background but when the user brings the app to foreground.
    // In this case, we don't want sound and vibration to play because the user already received a notification.

    // On Android this is not an issue because the finish event is executed while the app is in the background.
    // We have a foreground service there keeping the app alive.
    // Note: endTime is stored as ElapsedRealtime (ms since boot)
    private var endTime = 0L

    override fun onEvent(event: Event) {
        logger.d { "onEvent: $event, elapsedRealtime: ${timeProvider.elapsedRealtime()}, endTime: $endTime" }
        if (event !is Event.Finished && event !is Event.BringToForeground) {
            logger.d { "resetting endTime" }
            reset()
        }

        when (event) {
            is Event.Start -> {
                if (!event.autoStarted) {
                    soundPlayer.stop()
                    vibrationPlayer.stop()
                    torchManager.stop()
                }
            }

            is Event.Finished -> {
                val now = timeProvider.elapsedRealtime()
                // if the app stayed in the foreground during the session OR
                // there's less than 1 second difference between the expected end time and now, play the orchestra
                // this condition is not true if the user brings the app to foreground 1 second after receiving the notification
                if (endTime == 0L || abs(now - endTime) < 1000L) {
                    logger.d { "playing sound and vibration" }
                    soundPlayer.play(event.type)
                    vibrationPlayer.start()
                    torchManager.start()
                }
            }

            Event.Reset -> {
                logger.d { "stopping sound and vibration" }
                soundPlayer.stop()
                vibrationPlayer.stop()
                torchManager.stop()
            }

            is Event.SendToBackground -> {
                logger.d { "update endTime: ${event.endTime}" }
                endTime = event.endTime
            }

            else -> {
                // do nothing
            }
        }
    }

    private fun reset() {
        endTime = 0L
    }
}
