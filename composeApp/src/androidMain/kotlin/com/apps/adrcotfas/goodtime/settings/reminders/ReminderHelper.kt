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
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.settings.ProductivityReminderSettings
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class ReminderHelper(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
) {
    private var pendingIntents: Array<PendingIntent?> = arrayOfNulls(7)
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(ALARM_SERVICE) as AlarmManager
    }

    private var reminderSettings = ProductivityReminderSettings()

    suspend fun init() {
        settingsRepository.settings
            .map { it.productivityReminderSettings }
            .distinctUntilChanged()
            .collect { settings ->
                reminderSettings = settings
                scheduleNotifications()
            }
    }

    fun scheduleNotifications() {
        logger.d("scheduleNotifications")
        cancelNotifications()
        val enabledDays = reminderSettings.days.map { DayOfWeek.of(it) }
        enabledDays.forEach { day ->
            scheduleNotification(day, reminderSettings.secondOfDay)
        }
    }

    private fun getReminderPendingIntent(index: Int): PendingIntent {
        if (pendingIntents[index] == null) {
            val intent = Intent(context, ReminderReceiver::class.java)
            intent.action = REMINDER_ACTION
            pendingIntents[index] =
                PendingIntent.getBroadcast(
                    context,
                    REMINDER_REQUEST_CODE + index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
        }
        return pendingIntents[index]!!
    }

    private fun cancelNotifications() {
        logger.d("cancelNotifications")
        for (day in DayOfWeek.entries) {
            cancelNotification(day)
        }
    }

    private fun cancelNotification(day: DayOfWeek) {
        logger.d("cancelNotification for $day")
        val reminderPendingIntent = getReminderPendingIntent(day.ordinal)
        alarmManager.cancel(reminderPendingIntent)
    }

    private fun scheduleNotification(
        reminderDay: DayOfWeek,
        secondOfDay: Int,
    ) {
        val now = LocalDateTime.now()
        logger.d("now: ${now.toLocalTime()}")

        val time = LocalTime.ofSecondOfDay(secondOfDay.toLong())
        var reminderTime =
            now
                .withHour(time.hour)
                .withMinute(time.minute)
                .withSecond(0)
                .with(TemporalAdjusters.nextOrSame(reminderDay))

        if (reminderTime.isBefore(now)) {
            logger.d("reminderTime is before now; schedule for next week")
            reminderTime = reminderTime.plusWeeks(1)
        }

        logger.d("reminderTime: $reminderTime")

        val reminderMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        logger.d("scheduleNotification at: $reminderMillis")
        // TODO: consider daylight saving and time zone changes
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            reminderMillis,
            AlarmManager.INTERVAL_DAY * 7,
            getReminderPendingIntent(reminderDay.ordinal),
        )
    }

    companion object {
        const val REMINDER_ACTION = "goodtime.reminder_action"
        const val REMINDER_REQUEST_CODE = 11
    }
}
