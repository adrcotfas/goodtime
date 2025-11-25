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
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

// TODO:
// To enable background session reset on iOS, add the following to your `Info.plist`:
//
// ```xml
// <key>UIBackgroundModes</key>
// <array>
// <string>processing</string>
// </array>
//
// <key>BGTaskSchedulerPermittedIdentifiers</key>
// <array>
// <string>com.apps.adrcotfas.goodtime.sessionreset</string>
// </array>

@OptIn(ExperimentalForeignApi::class)
class IosSessionResetHandler(
    log: Logger,
) : SessionResetHandler(log),
    KoinComponent {
    private val timerManager: TimerManager by inject()

    init {
        registerBackgroundTaskHandler()
    }

    private fun registerBackgroundTaskHandler() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            TASK_IDENTIFIER,
            usingQueue = null,
        ) { task ->
            val processingTask = task as BGProcessingTask
            log.d { "Executing background reset task" }
            timerManager.reset(actionType = FinishActionType.MANUAL_DO_NOTHING)
            processingTask.setTaskCompletedWithSuccess(true)
        }
        log.d { "Background task handler registered" }
    }

    override fun scheduleReset() {
        log.d { "Scheduling session reset after delay" }
        cancel()

        val request = BGProcessingTaskRequest(TASK_IDENTIFIER)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(30.0 * 60.0) // 30 minutes
        request.requiresNetworkConnectivity = false
        request.requiresExternalPower = false

        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
            log.d { "Session reset task scheduled" }
        } catch (e: Exception) {
            log.e(e) { "Failed to schedule session reset task" }
        }
    }

    override fun cancel() {
        log.d { "Canceling session reset task" }
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(TASK_IDENTIFIER)
    }

    companion object {
        const val TASK_IDENTIFIER = "com.apps.adrcotfas.goodtime.sessionreset"
    }
}
