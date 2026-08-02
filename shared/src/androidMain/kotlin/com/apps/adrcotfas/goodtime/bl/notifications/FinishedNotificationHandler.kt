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
package com.apps.adrcotfas.goodtime.bl.notifications

import com.apps.adrcotfas.goodtime.bl.DomainTimerData
import com.apps.adrcotfas.goodtime.bl.Event
import com.apps.adrcotfas.goodtime.bl.EventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns the finished notification in-process so it does not depend on a service start,
 * which Android 12+ can deny in the background. TimerService keeps only the
 * foreground lifecycle.
 */
class FinishedNotificationHandler(
    private val notificationManager: NotificationArchManager,
    private val timerData: () -> DomainTimerData,
    private val coroutineScope: CoroutineScope,
) : EventListener {
    private var pendingNotificationJob: Job? = null

    override fun onEvent(event: Event) {
        when (event) {
            is Event.Finished -> {
                if (!event.autostartNextSession) {
                    // a no-op while the foreground service owns the notification;
                    // removes the orphan when the service start was denied
                    notificationManager.clearInProgressNotification()
                }
                pendingNotificationJob =
                    coroutineScope.launch {
                        notificationManager.notifyFinished(
                            finishedType = event.type,
                            data = timerData(),
                            withActions = !event.autostartNextSession,
                        )
                    }
            }

            is Event.Start, Event.Reset -> {
                val clearInProgress = event is Event.Reset
                coroutineScope.launch {
                    // wait for a pending finished notification to post before clearing
                    pendingNotificationJob?.join()
                    notificationManager.clearFinishedNotification()
                    if (clearInProgress) {
                        notificationManager.clearInProgressNotification()
                    }
                }
            }

            else -> {}
        }
    }
}
