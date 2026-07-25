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
import com.apps.adrcotfas.goodtime.data.settings.BreakBudgetData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Owns the count-up profile break budget: accumulation snapshots and persistence.
 * The current value lives in [DomainTimerData.breakBudgetData]; TimerManager applies
 * what this class computes.
 */
class BreakBudgetManager(
    private val settingsRepo: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val coroutineScope: CoroutineScope,
    private val log: Logger,
) {
    /**
     * Reads the persisted budget, resetting its start time if the device was rebooted
     * (elapsedRealtime restarted below the persisted start).
     */
    suspend fun initialBreakBudget(): BreakBudgetData {
        val persisted =
            settingsRepo.settings
                .map { it.breakBudgetData }
                .first()
        val elapsedRealtime = timeProvider.elapsedRealtime()
        val deviceWasRestarted = elapsedRealtime < persisted.breakBudgetStart
        return if (deviceWasRestarted) {
            val breakBudget = persisted.copy(breakBudgetStart = elapsedRealtime)
            settingsRepo.setBreakBudgetData(breakBudget)
            breakBudget
        } else {
            persisted
        }
    }

    /**
     * Snapshots the current budget for count-up profiles and persists it.
     * Returns null for countdown profiles (no budget to update).
     */
    fun updatedBreakBudget(data: DomainTimerData): BreakBudgetData? {
        if (data.label.isCountdown) return null
        val elapsedRealtime = timeProvider.elapsedRealtime()
        val breakBudget = data.getBreakBudget(elapsedRealtime)
        log.v { "Persisting break budget: $breakBudget" }
        val newData =
            BreakBudgetData(
                breakBudget = breakBudget,
                breakBudgetStart = elapsedRealtime,
            )
        coroutineScope.launch {
            settingsRepo.setBreakBudgetData(newData)
        }
        return newData
    }

    /** Clears the budget. Callers must also restart accrual, see [TimerManager.resetBreakBudget]. */
    fun resetBreakBudget(): BreakBudgetData {
        val newData = BreakBudgetData(breakBudgetStart = timeProvider.elapsedRealtime())
        log.i { "Resetting break budget" }
        coroutineScope.launch {
            settingsRepo.setBreakBudgetData(newData)
        }
        return newData
    }
}
