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
import com.apps.adrcotfas.goodtime.settings.reminders.ReminderManager.Companion.REMINDER_ID_PREFIX
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class ReminderScheduler(
    private val logger: Logger,
) {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    actual suspend fun scheduleWeeklyReminder(
        dayOfWeek: DayOfWeek,
        secondOfDay: Int,
        identifier: String,
    ) {
        val content =
            UNMutableNotificationContent().apply {
                setTitle("Productivity Reminder")
                setBody("Time to focus on your goals!")
                setSound(UNNotificationSound.defaultSound)
            }

        // Convert secondOfDay to hour/minute
        val hour = secondOfDay / 3600
        val minute = (secondOfDay % 3600) / 60

        // Create weekly trigger
        val dateComponents =
            NSDateComponents().apply {
                setWeekday(dayOfWeek.toIOSWeekday().toLong())
                setHour(hour.toLong())
                setMinute(minute.toLong())
            }

        val trigger =
            UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents,
                repeats = true,
            )

        val request =
            UNNotificationRequest.requestWithIdentifier(
                identifier,
                content,
                trigger,
            )

        logger.d("Scheduling iOS reminder for $dayOfWeek at $hour:$minute with identifier: $identifier")

        suspendCancellableCoroutine<Unit> { cont ->
            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    logger.e("Failed to schedule reminder: ${error.localizedDescription}")
                } else {
                    logger.d("Successfully scheduled reminder: $identifier")
                }
                cont.resume(Unit)
            }
        }
    }

    actual fun cancelAllReminders() {
        logger.d("Cancelling all reminders")
        val reminderIdentifiers = DayOfWeek.entries.map { "${REMINDER_ID_PREFIX}${it.isoDayNumber}" }
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(reminderIdentifiers)
    }

    /**
     * Converts Kotlin DayOfWeek to iOS weekday format.
     * iOS uses 1=Sunday, 2=Monday, ..., 7=Saturday
     * Kotlin DayOfWeek uses 1=Monday, 2=Tuesday, ..., 7=Sunday
     */
    private fun DayOfWeek.toIOSWeekday(): Int =
        when (this.isoDayNumber) {
            7 -> 1 // Sunday
            else -> this.isoDayNumber + 1
        }
}
