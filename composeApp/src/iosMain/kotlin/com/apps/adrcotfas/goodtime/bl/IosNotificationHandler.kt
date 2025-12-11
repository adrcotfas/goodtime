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

import co.touchlab.kermit.Logger
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.main_break_finished
import goodtime_productivity.composeapp.generated.resources.main_continue
import goodtime_productivity.composeapp.generated.resources.main_focus_session_finished
import goodtime_productivity.composeapp.generated.resources.main_start_break
import goodtime_productivity.composeapp.generated.resources.main_start_focus
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

private const val NOTIFICATION_ID = "goodtime_timer_finished"
private const val CATEGORY_TIMER_FINISHED = "TIMER_FINISHED"
private const val ACTION_START_NEXT = "START_NEXT"

class IosNotificationHandler(
    private val timeProvider: TimeProvider,
    private val coroutineScope: CoroutineScope,
    private val log: Logger,
) : EventListener {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    private val delegate = IosNotificationDelegate(log, ::handleNotificationAction)

    // Store current session info to use when rescheduling notifications
    private var currentStartEvent: Event.Start? = null

    // Callback for handling "next" action from notification
    private var onStartNextSession: (() -> Unit)? = null

    init {
        // Set delegate to enable foreground notifications
        notificationCenter.delegate = delegate

        // Register notification categories with actions
        registerNotificationCategories()
    }

    fun init(onStartNextSession: () -> Unit) {
        this.onStartNextSession = onStartNextSession
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun registerNotificationCategories() {
        val startNextAction =
            UNNotificationAction.actionWithIdentifier(
                identifier = ACTION_START_NEXT,
                title = "Start Next", // Will be dynamically set based on session type
                options = 0u, // Foreground action - opens the app
            )

        // Create category with action
        val category =
            UNNotificationCategory.categoryWithIdentifier(
                identifier = CATEGORY_TIMER_FINISHED,
                actions = listOf<Any?>(startNextAction),
                intentIdentifiers = listOf<Any?>(),
                options = 0u,
            )

        // Register categories
        notificationCenter.setNotificationCategories(setOf<Any?>(category))
        log.v { "Registered notification categories with action" }
    }

    private fun handleNotificationAction(actionId: String) {
        log.v { "Handling notification action: $actionId" }

        when (actionId) {
            ACTION_START_NEXT -> {
                log.v { "User tapped 'Start Next' action - starting next session" }
                // Equivalent to Android's TimerService.Action.Next
                coroutineScope.launch {
                    onStartNextSession?.invoke()
                }
            }
            "com.apple.UNNotificationDefaultActionIdentifier" -> {
                log.v { "User tapped notification body (default action) - opens app" }
                // User tapped the notification itself, not a button
                // iOS automatically brings the app to foreground
            }
            "com.apple.UNNotificationDismissActionIdentifier" -> {
                log.v { "User dismissed notification" }
            }
            else -> {
                log.w { "Unknown action identifier: $actionId" }
            }
        }
    }

    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start -> {
                // Store current session info for later use
                currentStartEvent = event

                if (event.endTime != 0L) {
                    coroutineScope.launch {
                        scheduleNotification(event.endTime, event)
                    }
                }
            }

            is Event.AddOneMinute -> {
                if (event.endTime != 0L) {
                    cancelNotification()
                    currentStartEvent?.let { startEvent ->
                        coroutineScope.launch {
                            scheduleNotification(event.endTime, startEvent)
                        }
                    }
                }
            }

            is Event.Pause, is Event.Reset -> {
                cancelNotification()
            }

            else -> {
                // Other events don't need notification handling
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun scheduleNotification(
        endTime: Long,
        startEvent: Event.Start,
    ) {
        val currentTime = timeProvider.elapsedRealtime()
        val timeUntilFinish = (endTime - currentTime) / 1000.0

        if (timeUntilFinish <= 0) {
            log.w { "Attempted to schedule notification with non-positive time: $timeUntilFinish seconds" }
            return
        }

        log.v { "Scheduling notification in $timeUntilFinish seconds (isFocus: ${startEvent.isFocus})" }

        val mainStateText =
            if (startEvent.isFocus) {
                getString(Res.string.main_focus_session_finished)
            } else {
                getString(Res.string.main_break_finished)
            }

        val labelText = if (startEvent.isDefaultLabel) "" else "${startEvent.labelName} - "
        val titleText = "$labelText$mainStateText"

        // Determine action title based on next session type
        val actionTitle =
            if (startEvent.isFocus && startEvent.isBreakEnabled) {
                getString(Res.string.main_start_break)
            } else {
                getString(Res.string.main_start_focus)
            }

        // Update action with dynamic title
        updateNotificationAction(actionTitle)

        val content =
            UNMutableNotificationContent().apply {
                setTitle(titleText)
                setBody(getString(Res.string.main_continue))
                // TODO: Replace with actual sound from settings when sound resources are added
                // For now, use default system sound
                setSound(UNNotificationSound.defaultSound())
                // Attach category to enable action button
                setCategoryIdentifier(CATEGORY_TIMER_FINISHED)
            }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(timeUntilFinish, false)
        val request = UNNotificationRequest.requestWithIdentifier(NOTIFICATION_ID, content, trigger)

        notificationCenter.addNotificationRequest(request) { error ->
            if (error != null) {
                log.e { "Failed to schedule notification: ${error.localizedDescription}" }
            } else {
                log.v { "Successfully scheduled notification with action: $actionTitle" }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun updateNotificationAction(actionTitle: String) {
        // Re-register category with updated action title
        val startNextAction =
            UNNotificationAction.actionWithIdentifier(
                identifier = ACTION_START_NEXT,
                title = actionTitle,
                options = 0u,
            )

        val category =
            UNNotificationCategory.categoryWithIdentifier(
                identifier = CATEGORY_TIMER_FINISHED,
                actions = listOf<Any?>(startNextAction),
                intentIdentifiers = listOf<Any?>(),
                options = 0u,
            )

        notificationCenter.setNotificationCategories(setOf<Any?>(category))
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun cancelNotification() {
        log.v { "Cancelling notification, if any" }
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(NOTIFICATION_ID))
    }
}
