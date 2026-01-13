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
package com.apps.adrcotfas.goodtime.data.settings

import com.apps.adrcotfas.goodtime.bl.TimerRuntimeState
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Serializable
data class BreakBudgetData(
    val breakBudget: Duration = 0.minutes,
    val breakBudgetStart: Long = 0, // millis since boot
    val isAccumulating: Boolean = false,
) {
    fun getRemainingBreakBudget(elapsedRealtime: Long): Duration {
        val timeSinceBreakBudgetStart = elapsedRealtime - breakBudgetStart
        val breakBudgetMs = breakBudget.inWholeMilliseconds
        return max(0, (breakBudgetMs - timeSinceBreakBudgetStart)).milliseconds
    }
}

@Serializable
data class LongBreakData(
    val streak: Int = 0,
    val lastWorkEndTime: Long = 0, // millis since boot
)

fun LongBreakData.streakInUse(sessionsBeforeLongBreak: Int): Int = streak % sessionsBeforeLongBreak

/**
 * Persisted timer state for iOS to restore app state after process termination.
 * Contains both elapsedRealtime values (for normal app kill) and wall-clock timestamps
 * (for detecting expiration and handling device reboot).
 */
@Serializable
data class PersistedTimerState(
    val state: Int, // TimerState.ordinal
    val type: Int, // TimerType.ordinal
    val startTime: Long = 0, // elapsedRealtime when timer started
    val lastStartTime: Long = 0, // elapsedRealtime when timer was last started/resumed
    val endTime: Long = 0, // elapsedRealtime when timer should end
    val timeSpentPaused: Long = 0, // total duration paused in millis
    val timeAtPause: Long = 0, // remaining time when paused (duration in millis)
    val lastPauseTime: Long = 0, // elapsedRealtime when last paused
    val savedAtWallClock: Long = 0, // System.currentTimeMillis() when saved
    val endTimeWallClock: Long = 0, // Wall-clock time when timer should end
) {
    companion object {
        fun from(
            runtime: TimerRuntimeState,
            savedAtWallClock: Long,
            endTimeWallClock: Long,
        ) = PersistedTimerState(
            state = runtime.state.ordinal,
            type = runtime.type.ordinal,
            startTime = runtime.startTime,
            lastStartTime = runtime.lastStartTime,
            endTime = runtime.endTime,
            timeSpentPaused = runtime.timeSpentPaused,
            timeAtPause = runtime.timeAtPause,
            lastPauseTime = runtime.lastPauseTime,
            savedAtWallClock = savedAtWallClock,
            endTimeWallClock = endTimeWallClock,
        )
    }

    fun toRuntimeState() =
        TimerRuntimeState(
            startTime = startTime,
            lastStartTime = lastStartTime,
            lastPauseTime = lastPauseTime,
            endTime = endTime,
            timeAtPause = timeAtPause,
            state = com.apps.adrcotfas.goodtime.bl.TimerState.entries[state],
            type = com.apps.adrcotfas.goodtime.bl.TimerType.entries[type],
            timeSpentPaused = timeSpentPaused,
        )
}
