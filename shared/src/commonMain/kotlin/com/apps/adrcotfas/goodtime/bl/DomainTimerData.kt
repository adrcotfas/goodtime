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

// BreakBudgetData removed
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.model.duration
import com.apps.adrcotfas.goodtime.data.model.endTime
import com.apps.adrcotfas.goodtime.data.model.getLabelData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class LabelData(
    val name: String,
    val colorIndex: Long,
)

data class DomainLabel(
    val label: Label = Label.defaultLabel(),
    val profile: TimerProfile = TimerProfile(),
) {
    fun getLabelName() = label.name
    val isCountdown = profile.isCountdown
}

fun DomainLabel.getLabelData() = this.label.getLabelData()

/**
 * Data class that captures the current timer state.
 * There can only be one running timer at a time.
 */
data class DomainTimerData(
    val isReady: Boolean = false,
    val label: DomainLabel = DomainLabel(),
    // Removed longBreakData - managed by StreakManager
    // Removed breakBudgetData - managed by BreakBudgetManager

    val startTime: Long = 0, // millis since boot
    val lastStartTime: Long = 0, // millis since boot
    val lastPauseTime: Long = 0, // millis since boot
    val endTime: Long = 0, // millis since boot
    val timeAtPause: Long = 0, // millis
    val state: TimerState = TimerState.RESET,
    val type: TimerType = TimerType.WORK,
    val timeSpentPaused: Long = 0, // millis spent in pause
    val completedMinutes: Long = 0, // minutes
) {
    fun reset() = DomainTimerData(
        isReady = isReady,
        label = label,
        state = TimerState.RESET,
        // Removed longBreakData
        // Removed breakBudgetData
    )

    fun getTimerProfile(): TimerProfile {
        return label.profile
    }

    fun getLabelName(): String {
        return label.getLabelName()
    }

    fun inUseSessionsBeforeLongBreak(): Int {
        val profile = label.profile
        return if (profile.isCountdown && profile.isBreakEnabled && profile.isLongBreakEnabled) {
            profile.sessionsBeforeLongBreak
        } else {
            0
        }
    }

    fun getEndTime(timerType: TimerType, elapsedRealtime: Long, breakBudgetDuration: Duration): Long {
        return if (getTimerProfile().isCountdown) {
            getTimerProfile().endTime(timerType, elapsedRealtime)
        } else if (timerType.isBreak) {
            elapsedRealtime + breakBudgetDuration.inWholeMilliseconds
        } else {
            0 // Count-up work has no predefined end time based on duration
        }
    }

    fun isDefaultLabel() = label.getLabelName() == Label.DEFAULT_LABEL_NAME

    // Removed getBreakBudget - logic moved to BreakBudgetManager

    fun isCurrentSessionCountdown(): Boolean {
        // Simplified: Count-up work is the only non-countdown session type relevant here
        return getTimerProfile().isCountdown || type.isBreak
    }
}

enum class TimerState {
    RESET, RUNNING, PAUSED, FINISHED
}

val TimerState.isRunning: Boolean
    get() = this == TimerState.RUNNING

val TimerState.isPaused: Boolean
    get() = this == TimerState.PAUSED

val TimerState.isActive: Boolean
    get() = isRunning || isPaused

val TimerState.isFinished: Boolean
    get() = this == TimerState.FINISHED

val TimerState.isReset: Boolean
    get() = this == TimerState.RESET

enum class TimerType {
    WORK, BREAK, LONG_BREAK
}

val TimerType.isBreak: Boolean
    get() = this != TimerType.WORK

val TimerType.isWork: Boolean
    get() = this == TimerType.WORK

fun DomainTimerData.getBaseTime(timerProvider: TimeProvider): Long {
    val countdown = label.profile.isCountdown

    if (state == TimerState.RESET) {
        return if (countdown) {
            label.profile.duration(type).minutes.inWholeMilliseconds
        } else {
            0
        }
    } else if (state == TimerState.PAUSED) {
        return timeAtPause
    }

    return if (countdown || (!countdown && type.isBreak)) {
        endTime - timerProvider.elapsedRealtime()
    } else {
        timerProvider.elapsedRealtime() - startTime - timeSpentPaused
    }
}
