/**
 *     Goodtime Productivity
 *     Copyright (C) Adrian Cotfas
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
import com.apps.adrcotfas.goodtime.bl.TimerManager
import com.apps.adrcotfas.goodtime.bl.TimerServiceStarter
import com.apps.adrcotfas.goodtime.bl.isActive
import com.apps.adrcotfas.goodtime.di.MAIN_SCOPE
import com.apps.adrcotfas.goodtime.di.injectLogger
import com.apps.adrcotfas.goodtime.settings.restoreGoodtimeLauncherIfNeeded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.lang.RuntimeException

class BootReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val reminderManager: ReminderManager by inject()
    private val timerManager: TimerManager by inject()
    private val timerServiceStarter: TimerServiceStarter by inject()
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
        restoreGoodtimeLauncherIfNeeded(context)
        val pendingResult = goAsync()
        scope.launch {
            try {
                restoreTimer()
                reminderManager.rescheduleAllReminders()
            } catch (e: RuntimeException) {
                logger.e(e) { "Could not process intent" }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * The reboot or app update wiped the alarm and the foreground service; the persisted
     * timer state was restored by [TimerManager]. Catch up on an expiry and re-establish the alarm and the
     * in-progress notification for a timer that is still running.
     */
    private suspend fun restoreTimer() {
        timerManager.reconcile()
        // the app is in the background: this arms the alarm for a timer that is
        // still running (or was just auto-started by a finish above)
        timerManager.onSendToBackground()
        if (timerManager.timerData.value.runtime.state.isActive) {
            timerServiceStarter.startService()
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
