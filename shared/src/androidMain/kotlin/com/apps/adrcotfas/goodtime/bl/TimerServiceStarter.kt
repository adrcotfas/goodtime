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
import android.content.Intent
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.TimerService.Companion.Action
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager

class TimerServiceStarter(
    private val context: Context,
    private val notificationManager: NotificationArchManager,
    private val log: Logger,
) : EventListener {
    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start, is Event.Pause, is Event.AddOneMinute, is Event.UpdateActiveLabel -> startService()
            is Event.Reset -> startService(Action.Reset)
            is Event.Finished -> startServiceWithFinished(event.autostartNextSession, event.type)
            else -> {}
        }
    }

    private fun startService(action: Action = Action.StartOrUpdate) {
        start(
            TimerService.createIntentWithAction(
                context,
                action,
            ),
            action,
        )
    }

    private fun startServiceWithFinished(
        autoStart: Boolean,
        type: TimerType,
    ) {
        start(
            TimerService.createFinishEvent(
                context,
                autoStart,
                type,
            ),
            Action.Finished,
        )
    }

    private fun start(
        intent: Intent,
        action: Action,
    ) {
        try {
            context.startService(intent)
        } catch (e: IllegalStateException) {
            // Android 12+ rejects background service starts; this can happen for instance when
            // the timer is reset while the screen is off. A reset only needs the notifications
            // gone and the service is already stopped in that case, so do it here.
            log.w(e) { "Could not start the service for $action" }
            if (action == Action.Reset) {
                notificationManager.clearFinishedNotification()
                notificationManager.clearInProgressNotification()
            }
        }
    }
}
