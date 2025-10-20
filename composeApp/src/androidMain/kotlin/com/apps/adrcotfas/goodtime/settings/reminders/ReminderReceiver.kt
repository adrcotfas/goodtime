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
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.di.injectLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val notificationManager: NotificationArchManager by inject()
    private val logger by injectLogger(TAG)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        logger.d("onReceive")
        notificationManager.notifyReminder()
    }

    companion object {
        private const val TAG = "ReminderReceiver"
    }
}
