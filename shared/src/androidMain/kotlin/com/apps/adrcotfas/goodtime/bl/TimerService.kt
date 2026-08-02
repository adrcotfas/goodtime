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
import android.widget.Toast
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.di.MAIN_SCOPE
import com.apps.adrcotfas.goodtime.di.injectLogger
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.main_no_break_budget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimerService :
    Service(),
    KoinComponent {
    private val notificationManager: NotificationArchManager by inject()
    private val timerManager: TimerManager by inject()
    private val timeProvider: TimeProvider by inject()

    private val coroutineScope: CoroutineScope by inject((named(MAIN_SCOPE)))
    private val log: Logger by injectLogger("TimerService")

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent == null || intent.action == null) {
            // sticky restart after the process was killed: the old foreground notification
            // was kept by the system and is now orphaned (its chronometer keeps ticking in
            // SystemUI); reconcile with the restored timer state
            log.w { "onStartCommand: restarted after process death, reconciling" }
            reconcileAfterProcessDeath()
            return START_STICKY
        }
        val data = timerManager.timerData.value
        log.v { "onStartCommand: ${intent.action}" }
        when (intent.action) {
            Action.StartOrUpdate.name -> {
                coroutineScope.launch {
                    startInProgressForeground(data)
                }
            }

            Action.Reset.name -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            Action.Finished.name -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            // actions triggered from the notification itself; a tap can cold-start the
            // process (e.g. on the finished notification surviving a kill during overtime),
            // so wait for the restored state before acting
            Action.Toggle.name -> withReadyTimer { it.toggle() }

            Action.AddOneMinute.name -> withReadyTimer { it.addOneMinute() }

            Action.Skip.name ->
                withReadyTimer {
                    // mirrors the guard in TimerManager.nextInternal, which silently ignores the skip
                    val timerData = it.timerData.value
                    val noBreakBudget =
                        timerData.runtime.type.isFocus &&
                            !timerData.getTimerProfile().isCountdown &&
                            timerData.getBreakBudget(timeProvider.elapsedRealtime()) < 1.minutes
                    if (noBreakBudget) {
                        Toast
                            .makeText(
                                this,
                                getString(Res.string.main_no_break_budget),
                                Toast.LENGTH_SHORT,
                            ).show()
                    } else {
                        it.next(actionType = FinishActionType.MANUAL_SKIP)
                    }
                }

            Action.Next.name -> withReadyTimer { it.next(actionType = FinishActionType.MANUAL_NEXT) }

            Action.DoReset.name -> withReadyTimer { it.reset() }
        }

        return START_STICKY
    }

    private suspend fun startInProgressForeground(data: DomainTimerData) {
        try {
            startForeground(
                NotificationArchManager.IN_PROGRESS_NOTIFICATION_ID,
                notificationManager.buildInProgressNotification(data),
            )
        } catch (e: IllegalStateException) {
            log.w(e) { "Could not start the service in foreground" }
        }
    }

    private fun withReadyTimer(block: suspend (TimerManager) -> Unit) {
        coroutineScope.launch {
            withTimeoutOrNull(AWAIT_READY_TIMEOUT) { timerManager.awaitReady() }
            block(timerManager)
        }
    }

    private fun reconcileAfterProcessDeath() {
        coroutineScope.launch {
            withTimeoutOrNull(AWAIT_READY_TIMEOUT) { timerManager.awaitReady() }
            val data = timerManager.timerData.value
            if (!data.isReady) {
                // could not load the state in time; leave everything untouched
                return@launch
            }
            val state = data.runtime.state
            when {
                state.isRunning &&
                    data.isCurrentSessionCountdown() &&
                    data.getBaseTime(timeProvider) <= 0 -> {
                    log.i { "countdown expired while the process was dead, finishing it" }
                    timerManager.finish(actionType = FinishActionType.FORCE_FINISH)
                }

                state.isActive -> {
                    log.i { "re-adopting the in-progress notification" }
                    startInProgressForeground(data)
                    // re-arm the alarm in case it was lost along with the process
                    timerManager.onSendToBackground()
                }

                else -> {
                    // nothing to resume; a finished notification, if any, is left untouched
                    notificationManager.clearInProgressNotification()
                    stopSelf()
                }
            }
        }
    }

    companion object {
        // stay well under the system's patience for a restarted service
        private val AWAIT_READY_TIMEOUT = 8.seconds

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

        fun createIntentWithAction(
            context: Context,
            action: Action,
        ): Intent = Intent(context, TimerService::class.java).setAction(action.name)
    }
}
