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
import com.apps.adrcotfas.goodtime.data.settings.BreakBudgetData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Manages the break budget logic for count-up timers.
 */
class BreakBudgetManager(
    private val settingsRepo: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val log: Logger,
    private val coroutineScope: CoroutineScope,
) {

    val breakBudgetData: StateFlow<BreakBudgetData> = settingsRepo.settings
        .map { it.breakBudgetData }
        .stateIn(coroutineScope, SharingStarted.Eagerly, BreakBudgetData())

    /**
     * Calculates the current break budget based on the timer state and profile.
     * For countdown timers, the budget is always zero.
     * For count-up timers:
     * - If working and running: Calculates earned budget based on elapsed time and ratio, adds to existing budget.
     * - If working but paused/reset: Calculates remaining budget considering idle time decay.
     * - If breaking: Calculates remaining budget considering idle time decay.
     */
    fun getCurrentBreakBudget(
        timerType: TimerType,
        timerState: TimerState,
        timerProfile: TimerProfile,
        lastStartTime: Long, // Relevant only when state is RUNNING and type is WORK
        elapsedRealtime: Long = timeProvider.elapsedRealtime(),
    ): Duration {
        if (timerProfile.isCountdown) return 0.minutes

        val currentPersistedData = breakBudgetData.value

        return if (timerType.isWork) {
            when (timerState) {
                TimerState.RUNNING -> {
                    val earnedBudget = (elapsedRealtime - lastStartTime).milliseconds / timerProfile.workBreakRatio
                    val totalBudget = currentPersistedData.breakBudget + earnedBudget
                    if (totalBudget.isNegative()) 0.minutes else totalBudget
                }
                // For PAUSED or RESET work sessions, budget decays like a break
                else -> currentPersistedData.getRemainingBreakBudget(elapsedRealtime)
            }
        } else { // TimerType is BREAK or LONG_BREAK (though budget usually irrelevant for long break)
            currentPersistedData.getRemainingBreakBudget(elapsedRealtime)
        }
    }

    /**
     * Updates and persists the break budget if the current session is count-up work.
     * This should be called when the timer starts, pauses, resumes, or resets.
     * @return The calculated break budget at the time of the update.
     */
    fun updateAndPersistBreakBudget(
        timerType: TimerType,
        timerState: TimerState,
        timerProfile: TimerProfile,
        lastStartTime: Long,
    ): Duration {
        if (timerProfile.isCountdown) return 0.minutes

        val elapsedRealtime = timeProvider.elapsedRealtime()
        val calculatedBudget = getCurrentBreakBudget(timerType, timerState, timerProfile, lastStartTime, elapsedRealtime)

        log.v { "Persisting break budget: $calculatedBudget" }
        val newData = BreakBudgetData(
            breakBudget = calculatedBudget,
            breakBudgetStart = elapsedRealtime, // Reset start time whenever budget is persisted
        )

        // Update the repository, which will eventually update the state flow
        coroutineScope.launch {
            settingsRepo.setBreakBudgetData(newData)
        }
        return calculatedBudget
    }

    /**
     * Gets the currently persisted break budget amount.
     * Useful for determining the duration of a count-up break session.
     */
    fun getPersistedBreakBudgetAmount(): Duration {
        return breakBudgetData.value.breakBudget
    }
}
