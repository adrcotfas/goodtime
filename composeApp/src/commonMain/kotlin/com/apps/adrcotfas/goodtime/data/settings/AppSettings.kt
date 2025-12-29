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
package com.apps.adrcotfas.goodtime.data.settings

import com.apps.adrcotfas.goodtime.data.model.Label
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.Serializable

data class AppSettings(
    private val version: Int = 1,
    val isPro: Boolean = false,
    val timeProfilesInitialized: Boolean = false,
    val shouldAskForReview: Boolean = false,
    val productivityReminderSettings: ProductivityReminderSettings = ProductivityReminderSettings(),
    val uiSettings: UiSettings = UiSettings(),
    val timerStyle: TimerStyleData = TimerStyleData(),
    val workdayStart: Int = LocalTime(5, 0).toSecondOfDay(),
    val firstDayOfWeek: Int = DayOfWeek.MONDAY.isoDayNumber,
    /** The name/URI of the sound file or empty for default*/
    val workFinishedSound: String = "",
    /** The name/URI of the sound file or empty for default*/
    val breakFinishedSound: String = "",
    val overrideSoundProfile: Boolean = false,
    val userSounds: Set<SoundData> = emptySet(),
    val vibrationStrength: Int = 3,
    val enableTorch: Boolean = false,
    val flashScreen: Boolean = false,
    val insistentNotification: Boolean = false,
    /** only valid with insistentNotification off **/
    val autoStartFocus: Boolean = false,
    /** only valid with insistentNotification off and for countdown timers **/
    val autoStartBreak: Boolean = false,
    val labelName: String = Label.DEFAULT_LABEL_NAME,
    val longBreakData: LongBreakData = LongBreakData(),
    val breakBudgetData: BreakBudgetData = BreakBudgetData(),
    val notificationPermissionState: NotificationPermissionState = NotificationPermissionState.NOT_ASKED,
    val lastInsertedSessionId: Long = Long.MAX_VALUE,
    val statisticsSettings: StatisticsSettings = StatisticsSettings(),
    val historyChartSettings: HistoryChartSettings = HistoryChartSettings(),
    val showOnboarding: Boolean = true,
    val showTutorial: Boolean = true,
    val backupSettings: BackupSettings = BackupSettings(),
    /** The version code of the last dismissed update, or 0 if no update has been dismissed */
    val lastDismissedUpdateVersionCode: Long = 0,
)

enum class NotificationPermissionState {
    NOT_ASKED,
    GRANTED,
    DENIED,
}

@Serializable
data class ProductivityReminderSettings(
    /** A list representing the days when the reminder was enabled, Monday is 1 and Sunday is 7. */
    val days: List<Int> = emptyList(),
    /** The time of the reminder represented in seconds of the day */
    val secondOfDay: Int = LocalTime(9, 0).toSecondOfDay(),
)

@Serializable
data class UiSettings(
    val themePreference: ThemePreference = ThemePreference.DARK,
    val useDynamicColor: Boolean = false,
    val fullscreenMode: Boolean = false,
    val trueBlackMode: Boolean = false,
    val screensaverMode: Boolean = false,
    val dndDuringWork: Boolean = false,
    val showWhenLocked: Boolean = false,
    val launcherNameIndex: Int = 0,
)

@Serializable
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

fun ThemePreference.isDarkTheme(isSystemInDarkTheme: Boolean): Boolean =
    this == ThemePreference.DARK ||
        (this == ThemePreference.SYSTEM && isSystemInDarkTheme)

@Serializable
data class TimerStyleData(
    val colorIndex: Int = Label.DEFAULT_LABEL_COLOR_INDEX,
    val minSize: Float = 0f, // in em
    val maxSize: Float = 0f, // in em
    val fontSize: Float = 0f, // in em
    val fontWeight: Int = 200,
    val currentScreenWidth: Float = 0f, // in dp
    val minutesOnly: Boolean = false,
    val showStatus: Boolean = true,
    val showStreak: Boolean = true,
    val showBreakBudget: Boolean = true,
) {
    fun inUseFontSize() = fontSize
}

@Serializable
data class SoundData(
    val name: String = "",
    val uriString: String = "",
    val isSilent: Boolean = false,
)

@Serializable
data class BackupSettings(
    val cloudAutoBackupEnabled: Boolean = false,
    /**
     * Android-only: local auto-backups to a user-selected folder (tree URI) using WorkManager.
     */
    val autoBackupEnabled: Boolean = false,
    /**
     * Android-only: persisted folder URI (tree URI) used for the *local* auto-backup worker.
     */
    val path: String = "",
    /** Timestamp in milliseconds */
    val lastBackupTimestamp: Long = 0L,
)
