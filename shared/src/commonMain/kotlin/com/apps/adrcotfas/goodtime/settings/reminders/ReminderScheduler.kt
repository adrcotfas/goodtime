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

import kotlinx.datetime.DayOfWeek

/**
 * Platform-specific reminder scheduler.
 * Android: Uses AlarmManager
 * iOS: Uses UNUserNotificationCenter
 */
expect class ReminderScheduler {
    /**
     * Schedules a weekly repeating reminder.
     *
     * @param dayOfWeek The day of the week for the reminder
     * @param secondOfDay The time of day in seconds (0-86399)
     * @param identifier Unique identifier for this reminder
     */
    suspend fun scheduleWeeklyReminder(
        dayOfWeek: DayOfWeek,
        secondOfDay: Int,
        identifier: String,
    )

    /**
     * Cancels all scheduled reminders.
     */
    fun cancelAllReminders()
}
