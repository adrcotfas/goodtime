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

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val log: Logger,
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val isProKey = booleanPreferencesKey("isProKey")
        val timerProfilesInitializedKey = booleanPreferencesKey("timerProfilesInitializedKey")
        val shouldAskForReviewKey = booleanPreferencesKey("shouldAskForReview")
        val lastAskedForReviewTimeKey = longPreferencesKey("lastAskedForReviewTime")
        val productivityReminderSettingsKey =
            stringPreferencesKey("productivityReminderSettingsKey")
        val uiSettingsKey = stringPreferencesKey("uiSettingsKey")
        val statisticsSettingsKey = stringPreferencesKey("statisticsSettingsKey")
        val historyChartSettingsKey = stringPreferencesKey("historyChartSettingsKey")
        val timerStyleKey = stringPreferencesKey("timerStyleKey")
        val workdayStartKey = intPreferencesKey("workdayStartKey")
        val firstDayOfWeekKey = intPreferencesKey("firstDayOfWeekKey")
        val workFinishedSoundKey = stringPreferencesKey("workFinishedSoundKey")
        val breakFinishedSoundKey = stringPreferencesKey("breakFinishedSoundKey")
        val userSoundsKey = stringPreferencesKey("userSoundsKey")
        val notificationSoundVolumeKey = intPreferencesKey("notificationSoundVolumeKey")
        val vibrationStrengthKey = intPreferencesKey("vibrationStrengthKey")
        val enableTorchKey = booleanPreferencesKey("enableTorchKey")
        val enableFlashScreenKey = booleanPreferencesKey("enableFlashScreenKey")
        val insistentNotificationKey = booleanPreferencesKey("insistentNotificationKey")
        val autoStartWorkKey = booleanPreferencesKey("autoStartWorkKey")
        val autoStartBreakKey = booleanPreferencesKey("autoStartBreakKey")
        val labelNameKey = stringPreferencesKey("labelNameKey")
        val longBreakDataKey = stringPreferencesKey("longBreakDataKey")
        val breakBudgetDataKey = stringPreferencesKey("breakBudgetDataKey")
        val notificationPermissionStateKey = intPreferencesKey("notificationPermissionStateKey")
        val lastInsertedSessionIdKey = longPreferencesKey("lastInsertedSessionIdKey")
        val showOnboardingKey = booleanPreferencesKey("showOnboardingKey")
        val showTutorialKey = booleanPreferencesKey("showTutorialKey")
        val dndOwnedByAppKey = booleanPreferencesKey("dndOwnedByAppKey")
        val backupSettingsKey = stringPreferencesKey("backupSettingsKey")
        val lastDismissedUpdateVersionCodeKey = longPreferencesKey("lastDismissedUpdateVersionCodeKey")
        val persistedTimerStateKey = stringPreferencesKey("persistedTimerStateKey")
    }

    override val settings: Flow<AppSettings> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    log.e("Error reading settings", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { it.toAppSettings() }
            .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit {
            val old = it.toAppSettings()
            it.write(old, transform(old))
        }
    }

    private inline fun <reified T> Preferences.decode(key: Preferences.Key<String>): T? = this[key]?.let { value ->
        runCatching { json.decodeFromString<T>(value) }
            .onFailure { e -> log.e("Error parsing ${key.name}", e) }
            .getOrNull()
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val default = AppSettings()
        return AppSettings(
            isPro = this[Keys.isProKey] ?: default.isPro,
            timeProfilesInitialized = this[Keys.timerProfilesInitializedKey] ?: default.timeProfilesInitialized,
            shouldAskForReview = this[Keys.shouldAskForReviewKey] ?: default.shouldAskForReview,
            lastAskedForReviewTime = this[Keys.lastAskedForReviewTimeKey] ?: default.lastAskedForReviewTime,
            productivityReminderSettings = decode(Keys.productivityReminderSettingsKey) ?: default.productivityReminderSettings,
            uiSettings = decode(Keys.uiSettingsKey) ?: default.uiSettings,
            statisticsSettings = decode(Keys.statisticsSettingsKey) ?: default.statisticsSettings,
            historyChartSettings = decode(Keys.historyChartSettingsKey) ?: default.historyChartSettings,
            timerStyle = decode(Keys.timerStyleKey) ?: default.timerStyle,
            workdayStart = this[Keys.workdayStartKey] ?: default.workdayStart,
            firstDayOfWeek = this[Keys.firstDayOfWeekKey] ?: default.firstDayOfWeek,
            workFinishedSound = this[Keys.workFinishedSoundKey] ?: default.workFinishedSound,
            breakFinishedSound = this[Keys.breakFinishedSoundKey] ?: default.breakFinishedSound,
            userSounds = decode(Keys.userSoundsKey) ?: default.userSounds,
            notificationSoundVolume = this[Keys.notificationSoundVolumeKey] ?: default.notificationSoundVolume,
            vibrationStrength = this[Keys.vibrationStrengthKey] ?: default.vibrationStrength,
            enableTorch = this[Keys.enableTorchKey] ?: default.enableTorch,
            flashScreen = this[Keys.enableFlashScreenKey] ?: default.flashScreen,
            insistentNotification = this[Keys.insistentNotificationKey] ?: default.insistentNotification,
            autoStartFocus = this[Keys.autoStartWorkKey] ?: default.autoStartFocus,
            autoStartBreak = this[Keys.autoStartBreakKey] ?: default.autoStartBreak,
            labelName = this[Keys.labelNameKey] ?: default.labelName,
            longBreakData = decode(Keys.longBreakDataKey) ?: default.longBreakData,
            breakBudgetData = decode(Keys.breakBudgetDataKey) ?: default.breakBudgetData,
            notificationPermissionState =
            this[Keys.notificationPermissionStateKey]?.let { NotificationPermissionState.entries.getOrNull(it) }
                ?: default.notificationPermissionState,
            lastInsertedSessionId = this[Keys.lastInsertedSessionIdKey] ?: default.lastInsertedSessionId,
            showOnboarding = this[Keys.showOnboardingKey] ?: default.showOnboarding,
            showTutorial = this[Keys.showTutorialKey] ?: default.showTutorial,
            dndOwnedByApp = this[Keys.dndOwnedByAppKey] ?: default.dndOwnedByApp,
            backupSettings = decode(Keys.backupSettingsKey) ?: default.backupSettings,
            lastDismissedUpdateVersionCode =
            this[Keys.lastDismissedUpdateVersionCodeKey] ?: default.lastDismissedUpdateVersionCode,
            persistedTimerState = decode(Keys.persistedTimerStateKey),
        )
    }

    /** Writes only the fields that changed, so untouched settings keep following their defaults. */
    private class Writer(
        private val json: Json,
        private val prefs: MutablePreferences,
        private val old: AppSettings,
        private val new: AppSettings,
    ) {
        fun <T> put(key: Preferences.Key<T>, get: AppSettings.() -> T) {
            val value = new.get()
            if (old.get() != value) prefs[key] = value
        }

        inline fun <reified T> putJson(key: Preferences.Key<String>, get: AppSettings.() -> T?) {
            val value = new.get()
            if (old.get() == value) return
            if (value == null) prefs.remove(key) else prefs[key] = json.encodeToString(value)
        }
    }

    private fun MutablePreferences.write(old: AppSettings, new: AppSettings) = with(Writer(json, this, old, new)) {
        put(Keys.isProKey) { isPro }
        put(Keys.timerProfilesInitializedKey) { timeProfilesInitialized }
        put(Keys.shouldAskForReviewKey) { shouldAskForReview }
        put(Keys.lastAskedForReviewTimeKey) { lastAskedForReviewTime }
        putJson(Keys.productivityReminderSettingsKey) { productivityReminderSettings }
        putJson(Keys.uiSettingsKey) { uiSettings }
        putJson(Keys.statisticsSettingsKey) { statisticsSettings }
        putJson(Keys.historyChartSettingsKey) { historyChartSettings }
        putJson(Keys.timerStyleKey) { timerStyle }
        put(Keys.workdayStartKey) { workdayStart }
        put(Keys.firstDayOfWeekKey) { firstDayOfWeek }
        put(Keys.workFinishedSoundKey) { workFinishedSound }
        put(Keys.breakFinishedSoundKey) { breakFinishedSound }
        putJson(Keys.userSoundsKey) { userSounds }
        put(Keys.notificationSoundVolumeKey) { notificationSoundVolume }
        put(Keys.vibrationStrengthKey) { vibrationStrength }
        put(Keys.enableTorchKey) { enableTorch }
        put(Keys.enableFlashScreenKey) { flashScreen }
        put(Keys.insistentNotificationKey) { insistentNotification }
        put(Keys.autoStartWorkKey) { autoStartFocus }
        put(Keys.autoStartBreakKey) { autoStartBreak }
        put(Keys.labelNameKey) { labelName }
        putJson(Keys.longBreakDataKey) { longBreakData }
        putJson(Keys.breakBudgetDataKey) { breakBudgetData }
        put(Keys.notificationPermissionStateKey) { notificationPermissionState.ordinal }
        put(Keys.lastInsertedSessionIdKey) { lastInsertedSessionId }
        put(Keys.showOnboardingKey) { showOnboarding }
        put(Keys.showTutorialKey) { showTutorial }
        put(Keys.dndOwnedByAppKey) { dndOwnedByApp }
        putJson(Keys.backupSettingsKey) { backupSettings }
        put(Keys.lastDismissedUpdateVersionCodeKey) { lastDismissedUpdateVersionCode }
        putJson(Keys.persistedTimerStateKey) { persistedTimerState }
    }
}
