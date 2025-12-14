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

import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.settings.ProductivityReminderSettings
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek

/**
 * Common business logic for managing productivity reminders.
 * Observes reminder settings and schedules notifications across platforms.
 */
class ReminderManager(
    private val scheduler: ReminderScheduler,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
) {
    private var currentSettings = ProductivityReminderSettings()

    suspend fun init() {
        logger.d("init")
        settingsRepository.settings
            .map { it.productivityReminderSettings }
            .distinctUntilChanged()
            .collect { settings ->
                currentSettings = settings
                rescheduleAllReminders()
            }
    }

    suspend fun rescheduleAllReminders() {
        logger.d("rescheduleAllReminders")
        scheduler.cancelAllReminders()

        if (currentSettings.days.isEmpty()) {
            logger.d("No reminder days configured")
            return
        }

        currentSettings.days.forEach { dayNumber ->
            val day = DayOfWeek(dayNumber)
            val identifier = "reminder_day_$dayNumber"
            logger.d("Scheduling reminder for $day with identifier: $identifier")
            scheduler.scheduleWeeklyReminder(
                dayOfWeek = day,
                secondOfDay = currentSettings.secondOfDay,
                identifier = identifier,
            )
        }
    }
}
