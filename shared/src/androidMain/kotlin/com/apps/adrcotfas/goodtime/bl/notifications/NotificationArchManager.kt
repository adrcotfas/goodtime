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
package com.apps.adrcotfas.goodtime.bl.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.bl.DndModeController
import com.apps.adrcotfas.goodtime.bl.DomainTimerData
import com.apps.adrcotfas.goodtime.bl.TimerService
import com.apps.adrcotfas.goodtime.bl.TimerService.Companion.Action
import com.apps.adrcotfas.goodtime.bl.TimerState
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.bl.isFocus
import com.apps.adrcotfas.goodtime.common.formatMillisToTime
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.main_break_complete
import goodtime_productivity.shared.generated.resources.main_continue
import goodtime_productivity.shared.generated.resources.main_focus_complete
import goodtime_productivity.shared.generated.resources.main_notifications_channel_name
import goodtime_productivity.shared.generated.resources.main_pause
import goodtime_productivity.shared.generated.resources.main_plus_1_min
import goodtime_productivity.shared.generated.resources.main_productivity_reminder_desc
import goodtime_productivity.shared.generated.resources.main_reminder_channel_name
import goodtime_productivity.shared.generated.resources.main_resume
import goodtime_productivity.shared.generated.resources.main_start_break
import goodtime_productivity.shared.generated.resources.main_start_focus
import goodtime_productivity.shared.generated.resources.main_stop
import goodtime_productivity.shared.generated.resources.settings_productivity_reminder_title
import goodtime_productivity.shared.generated.resources.stats_break
import goodtime_productivity.shared.generated.resources.stats_focus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import java.time.Duration

class NotificationArchManager(
    private val context: Context,
    private val activityClass: Class<*>,
    coroutineScope: CoroutineScope,
) : DndModeController {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        coroutineScope.launch {
            createMainNotificationChannel()
            createReminderChannel()
        }
    }

    suspend fun buildInProgressNotification(data: DomainTimerData): Notification {
        val isFocus = data.runtime.type.isFocus
        val stateText = getString(if (isFocus) Res.string.stats_focus else Res.string.stats_break)
        return NotificationCompat
            .Builder(context, MAIN_CHANNEL_ID)
            .apply {
                setSmallIcon(if (isFocus) R.drawable.ic_status_goodtime else R.drawable.ic_break)
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setContentIntent(createOpenActivityIntent(activityClass))
                setOngoing(true)
                setRequestPromotedOngoing(true)
                setOnlyAlertOnce(true)
                setSilent(true)
                setAutoCancel(false)
                when {
                    Build.VERSION.SDK_INT >= 37 -> setMetricTimer(data, stateText)
                    Build.VERSION.SDK_INT < 36 -> setCustomViewTimer(data, stateText)
                    else -> setChronometerTimer(data, stateText)
                }
                inProgressActions(data).forEach { addAction(it) }
            }.build()
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun NotificationCompat.Builder.setMetricTimer(
        data: DomainTimerData,
        stateText: String,
    ) {
        val format = NotificationCompat.Metric.TimeDifference.FORMAT_CHRONOMETER
        val timeAtPause = Duration.ofMillis(data.runtime.timeAtPause)
        val value =
            when {
                !data.isRunning && data.isCurrentSessionCountdown() -> NotificationCompat.Metric.TimeDifference.forPausedTimer(timeAtPause, format)
                !data.isRunning -> NotificationCompat.Metric.TimeDifference.forPausedStopwatch(timeAtPause, format)
                data.isCurrentSessionCountdown() -> NotificationCompat.Metric.TimeDifference.forTimer(data.runningChronometerBase, format)
                else -> NotificationCompat.Metric.TimeDifference.forStopwatch(data.runningChronometerBase, format)
            }
        setStyle(NotificationCompat.MetricStyle().addMetric(NotificationCompat.Metric(value, stateText)).setCriticalMetric(0))
        setContentTitle(if (data.label.isDefault()) context.applicationInfo.loadLabel(context.packageManager) else data.getLabelName())
        setShowWhen(false)
        if (!data.isRunning) {
            setShortCriticalText(formatMillisToTime(data.runtime.timeAtPause))
        }
    }

    private fun NotificationCompat.Builder.setCustomViewTimer(
        data: DomainTimerData,
        stateText: String,
    ) {
        val view =
            RemoteViews(context.packageName, R.layout.notification_timer).apply {
                setTextViewText(R.id.notification_title, stateText)
                setChronometer(R.id.notification_chronometer, data.chronometerBase(), null, data.isRunning)
                setChronometerCountDown(R.id.notification_chronometer, data.isCurrentSessionCountdown())
                if (!data.isRunning) {
                    setTextViewText(R.id.notification_chronometer, formatMillisToTime(data.runtime.timeAtPause))
                }
            }
        setContentTitle(stateText)
        setSubText(data.labelSubText())
        setCustomContentView(view)
        setStyle(NotificationCompat.DecoratedCustomViewStyle())
        setShowWhen(false)
    }

    private fun NotificationCompat.Builder.setChronometerTimer(
        data: DomainTimerData,
        stateText: String,
    ) {
        setContentTitle(stateText)
        setSubText(data.labelSubText())
        if (data.isRunning) {
            setUsesChronometer(true)
            setChronometerCountDown(data.isCurrentSessionCountdown())
            setWhen(System.currentTimeMillis() + data.runningChronometerBase - SystemClock.elapsedRealtime())
        } else {
            setShortCriticalText(formatMillisToTime(data.runtime.timeAtPause))
        }
    }

    private suspend fun inProgressActions(data: DomainTimerData): List<NotificationCompat.Action> {
        val toggle = (if (data.isRunning) Res.string.main_pause else Res.string.main_resume) to Action.Toggle
        val stop = Res.string.main_stop to Action.DoReset
        val buttons =
            when {
                // count-up is always a focus session; Skip no-ops in TimerManager while the break budget is under one minute
                !data.isCurrentSessionCountdown() -> listOf(toggle, stop)

                // a break of a count-up profile is bounded by the break budget: no pause, no +1 min
                !data.getTimerProfile().isCountdown -> listOf(stop)

                data.isRunning -> listOf(toggle, Res.string.main_plus_1_min to Action.AddOneMinute)

                else -> listOf(toggle, stop)
            }
        val next = (if (data.runtime.type.isFocus) Res.string.main_start_break else Res.string.main_start_focus) to Action.Skip
        val all = if (data.label.profile.isBreakEnabled) buttons + next else buttons
        return all.map { (title, action) -> createNotificationAction(getString(title), action) }
    }

    suspend fun notifyFinished(
        finishedType: TimerType,
        data: DomainTimerData,
        withActions: Boolean,
    ) {
        val isFocus = finishedType.isFocus
        val builder =
            NotificationCompat.Builder(context, MAIN_CHANNEL_ID).apply {
                setSmallIcon(R.drawable.ic_status_goodtime)
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                if (!data.isDefaultLabel()) {
                    setSubText(data.getLabelName())
                }
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setContentIntent(createOpenActivityIntent(activityClass))
                setOngoing(false)
                setSilent(true)
                setShowWhen(false)
                setAutoCancel(true)
                setContentTitle(getString(if (isFocus) Res.string.main_focus_complete else Res.string.main_break_complete))
            }
        val extender = NotificationCompat.WearableExtender()
        if (withActions) {
            builder.setContentText(getString(Res.string.main_continue))
            val startBreak = isFocus && data.label.profile.isBreakEnabled
            val nextAction =
                createNotificationAction(
                    title = getString(if (startBreak) Res.string.main_start_break else Res.string.main_start_focus),
                    action = Action.Next,
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

    fun clearInProgressNotification() {
        notificationManager.cancel(IN_PROGRESS_NOTIFICATION_ID)
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
        title: String,
        action: Action,
    ): NotificationCompat.Action = NotificationCompat.Action
        .Builder(
            null,
            title,
            PendingIntent.getService(
                context,
                0,
                TimerService.createIntentWithAction(context, action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()

    override fun isDndModeEnabled(): Boolean = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    override fun toggleDndMode(enabled: Boolean) {
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

    private val DomainTimerData.isRunning get() = runtime.state != TimerState.PAUSED

    private val DomainTimerData.runningChronometerBase
        get() = if (isCurrentSessionCountdown()) runtime.endTime else runtime.startTime + runtime.timeSpentPaused

    private fun DomainTimerData.chronometerBase(): Long {
        if (isRunning) return runningChronometerBase
        val now = SystemClock.elapsedRealtime()
        return if (isCurrentSessionCountdown()) now + runtime.timeAtPause else now - runtime.timeAtPause
    }

    private fun DomainTimerData.labelSubText() = if (label.isDefault()) null else getLabelName()

    companion object {
        const val MAIN_CHANNEL_ID = "goodtime.notification"
        const val IN_PROGRESS_NOTIFICATION_ID = 42
        const val FINISHED_NOTIFICATION_ID = 43
        const val REMINDER_CHANNEL_ID = "goodtime_reminder_notification"
        const val REMINDER_NOTIFICATION_ID = 99
    }
}
