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
package com.apps.adrcotfas.goodtime.data.model

import com.apps.adrcotfas.goodtime.bl.TimerType
import kotlin.time.Duration.Companion.minutes

data class TimerProfile(
    val name: String? = null,
    val isCountdown: Boolean = true,
    /** Work(focus) duration in minutes; invalid for isCountdown false */
    val workDuration: Int = DEFAULT_WORK_DURATION,
    /** Break duration in minutes */
    val isBreakEnabled: Boolean = true,
    val breakDuration: Int = DEFAULT_BREAK_DURATION,
    val isLongBreakEnabled: Boolean = false,
    /** Long break duration in minutes */
    val longBreakDuration: Int = DEFAULT_LONG_BREAK_DURATION,
    /** Number of sessions before long break*/
    val sessionsBeforeLongBreak: Int = DEFAULT_SESSIONS_BEFORE_LONG_BREAK,
    /** the ratio between work and break duration; invalid for isCountdown true */
    val workBreakRatio: Int = DEFAULT_WORK_BREAK_RATIO,
) {
    companion object {
        const val DEFAULT_PROFILE_NAME = "25/5"
        const val DEFAULT_WORK_DURATION = 25
        const val DEFAULT_BREAK_DURATION = 5
        const val DEFAULT_LONG_BREAK_DURATION = 15
        const val DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4
        const val DEFAULT_WORK_BREAK_RATIO = 3

        fun default() =
            TimerProfile(
                name = null,
                isCountdown = true,
                workDuration = DEFAULT_WORK_DURATION,
                isBreakEnabled = true,
                breakDuration = DEFAULT_BREAK_DURATION,
                isLongBreakEnabled = false,
                longBreakDuration = DEFAULT_LONG_BREAK_DURATION,
                sessionsBeforeLongBreak = DEFAULT_SESSIONS_BEFORE_LONG_BREAK,
                workBreakRatio = DEFAULT_WORK_BREAK_RATIO,
            )
    }
}

/**
 * Returns the end time of the timer in milliseconds since Unix Epoch.
 * If the timer is not a countdown timer, returns 0.
 * @param timerType the type of the timer
 * @param elapsedRealTime the elapsed real time in milliseconds since boot, including time spent in sleep
 */
fun TimerProfile.endTime(
    timerType: TimerType,
    elapsedRealTime: Long,
): Long =
    if (isCountdown) {
        elapsedRealTime + this.duration(timerType).minutes.inWholeMilliseconds
    } else {
        0
    }

fun TimerProfile.duration(timerType: TimerType): Int =
    when (timerType) {
        TimerType.FOCUS -> workDuration
        TimerType.BREAK -> breakDuration
        TimerType.LONG_BREAK -> longBreakDuration
    }
