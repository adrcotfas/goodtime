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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone

actual class ReminderScheduler(
    private val context: Context,
    private val timeProvider: TimeProvider,
    private val logger: Logger,
) {
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val pendingIntents = mutableMapOf<String, PendingIntent>()

    actual suspend fun scheduleWeeklyReminder(
        dayOfWeek: DayOfWeek,
        secondOfDay: Int,
        identifier: String,
    ) {
        val triggerMillis =
            ReminderTimeCalculator.calculateNextReminderTime(
                currentTimeMillis = timeProvider.now(),
                reminderDay = dayOfWeek,
                secondOfDay = secondOfDay,
                timeZone = TimeZone.currentSystemDefault(),
            )

        val pendingIntent = getOrCreatePendingIntent(identifier)

        logger.d("Scheduling reminder at: $triggerMillis for $dayOfWeek")
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent,
        )
    }

    actual fun cancelAllReminders() {
        logger.d("Cancelling all reminders")
        pendingIntents.values.forEach { alarmManager.cancel(it) }
        pendingIntents.clear()
    }

    private fun getOrCreatePendingIntent(identifier: String): PendingIntent =
        pendingIntents.getOrPut(identifier) {
            val intent =
                Intent(context, ReminderReceiver::class.java).apply {
                    action = REMINDER_ACTION
                    putExtra(EXTRA_REMINDER_ID, identifier)
                }
            PendingIntent.getBroadcast(
                context,
                identifier.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

    companion object {
        const val REMINDER_ACTION = "goodtime.reminder_action"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
