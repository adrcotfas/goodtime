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

import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.data.settings.SoundData

/**
 * Represents the configuration state of the sound player.
 */
data class SoundPlayerState(
    /** Sound configuration for work/focus timer completion */
    val workRingTone: SoundData = SoundData(),
    /** Sound configuration for break timer completion */
    val breakRingTone: SoundData = SoundData(),
    /** Whether sounds should loop until manually stopped */
    val loop: Boolean = false,
    /** Whether to override system sound profile settings */
    val overrideSoundProfile: Boolean = false,
)

interface SoundPlayer {
    /**
     * Plays the appropriate sound for the given timer type.
     */
    fun play(timerType: TimerType)

    /**
     * Plays a specific sound with custom configuration.
     */
    fun play(
        soundData: SoundData,
        loop: Boolean = false,
        forceSound: Boolean = false,
    )

    /**
     * Stops any currently playing sound.
     */
    fun stop()

    /**
     * Cleanup resources.
     */
    fun close()
}
