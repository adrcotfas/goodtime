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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground/background coordination that shouldn't live in UI viewmodels.
 *
 * Driven by the UI layer via `LifecycleResumeEffect`, but depends only on business logic.
 */
class TimerForegroundMonitor(
    private val timerManager: TimerManager,
    private val timeProvider: TimeProvider,
) {
    private var foregroundJob: Job? = null

    fun onEnterForeground(scope: CoroutineScope) {
        timerManager.onBringToForeground()
        foregroundJob?.cancel()
        foregroundJob =
            scope.launch {
                listenForeground()
            }
    }

    fun onExitForeground() {
        timerManager.onSendToBackground()
        foregroundJob?.cancel()
        foregroundJob = null
    }

    private suspend fun listenForeground() {
        timerManager.timerData
            .filter { it.state.isActive }
            .collectLatest { activeTimerData ->
                while (currentCoroutineContext().isActive && timerManager.timerData.value.state.isActive) {
                    val isCountdown = activeTimerData.isCurrentSessionCountdown()
                    val baseTime = activeTimerData.getBaseTime(timeProvider)

                    if (isCountdown && baseTime < 500) {
                        timerManager.finish(actionType = FinishActionType.FORCE_FINISH)
                        return@collectLatest
                    } else if (!isCountdown && baseTime > TimerManager.COUNT_UP_HARD_LIMIT) {
                        timerManager.reset(actionType = FinishActionType.MANUAL_RESET)
                        return@collectLatest
                    }

                    delay(1000)
                }
            }
    }
}
