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
package com.apps.adrcotfas.goodtime.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.bl.TimerManager
import com.apps.adrcotfas.goodtime.bl.isActive
import com.apps.adrcotfas.goodtime.bl.isFinished
import com.apps.adrcotfas.goodtime.data.settings.NotificationPermissionState
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val loading: Boolean = true,
    val showOnboarding: Boolean = false,
    val isActive: Boolean = false,
    val isFinished: Boolean = false,
    val dndDuringWork: Boolean = false,
    val darkThemePreference: ThemePreference = ThemePreference.SYSTEM,
    val isDynamicColor: Boolean = false,
    val isMainScreen: Boolean = true,
    val fullscreenMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val showWhenLocked: Boolean = false,
    val shouldAskForReview: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val wasNotificationPermissionDenied: Boolean = false,
    val lastDismissedUpdateVersionCode: Long = 0,
)

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val timerManager: TimerManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .distinctUntilChanged { old, new ->
                    old.showOnboarding == new.showOnboarding &&
                        old.uiSettings.dndDuringWork == new.uiSettings.dndDuringWork &&
                        old.uiSettings.themePreference == new.uiSettings.themePreference &&
                        old.uiSettings.useDynamicColor == new.uiSettings.useDynamicColor &&
                        old.uiSettings.fullscreenMode == new.uiSettings.fullscreenMode &&
                        old.uiSettings.keepScreenOn == new.uiSettings.keepScreenOn &&
                        old.uiSettings.showWhenLocked == new.uiSettings.showWhenLocked &&
                        old.shouldAskForReview == new.shouldAskForReview &&
                        old.notificationPermissionState == new.notificationPermissionState &&
                        old.lastDismissedUpdateVersionCode == new.lastDismissedUpdateVersionCode
                }.collect { settings ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            showOnboarding = settings.showOnboarding,
                            dndDuringWork = settings.uiSettings.dndDuringWork,
                            darkThemePreference = settings.uiSettings.themePreference,
                            isDynamicColor = settings.uiSettings.useDynamicColor,
                            fullscreenMode = settings.uiSettings.fullscreenMode,
                            keepScreenOn = settings.uiSettings.keepScreenOn,
                            showWhenLocked = settings.uiSettings.showWhenLocked,
                            shouldAskForReview = settings.shouldAskForReview,
                            wasNotificationPermissionDenied =
                                settings.notificationPermissionState ==
                                    NotificationPermissionState.DENIED,
                            lastDismissedUpdateVersionCode = settings.lastDismissedUpdateVersionCode,
                        )
                    }
                }
        }
        viewModelScope.launch {
            timerManager.timerData
                .distinctUntilChanged { old, new ->
                    old.state == new.state &&
                        old.type == new.type
                }.collect { timerData ->
                    _uiState.update {
                        it.copy(
                            isActive = timerData.state.isActive,
                            isFinished = timerData.state.isFinished,
                        )
                    }
                }
        }
    }

    fun setShowOnboarding(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowOnboarding(show)
        }
    }

    fun setShowTutorial(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowTutorial(show)
        }
    }

    fun resetShouldAskForReview() = viewModelScope.launch { settingsRepository.setShouldAskForReview(false) }

    fun setUpdateAvailable(available: Boolean) {
        _uiState.update {
            it.copy(isUpdateAvailable = available)
        }
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        viewModelScope.launch {
            val state =
                if (granted) NotificationPermissionState.GRANTED else NotificationPermissionState.DENIED
            settingsRepository.setNotificationPermissionState(state)
        }
    }

    fun setLastDismissedUpdateVersionCode(versionCode: Long) {
        viewModelScope.launch {
            settingsRepository.setLastDismissedUpdateVersionCode(versionCode)
        }
    }
}
