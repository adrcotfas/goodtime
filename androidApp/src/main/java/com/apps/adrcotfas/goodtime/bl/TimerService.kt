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

import android.app.Service
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.di.injectLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TimerService :
    Service(),
    KoinComponent {
    private val notificationManager: NotificationArchManager by inject()
    private val timerManager: TimerManager by inject()
    private val log: Logger by injectLogger("TimerService")

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent == null || intent.action == null) {
            log.w { "onStartCommand: intent or action is null" }
            // TODO: This is true when the service is killed after revoking the SCHEDULE_EXACT_ALARM permission
            //      the notification remains active but TimerManager is reset
            // TODO: Investigate if we have other cases when this happens
            return START_NOT_STICKY
        }
        val data = timerManager.timerData.value
        log.v { "onStartCommand: ${intent.action}" }
        when (intent.action) {
            Action.StartOrUpdate.name -> {
                notificationManager.clearFinishedNotification()
                startForeground(
                    NotificationArchManager.IN_PROGRESS_NOTIFICATION_ID,
                    notificationManager.buildInProgressNotification(data),
                )
            }

            Action.Reset.name -> {
                notificationManager.clearFinishedNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            Action.Finished.name -> {
                val autoStart = intent.getBooleanExtra(EXTRA_FINISHED_AUTOSTART, false)
                if (!autoStart) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                notificationManager.notifyFinished(data, withActions = !autoStart)
                return START_NOT_STICKY
            }

            // actions triggered from the notification itself
            Action.Toggle.name -> timerManager.toggle()
            Action.AddOneMinute.name -> timerManager.addOneMinute()
            Action.Skip.name -> timerManager.next(finishActionType = FinishActionType.MANUAL_SKIP)
            Action.Next.name -> timerManager.next(finishActionType = FinishActionType.MANUAL_NEXT)
            Action.DoReset.name -> timerManager.reset()
        }

        return START_STICKY
    }

    companion object {
        enum class Action {
            StartOrUpdate,
            Reset,
            Finished,

            // actions triggered from the notification itself
            Toggle,
            AddOneMinute,
            Skip,
            Next,
            DoReset,
        }

        private const val EXTRA_FINISHED_AUTOSTART = "EXTRA_FINISHED_AUTOSTART"

        fun createIntentWithAction(
            context: Context,
            action: Action,
        ): Intent = Intent(context, TimerService::class.java).setAction(action.name)

        fun createFinishEvent(
            context: Context,
            autostart: Boolean = false,
        ): Intent =
            Intent(context, TimerService::class.java).apply {
                action = Action.Finished.name
                putExtra(EXTRA_FINISHED_AUTOSTART, autostart)
            }
    }
}
