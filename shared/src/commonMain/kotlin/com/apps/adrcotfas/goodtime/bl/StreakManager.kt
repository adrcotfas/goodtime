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
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.streakInUse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes

/**
 * Manages the long break streak logic.
 */
class StreakManager(
    private val settingsRepo: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val log: Logger,
    private val coroutineScope: CoroutineScope,
) {

    val longBreakData: StateFlow<LongBreakData> = settingsRepo.settings
        .map { it.longBreakData }
        .stateIn(coroutineScope, SharingStarted.Eagerly, LongBreakData())

    fun incrementStreak() {
        val lastWorkEndTime = timeProvider.elapsedRealtime()
        val newStreak = longBreakData.value.streak + 1
        val newData = LongBreakData(newStreak, lastWorkEndTime)
        coroutineScope.launch {
            settingsRepo.setLongBreakData(newData)
        }
        log.v { "Streak incremented: $newStreak" }
    }

    fun resetStreakIfNeeded(timerProfile: TimerProfile, millis: Long = timeProvider.elapsedRealtime()) {
        log.v { "resetStreakIfNeeded check" }
        if (!didLastWorkSessionFinishRecently(timerProfile, millis)) {
            log.v { "Resetting long break data due to inactivity" }
            coroutineScope.launch {
                settingsRepo.setLongBreakData(LongBreakData())
            }
        }
    }

    fun shouldConsiderStreak(timerProfile: TimerProfile, workEndTime: Long): Boolean {
        if (!timerProfile.isCountdown || !timerProfile.isLongBreakEnabled) return false

        val streakForLongBreakIsReached =
            (longBreakData.value.streakInUse(timerProfile.sessionsBeforeLongBreak) == 0)
        return streakForLongBreakIsReached && didLastWorkSessionFinishRecently(
            timerProfile,
            workEndTime,
        )
    }

    private fun didLastWorkSessionFinishRecently(timerProfile: TimerProfile, workEndTime: Long): Boolean {
        if (!timerProfile.isCountdown) return false

        val currentData = longBreakData.value
        val maxIdleTime = timerProfile.workDuration.minutes.inWholeMilliseconds +
            timerProfile.breakDuration.minutes.inWholeMilliseconds +
            30.minutes.inWholeMilliseconds // Consider making this configurable or a constant

        return currentData.lastWorkEndTime != 0L && max(
            0,
            workEndTime - currentData.lastWorkEndTime,
        ) < maxIdleTime
    }
}
