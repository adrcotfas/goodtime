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

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.TimeUnit

class SessionResetHandler(
    private val context: Context,
    private val log: Logger,
) : EventListener {
    override fun onEvent(event: Event) {
        when (event) {
            is Event.Finished -> scheduleReset()
            is Event.SendToBackground, Event.BringToForeground -> {
            }
            else -> cancel()
        }
    }

    private fun scheduleReset() {
        log.d { "Resetting the session after delay" }
        val uploadWorkRequest =
            OneTimeWorkRequestBuilder<ResetWorker>()
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_ID,
            ExistingWorkPolicy.REPLACE,
            uploadWorkRequest,
        )
    }

    fun cancel() {
        log.d { "Canceling the reset worker" }
        WorkManager.getInstance(context).cancelUniqueWork(WORK_ID)
    }

    companion object {
        const val WORK_ID = "resetWorker"
    }
}

class ResetWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    private val timerManager: TimerManager by inject(TimerManager::class.java)

    override fun doWork(): Result {
        timerManager.reset(actionType = FinishActionType.MANUAL_DO_NOTHING)
        return Result.success()
    }
}
