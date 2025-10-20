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

import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.model.duration
import com.apps.adrcotfas.goodtime.data.model.endTime
import com.apps.adrcotfas.goodtime.data.model.getLabelData
import com.apps.adrcotfas.goodtime.data.model.isDefault
import com.apps.adrcotfas.goodtime.data.settings.BreakBudgetData
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

data class LabelData(
    val name: String,
    val colorIndex: Long,
)

fun LabelData.isDefault(): Boolean = name == Label.DEFAULT_LABEL_NAME

data class DomainLabel(
    val label: Label = Label.defaultLabel(),
    val profile: TimerProfile = TimerProfile(),
) {
    fun getLabelName() = label.name

    fun isDefault() = label.isDefault()

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
    /**
     * This is persisted in the settings too for cases where the app is killed.
     * I don't like updating the data in two places(see [TimerManager.incrementStreak]
     * and [TimerManager.resetStreakIfNeeded]) but I don't want to risk invalid data
     *  when updating the repository and possibly waiting for the flow to emit the new data
     *  while executing the next command.
     */
    val longBreakData: LongBreakData = LongBreakData(),
    val breakBudgetData: BreakBudgetData = BreakBudgetData(),
    /**
     * Bellow we have the dynamic data that is not stored in persistent storage.
     */
    val startTime: Long = 0, // millis since boot
    val lastStartTime: Long = 0, // millis since boot
    val lastPauseTime: Long = 0, // millis since boot
    val endTime: Long = 0, // millis since boot
    val timeAtPause: Long = 0, // millis
    val state: TimerState = TimerState.RESET,
    val type: TimerType = TimerType.FOCUS,
    val timeSpentPaused: Long = 0, // millis spent in pause
    val completedMinutes: Long = 0, // minutes
) {
    fun reset() =
        DomainTimerData(
            isReady = isReady,
            label = label,
            state = TimerState.RESET,
            longBreakData = longBreakData,
            breakBudgetData = breakBudgetData,
        )

    fun getTimerProfile(): TimerProfile = label.profile

    fun getLabelName(): String = label.getLabelName()

    fun inUseSessionsBeforeLongBreak(): Int {
        val profile = label.profile
        return if (profile.isCountdown && profile.isBreakEnabled && profile.isLongBreakEnabled) {
            profile.sessionsBeforeLongBreak
        } else {
            0
        }
    }

    fun getEndTime(
        timerType: TimerType,
        elapsedRealtime: Long,
    ): Long =
        if (getTimerProfile().isCountdown) {
            getTimerProfile().endTime(timerType, elapsedRealtime)
        } else if (timerType.isBreak) {
            val breakBudget = breakBudgetData.breakBudget.inWholeMilliseconds
            elapsedRealtime + breakBudget
        } else {
            0
        }

    fun isDefaultLabel() = label.getLabelName() == Label.DEFAULT_LABEL_NAME

    fun getBreakBudget(elapsedRealtime: Long): Duration {
        if (label.profile.isCountdown) return 0.minutes
        return if (type.isFocus) {
            when (state) {
                TimerState.RUNNING -> {
                    val breakBudgetMillis = breakBudgetData.breakBudget
                    val workBreakRatio = label.profile.workBreakRatio
                    (
                        (
                            (elapsedRealtime - lastStartTime).milliseconds /
                                workBreakRatio
                        ) + breakBudgetMillis
                    ).let {
                        if (it.isNegative()) 0.minutes else it
                    }
                }
                else -> breakBudgetData.getRemainingBreakBudget(elapsedRealtime)
            }
        } else {
            breakBudgetData.getRemainingBreakBudget(elapsedRealtime)
        }
    }

    fun isCurrentSessionCountdown(): Boolean = getTimerProfile().isCountdown || type != TimerType.FOCUS
}

enum class TimerState {
    RESET,
    RUNNING,
    PAUSED,
    FINISHED,
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
    FOCUS,
    BREAK,
    LONG_BREAK,
}

val TimerType.isBreak: Boolean
    get() = this != TimerType.FOCUS

val TimerType.isFocus: Boolean
    get() = this == TimerType.FOCUS

fun DomainTimerData.getBaseTime(timerProvider: TimeProvider): Long {
    val countdown = label.profile.isCountdown

    if (state == TimerState.RESET) {
        return if (countdown) {
            label.profile
                .duration(type)
                .minutes.inWholeMilliseconds
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
