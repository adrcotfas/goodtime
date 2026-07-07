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
import com.apps.adrcotfas.goodtime.di.MAIN_SCOPE
import com.apps.adrcotfas.goodtime.di.injectLogger
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
    private val scope: CoroutineScope by inject(named(MAIN_SCOPE))
    private val logger by injectLogger(TAG)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == null) return
        try {
            if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
                logger.d("onBootComplete")
                scope.launch {
                    reminderManager.rescheduleAllReminders()
                }
            }
        } catch (e: RuntimeException) {
            logger.e("Could not process intent")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
