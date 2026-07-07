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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes

/**
 * Long-break streak policy and persistence. The current value lives in
 * [DomainTimerData.longBreakData]; TimerManager applies what this class computes.
 */
class StreakManager(
    private val settingsRepo: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val coroutineScope: CoroutineScope,
    private val log: Logger,
) {
    suspend fun initialLongBreakData(): LongBreakData =
        settingsRepo.settings
            .map { it.longBreakData }
            .first()

    fun incrementStreak(current: LongBreakData): LongBreakData {
        val newData = LongBreakData(current.streak + 1, lastWorkEndTime = timeProvider.elapsedRealtime())
        coroutineScope.launch {
            settingsRepo.setLongBreakData(newData)
        }
        log.v { "Streak incremented: ${newData.streak}" }
        return newData
    }

    /**
     * Returns the reset data if too much time passed since the last work session, else null.
     */
    fun resetStreakIfNeeded(
        current: LongBreakData,
        profile: TimerProfile,
        millis: Long = timeProvider.elapsedRealtime(),
    ): LongBreakData? {
        log.v { "resetStreakIfNeeded" }
        if (didLastWorkSessionFinishRecently(current, profile, millis)) return null
        log.v { "reset long break data" }
        coroutineScope.launch {
            settingsRepo.setLongBreakData(LongBreakData())
        }
        return LongBreakData()
    }

    fun shouldConsiderStreak(
        current: LongBreakData,
        profile: TimerProfile,
        workEndTime: Long,
    ): Boolean {
        if (!profile.isCountdown || !profile.isLongBreakEnabled) return false

        val streakForLongBreakIsReached = current.streakInUse(profile.sessionsBeforeLongBreak) == 0
        return streakForLongBreakIsReached && didLastWorkSessionFinishRecently(current, profile, workEndTime)
    }

    fun didLastWorkSessionFinishRecently(
        current: LongBreakData,
        profile: TimerProfile,
        workEndTime: Long,
    ): Boolean {
        if (!profile.isCountdown) return false

        val maxIdleTime =
            profile.workDuration.minutes.inWholeMilliseconds +
                profile.breakDuration.minutes.inWholeMilliseconds +
                30.minutes.inWholeMilliseconds
        return current.lastWorkEndTime != 0L &&
            max(0, workEndTime - current.lastWorkEndTime) < maxIdleTime
    }
}
