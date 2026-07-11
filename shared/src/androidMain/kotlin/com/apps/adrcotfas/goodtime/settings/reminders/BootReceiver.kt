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
package com.apps.adrcotfas.goodtime.settings.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apps.adrcotfas.goodtime.bl.FinishActionType
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.TimerManager
import com.apps.adrcotfas.goodtime.bl.TimerService
import com.apps.adrcotfas.goodtime.bl.TimerService.Companion.Action
import com.apps.adrcotfas.goodtime.bl.getBaseTime
import com.apps.adrcotfas.goodtime.bl.isActive
import com.apps.adrcotfas.goodtime.bl.isRunning
import com.apps.adrcotfas.goodtime.di.MAIN_SCOPE
import com.apps.adrcotfas.goodtime.di.injectLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.lang.RuntimeException
import kotlin.time.Duration.Companion.seconds

class BootReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val reminderManager: ReminderManager by inject()
    private val timerManager: TimerManager by inject()
    private val timeProvider: TimeProvider by inject()
    private val scope: CoroutineScope by inject(named(MAIN_SCOPE))
    private val logger by injectLogger(TAG)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // both wipe the alarm and the notifications: reboot clears everything,
        // an app update force-stops the old process
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        logger.d("onReceive: ${intent.action}")
        val pendingResult = goAsync()
        scope.launch {
            try {
                restoreTimer(context)
                reminderManager.rescheduleAllReminders()
            } catch (e: RuntimeException) {
                logger.e("Could not process intent")
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * The reboot or app update wiped the alarm and the foreground service; the persisted
     * timer state was restored by [TimerManager]. Finish an expired countdown (saving the
     * session and showing the finished notification) and re-establish the alarm and the
     * in-progress notification for a timer that is still running.
     */
    private suspend fun restoreTimer(context: Context) {
        withTimeoutOrNull(AWAIT_READY_TIMEOUT) { timerManager.awaitReady() }

        val data = timerManager.timerData.value
        if (data.runtime.state.isRunning &&
            data.isCurrentSessionCountdown() &&
            data.getBaseTime(timeProvider) <= 0
        ) {
            logger.d("countdown expired while the device was off, finishing it")
            timerManager.finish(actionType = FinishActionType.FORCE_FINISH)
        }

        // the app is in the background: this arms the alarm for a timer that is
        // still running (or was just auto-started by the finish above)
        timerManager.onSendToBackground()
        if (timerManager.timerData.value.runtime.state.isActive) {
            context.startService(TimerService.createIntentWithAction(context, Action.StartOrUpdate))
        }
    }

    companion object {
        private const val TAG = "BootReceiver"

        // stay under the ~10s broadcast ANR limit
        private val AWAIT_READY_TIMEOUT = 8.seconds
    }
}
