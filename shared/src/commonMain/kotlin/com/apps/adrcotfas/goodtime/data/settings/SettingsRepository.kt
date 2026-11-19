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
package com.apps.adrcotfas.goodtime.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<AppSettings>

    /** Atomically replaces the settings with [transform] applied to the current value. */
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/**
 * Observe a projection of the settings, emitting only when the selected value changes.
 * Prefer this over hand-rolled `distinctUntilChanged` comparators: the selector is both
 * the projection and the equality check, so they cannot drift apart.
 */
fun <T> SettingsRepository.select(selector: (AppSettings) -> T): Flow<T> = settings.map(selector).distinctUntilChanged()

suspend fun SettingsRepository.updateUiSettings(transform: (UiSettings) -> UiSettings) = update { it.copy(uiSettings = transform(it.uiSettings)) }

suspend fun SettingsRepository.updateTimerStyle(transform: (TimerStyleData) -> TimerStyleData) = update { it.copy(timerStyle = transform(it.timerStyle)) }

suspend fun SettingsRepository.updateStatisticsSettings(transform: (StatisticsSettings) -> StatisticsSettings) = update { it.copy(statisticsSettings = transform(it.statisticsSettings)) }

suspend fun SettingsRepository.updateHistoryChartSettings(transform: (HistoryChartSettings) -> HistoryChartSettings) = update { it.copy(historyChartSettings = transform(it.historyChartSettings)) }

suspend fun SettingsRepository.updateReminderSettings(transform: (ProductivityReminderSettings) -> ProductivityReminderSettings) = update { it.copy(productivityReminderSettings = transform(it.productivityReminderSettings)) }

suspend fun SettingsRepository.updateBackupSettings(transform: (BackupSettings) -> BackupSettings) = update { it.copy(backupSettings = transform(it.backupSettings)) }
