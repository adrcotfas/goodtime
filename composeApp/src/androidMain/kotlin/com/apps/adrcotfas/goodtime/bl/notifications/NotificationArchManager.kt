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
package com.apps.adrcotfas.goodtime.bl.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.bl.DomainTimerData
import com.apps.adrcotfas.goodtime.bl.TimerService
import com.apps.adrcotfas.goodtime.bl.TimerState
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.bl.isFocus
import com.apps.adrcotfas.goodtime.common.formatMillisToTime
import com.apps.adrcotfas.goodtime.di.injectLogger
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.main_break_complete
import goodtime_productivity.composeapp.generated.resources.main_break_in_progress
import goodtime_productivity.composeapp.generated.resources.main_continue
import goodtime_productivity.composeapp.generated.resources.main_focus_complete
import goodtime_productivity.composeapp.generated.resources.main_focus_session_in_progress
import goodtime_productivity.composeapp.generated.resources.main_focus_session_paused
import goodtime_productivity.composeapp.generated.resources.main_notifications_channel_name
import goodtime_productivity.composeapp.generated.resources.main_pause
import goodtime_productivity.composeapp.generated.resources.main_plus_1_min
import goodtime_productivity.composeapp.generated.resources.main_productivity_reminder_desc
import goodtime_productivity.composeapp.generated.resources.main_reminder_channel_name
import goodtime_productivity.composeapp.generated.resources.main_resume
import goodtime_productivity.composeapp.generated.resources.main_start_break
import goodtime_productivity.composeapp.generated.resources.main_start_focus
import goodtime_productivity.composeapp.generated.resources.main_stop
import goodtime_productivity.composeapp.generated.resources.settings_productivity_reminder_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class NotificationArchManager(
    private val context: Context,
    private val activityClass: Class<*>,
    coroutineScope: CoroutineScope,
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        coroutineScope.launch {
            createMainNotificationChannel()
            createReminderChannel()
        }
    }

    suspend fun buildInProgressNotification(data: DomainTimerData): Notification {
        val isCountDown = data.isCurrentSessionCountdown()
        val elapsedRealTime = SystemClock.elapsedRealtime()
        val baseTime =
            if (isCountDown) {
                data.endTime - elapsedRealTime
            } else {
                elapsedRealTime - (data.startTime + data.timeSpentPaused)
            }
        val running = data.state != TimerState.PAUSED
        val timerType = data.type
        val labelName = data.getLabelName()
        val isDefaultLabel = data.label.isDefault()
        val stateText =
            if (timerType.isFocus) {
                if (running) {
                    getString(Res.string.main_focus_session_in_progress)
                } else {
                    getString(Res.string.main_focus_session_paused)
                }
            } else {
                getString(Res.string.main_break_in_progress)
            }

        val icon = if (timerType.isFocus) R.drawable.ic_status_goodtime else R.drawable.ic_break
        val builder =
            NotificationCompat.Builder(context, MAIN_CHANNEL_ID).apply {
                setSmallIcon(icon)
                setContentTitle(stateText)
                if (!isDefaultLabel) {
                    setSubText(labelName)
                }
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setContentIntent(createOpenActivityIntent(activityClass))
                setOngoing(true)
                setRequestPromotedOngoing(true)
                setOnlyAlertOnce(true)
                setSilent(true)
                setAutoCancel(false)

                // When paused, show the current time as text instead of chronometer
                if (!running) {
                    setShortCriticalText(formatMillisToTime(data.timeAtPause))
                } else {
                    setUsesChronometer(true)
                    setChronometerCountDown(isCountDown)
                    if (isCountDown) {
                        setWhen(System.currentTimeMillis() + baseTime)
                    } else {
                        setWhen(System.currentTimeMillis() - baseTime)
                    }
                    setShowWhen(false)
                }
            }
        if (isCountDown) {
            if (timerType == TimerType.FOCUS) {
                if (running) {
                    val pauseAction =
                        createNotificationAction(
                            title = getString(Res.string.main_pause),
                            action = TimerService.Companion.Action.Toggle,
                        )
                    builder.addAction(pauseAction)
                    val addOneMinuteAction =
                        createNotificationAction(
                            title = getString(Res.string.main_plus_1_min),
                            action = TimerService.Companion.Action.AddOneMinute,
                        )
                    builder.addAction(addOneMinuteAction)
                } else {
                    val resumeAction =
                        createNotificationAction(
                            title = getString(Res.string.main_resume),
                            action = TimerService.Companion.Action.Toggle,
                        )
                    builder.addAction(resumeAction)
                    val stopAction =
                        createNotificationAction(
                            title = getString(Res.string.main_stop),
                            action = TimerService.Companion.Action.DoReset,
                        )
                    builder.addAction(stopAction)
                }
            } else {
                val stopAction =
                    createNotificationAction(
                        title = getString(Res.string.main_stop),
                        action = TimerService.Companion.Action.DoReset,
                    )
                builder.addAction(stopAction)
                val addOneMinuteAction =
                    createNotificationAction(
                        title = getString(Res.string.main_plus_1_min),
                        action = TimerService.Companion.Action.AddOneMinute,
                    )
                builder.addAction(addOneMinuteAction)
            }
            val nextActionTitle =
                if (timerType == TimerType.FOCUS) {
                    getString(Res.string.main_start_break)
                } else {
                    getString(Res.string.main_start_focus)
                }
            val nextAction =
                createNotificationAction(
                    title = nextActionTitle,
                    action = TimerService.Companion.Action.Skip,
                )
            if (data.label.profile.isBreakEnabled) {
                builder.addAction(nextAction)
            }
        } else {
            val stopAction =
                createNotificationAction(
                    title = getString(Res.string.main_stop),
                    action = TimerService.Companion.Action.DoReset,
                )
            builder.addAction(stopAction)
        }
        return builder.build()
    }

    suspend fun notifyFinished(
        finishedType: TimerType,
        data: DomainTimerData,
        withActions: Boolean,
    ) {
        val labelName = data.getLabelName()

        val stateText =
            if (finishedType == TimerType.FOCUS) {
                getString(Res.string.main_focus_complete)
            } else {
                getString(Res.string.main_break_complete)
            }

        val builder =
            NotificationCompat.Builder(context, MAIN_CHANNEL_ID).apply {
                setSmallIcon(R.drawable.ic_status_goodtime)
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                if (!data.isDefaultLabel()) {
                    setSubText(labelName)
                }
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setContentIntent(createOpenActivityIntent(activityClass))
                setOngoing(false)
                setSilent(true)
                setShowWhen(false)
                setAutoCancel(true)
                setContentTitle(stateText)
            }
        val extender = NotificationCompat.WearableExtender()
        if (withActions) {
            builder.setContentText(getString(Res.string.main_continue))
            val nextActionTitle =
                if (finishedType == TimerType.FOCUS && data.label.profile.isBreakEnabled) {
                    getString(Res.string.main_start_break)
                } else {
                    getString(Res.string.main_start_focus)
                }
            val nextAction =
                createNotificationAction(
                    title = nextActionTitle,
                    action = TimerService.Companion.Action.Next,
                )
            extender.addAction(nextAction)
            builder.addAction(nextAction)
        }
        builder.extend(extender)
        notificationManager.notify(FINISHED_NOTIFICATION_ID, builder.build())
    }

    fun clearFinishedNotification() {
        notificationManager.cancel(FINISHED_NOTIFICATION_ID)
    }

    suspend fun notifyReminder() {
        val pendingIntent = createOpenActivityIntent(activityClass)
        val builder =
            NotificationCompat
                .Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_status_goodtime)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setShowWhen(false)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(getString(Res.string.settings_productivity_reminder_title))
                .setContentText(getString(Res.string.main_productivity_reminder_desc))
        notificationManager.notify(REMINDER_NOTIFICATION_ID, builder.build())
    }

    private suspend fun createMainNotificationChannel() {
        val name = getString(Res.string.main_notifications_channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel =
            NotificationChannel(MAIN_CHANNEL_ID, name, importance).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
                setShowBadge(true)
            }
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun createReminderChannel() {
        val name = getString(Res.string.main_reminder_channel_name)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel =
            NotificationChannel(REMINDER_CHANNEL_ID, name, importance).apply {
                setShowBadge(true)
            }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createOpenActivityIntent(activityClass: Class<*>): PendingIntent {
        val intent = Intent(context, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationAction(
        icon: IconCompat? = null,
        title: String,
        action: TimerService.Companion.Action,
    ): NotificationCompat.Action =
        NotificationCompat.Action
            .Builder(
                icon,
                title,
                PendingIntent.getService(
                    context,
                    0,
                    TimerService.createIntentWithAction(context, action),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).build()

    fun isDndModeEnabled(): Boolean = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    fun toggleDndMode(enabled: Boolean) {
        if (!isNotificationPolicyAccessGranted()) {
            return
        }
        if (enabled) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } else {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    fun isNotificationPolicyAccessGranted(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    companion object {
        const val MAIN_CHANNEL_ID = "goodtime.notification"
        const val IN_PROGRESS_NOTIFICATION_ID = 42
        const val FINISHED_NOTIFICATION_ID = 43
        const val REMINDER_CHANNEL_ID = "goodtime_reminder_notification"
        const val REMINDER_NOTIFICATION_ID = 99
    }
}
