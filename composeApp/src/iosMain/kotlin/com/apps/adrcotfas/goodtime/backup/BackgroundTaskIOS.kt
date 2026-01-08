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
package com.apps.adrcotfas.goodtime.backup

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.BackgroundTasks.BGAppRefreshTask
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateByAddingTimeInterval

private const val TASK_IDENTIFIER = "com.apps.adrcotfas.goodtime.cloudbackup"
private const val TASK_INTERVAL_HOURS = 24.0

/**
 * Handles iOS background task registration and execution for cloud backups.
 */
object BackgroundTaskHandler : KoinComponent {
    private val cloudBackupManager: CloudBackupManager by inject()
    private val logger: Logger by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Register the background task with iOS.
     * Must be called during app launch (from AppDelegate.didFinishLaunchingWithOptions).
     */
    fun register() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = TASK_IDENTIFIER,
            usingQueue = null,
        ) { task ->
            logger.i { "Cloud backup task started" }
            val appRefreshTask = task as? BGAppRefreshTask

            // Create cancellable job
            val job =
                scope.launch {
                    try {
                        cloudBackupManager.checkAndPerformBackup()
                        logger.i { "Cloud backup completed successfully" }

                        // Schedule the next backup only if auto backup is still enabled (and the user is Pro).
                        if (cloudBackupManager.isAutoBackupEnabledForProUser()) {
                            scheduleCloudBackupTask()
                        } else {
                            logger.i { "Auto backup disabled or user is not Pro; not rescheduling" }
                        }

                        // Mark task as completed
                        appRefreshTask?.setTaskCompletedWithSuccess(true)
                    } catch (e: Exception) {
                        logger.e(e) { "Cloud backup failed" }
                        appRefreshTask?.setTaskCompletedWithSuccess(false)
                    }
                }

            // Handle iOS killing the task (after ~30 seconds)
            task?.expirationHandler = {
                logger.w { "Cloud backup task expired, cancelling" }
                job.cancel()
                appRefreshTask?.setTaskCompletedWithSuccess(false)
            }
        }
        logger.i { "Cloud backup task registered: $TASK_IDENTIFIER" }
    }
}

/**
 * Register the background task with iOS.
 * Must be called during app launch (from AppDelegate.didFinishLaunchingWithOptions).
 */
fun registerCloudBackupTask() {
    BackgroundTaskHandler.register()
}

/**
 * Schedule the next cloud backup task.
 * Should be called after completing a backup or when auto backup is enabled.
 */
@OptIn(ExperimentalForeignApi::class)
fun scheduleCloudBackupTask() {
    val logger = Logger.withTag("BackgroundTaskIOS")
    val request = BGAppRefreshTaskRequest(identifier = TASK_IDENTIFIER)
    // Schedule for approximately 24 hours from now
    request.earliestBeginDate = NSDate().dateByAddingTimeInterval(TASK_INTERVAL_HOURS * 60 * 60)

    try {
        val success = BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        if (success) {
            logger.i { "Cloud backup task scheduled for ~$TASK_INTERVAL_HOURS hours from now" }
        } else {
            logger.e { "Failed to schedule cloud backup task" }
        }
    } catch (e: Exception) {
        logger.e(e) { "Exception scheduling cloud backup task" }
    }
}

/**
 * Cancel the scheduled cloud backup task.
 * Should be called when auto backup is disabled.
 */
fun cancelCloudBackupTask() {
    val logger = Logger.withTag("BackgroundTaskIOS")
    BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(TASK_IDENTIFIER)
    logger.i { "Cloud backup task cancelled" }
}
