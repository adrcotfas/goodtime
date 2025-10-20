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

import com.apps.adrcotfas.goodtime.bl.Event
import com.apps.adrcotfas.goodtime.bl.EventListener

class SoundVibrationAndTorchPlayer(
    private val soundPlayer: SoundPlayer,
    private val vibrationPlayer: VibrationPlayer,
    private val torchManager: TorchManager,
) : EventListener {
    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start -> {
                if (!event.autoStarted) {
                    soundPlayer.stop()
                    vibrationPlayer.stop()
                    torchManager.stop()
                }
            }

            is Event.Finished -> {
                soundPlayer.play(event.type)
                vibrationPlayer.start()
                torchManager.start()
            }

            Event.Reset -> {
                soundPlayer.stop()
                vibrationPlayer.stop()
                torchManager.stop()
            }

            else -> {
                // do nothing
            }
        }
    }
}
