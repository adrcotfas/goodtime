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

import kotlinx.coroutines.flow.Flow

/**
 * Repository for the app settings.
 */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun updateReminderSettings(transform: (ProductivityReminderSettings) -> ProductivityReminderSettings)

    suspend fun updateUiSettings(transform: (UiSettings) -> UiSettings)

    suspend fun updateStatisticsSettings(transform: (StatisticsSettings) -> StatisticsSettings)

    suspend fun updateHistoryChartSettings(transform: (HistoryChartSettings) -> HistoryChartSettings)

    suspend fun updateTimerStyle(transform: (TimerStyleData) -> TimerStyleData)

    suspend fun setWorkDayStart(secondOfDay: Int)

    suspend fun setFirstDayOfWeek(dayOfWeek: Int)

    suspend fun setWorkFinishedSound(sound: String?)

    suspend fun setBreakFinishedSound(sound: String?)

    suspend fun addUserSound(sound: SoundData)

    suspend fun removeUserSound(sound: SoundData)

    suspend fun setVibrationStrength(strength: Int)

    suspend fun setEnableTorch(enabled: Boolean)

    suspend fun setEnableFlashScreen(enabled: Boolean)

    suspend fun setOverrideSoundProfile(enabled: Boolean)

    suspend fun setInsistentNotification(enabled: Boolean)

    suspend fun setAutoStartWork(enabled: Boolean)

    suspend fun setAutoStartBreak(enabled: Boolean)

    suspend fun setLongBreakData(longBreakData: LongBreakData)

    suspend fun setBreakBudgetData(breakBudgetData: BreakBudgetData)

    suspend fun setNotificationPermissionState(state: NotificationPermissionState)

    suspend fun activateLabelWithName(labelName: String)

    suspend fun activateDefaultLabel()

    suspend fun setLastInsertedSessionId(id: Long)

    suspend fun setShowOnboarding(show: Boolean)

    suspend fun setShowTutorial(show: Boolean)

    suspend fun setPro(isPro: Boolean)

    suspend fun setTimeProfilesInitialized(initialized: Boolean)

    suspend fun setShouldAskForReview(enable: Boolean)

    suspend fun setBackupSettings(backupSettings: BackupSettings)

    suspend fun setLastDismissedUpdateVersionCode(versionCode: Long)

    suspend fun setPersistedTimerState(state: PersistedTimerState?)

    suspend fun clearPersistedTimerState()
}
