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

import com.apps.adrcotfas.goodtime.data.model.Session
import kotlin.time.Duration.Companion.milliseconds

/**
 * Pure session-building math, extracted from TimerManager: duration rounding
 * (wiggle room), interruption subtraction for focus sessions, and the
 * elapsedRealtime -> wall-clock timestamp conversion.
 */
object FinishedSessionFactory {
    // some extra time to be used when converting millis to minutes and avoid rounding issues
    const val WIGGLE_ROOM_MILLIS = 10000L

    /**
     * Returns the finished [Session] together with its duration in minutes,
     * or null when the session is shorter than a minute (not worth saving).
     * [data] must already contain up-to-date pause bookkeeping.
     */
    fun create(
        data: DomainTimerData,
        now: Long,
        elapsedRealtime: Long,
        notes: String = "",
    ): Pair<Session, Long>? {
        val isFocus = data.runtime.type == TimerType.FOCUS

        val endTime = data.runtime.endTime
        val interruptions = data.runtime.timeSpentPaused

        val durationToSaveMinutes = durationMinutes(data.runtime)
        if (durationToSaveMinutes < 1) {
            return null
        }

        // Calculate timestamp based on when the session actually ended (endTime)
        // endTime is in elapsedRealtime (millis since boot), convert to wall-clock time
        val timestampAtEnd = now - elapsedRealtime + endTime

        return Session.create(
            timestamp = timestampAtEnd,
            duration = durationToSaveMinutes,
            interruptions = if (isFocus) interruptions.milliseconds.inWholeMinutes else 0,
            label = data.getLabelName(),
            isWork = isFocus,
            notes = notes,
        ) to durationToSaveMinutes
    }

    /**
     * Whole-minute duration credited for [runtime], matching what [create] saves.
     * Used to recompute completedMinutes when a finished session is restored after
     * process death (that value isn't part of the persisted runtime state).
     */
    fun durationMinutes(runtime: TimerRuntimeState): Long {
        val isFocus = runtime.type == TimerType.FOCUS
        val totalDuration = runtime.endTime - runtime.startTime
        return totalDuration
            .let { duration -> if (isFocus) duration - runtime.timeSpentPaused else duration }
            .plus(WIGGLE_ROOM_MILLIS)
            .milliseconds
            .inWholeMinutes
    }
}
