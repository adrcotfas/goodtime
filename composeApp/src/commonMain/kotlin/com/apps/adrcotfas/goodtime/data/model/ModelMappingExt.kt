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

import com.apps.adrcotfas.goodtime.data.local.LocalLabel
import com.apps.adrcotfas.goodtime.data.local.LocalSession
import com.apps.adrcotfas.goodtime.data.local.LocalTimerProfile

fun Label.toLocal(): LocalLabel =
    LocalLabel(
        name = name,
        colorIndex = colorIndex,
        orderIndex = orderIndex,
        useDefaultTimeProfile = useDefaultTimeProfile,
        timerProfileName = timerProfile.name,
        isCountdown = timerProfile.isCountdown,
        workDuration = timerProfile.workDuration,
        isBreakEnabled = timerProfile.isBreakEnabled,
        breakDuration = timerProfile.breakDuration,
        isLongBreakEnabled = timerProfile.isLongBreakEnabled,
        longBreakDuration = timerProfile.longBreakDuration,
        sessionsBeforeLongBreak = timerProfile.sessionsBeforeLongBreak,
        workBreakRatio = timerProfile.workBreakRatio,
        isArchived = isArchived,
    )

fun LocalLabel.toExternal(timerProfile: TimerProfile? = null): Label =
    Label(
        name = name,
        colorIndex = colorIndex,
        orderIndex = orderIndex,
        useDefaultTimeProfile = useDefaultTimeProfile,
        timerProfile =
            timerProfile ?: TimerProfile(
                name = null,
                isCountdown = isCountdown,
                workDuration = workDuration,
                isBreakEnabled = isBreakEnabled,
                breakDuration = breakDuration,
                isLongBreakEnabled = isLongBreakEnabled,
                longBreakDuration = longBreakDuration,
                sessionsBeforeLongBreak = sessionsBeforeLongBreak,
                workBreakRatio = workBreakRatio,
            ),
        isArchived = isArchived,
    )

fun Session.toLocal() =
    LocalSession(
        id = id,
        timestamp = timestamp,
        duration = duration,
        interruptions = interruptions,
        labelName = label,
        notes = notes,
        isWork = isWork,
        isArchived = isArchived,
    )

fun LocalSession.toExternal() =
    Session(
        id = id,
        timestamp = timestamp,
        duration = duration,
        interruptions = interruptions,
        label = labelName,
        notes = notes,
        isWork = isWork,
        isArchived = isArchived,
    )

fun LocalTimerProfile.toExternal(): TimerProfile =
    TimerProfile(
        name = name,
        isCountdown = isCountdown,
        workDuration = workDuration,
        isBreakEnabled = isBreakEnabled,
        breakDuration = breakDuration,
        isLongBreakEnabled = isLongBreakEnabled,
        longBreakDuration = longBreakDuration,
        sessionsBeforeLongBreak = sessionsBeforeLongBreak,
        workBreakRatio = workBreakRatio,
    )

fun TimerProfile.toLocal(): LocalTimerProfile {
    if (name == null) {
        throw IllegalArgumentException("Timer profile name cannot be null")
    }
    return LocalTimerProfile(
        name = name,
        isCountdown = isCountdown,
        workDuration = workDuration,
        isBreakEnabled = isBreakEnabled,
        breakDuration = breakDuration,
        isLongBreakEnabled = isLongBreakEnabled,
        longBreakDuration = longBreakDuration,
        sessionsBeforeLongBreak = sessionsBeforeLongBreak,
        workBreakRatio = workBreakRatio,
    )
}
