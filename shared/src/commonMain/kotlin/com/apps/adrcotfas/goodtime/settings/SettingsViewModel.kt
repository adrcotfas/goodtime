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
package com.apps.adrcotfas.goodtime.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.common.TimeFormatProvider
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.settings.AppSettings
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.ThemePreference
import com.apps.adrcotfas.goodtime.data.settings.TimerStyleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val lockedTimerStyle: TimerStyleData = TimerStyleData(),
    val defaultLabel: Label = Label.defaultLabel(),
    val showTimePicker: Boolean = false,
    val showWorkdayStartPicker: Boolean = false,
    val showSelectWorkSoundPicker: Boolean = false,
    val showSelectBreakSoundPicker: Boolean = false,
    val notificationSoundCandidate: String? = null,
    val is24HourFormat: Boolean = true,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val dataRepository: LocalDataRepository,
    private val timeFormatProvider: TimeFormatProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState =
        _uiState
            .onStart {
                loadData()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(
                flow = dataRepository.selectDefaultLabel().filterNotNull(),
                flow2 = settingsRepository.settings.distinctUntilChanged(),
            ) { defaultLabel, settings -> defaultLabel to settings }
                .collect { (defaultLabel, settings) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            settings = settings,
                            defaultLabel = defaultLabel,
                            lockedTimerStyle = settings.timerStyle,
                            is24HourFormat = timeFormatProvider.is24HourFormat(),
                        )
                    }
                }
        }
    }

    fun onToggleProductivityReminderDay(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            settingsRepository.updateReminderSettings {
                val days = it.days
                val alreadyEnabled = days.contains(dayOfWeek.isoDayNumber)
                it.copy(
                    days = if (alreadyEnabled) days - dayOfWeek.isoDayNumber else days + dayOfWeek.isoDayNumber,
                )
            }
        }
    }

    fun setShowTimePicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTimePicker = show)
    }

    fun setReminderTime(secondOfDay: Int) {
        viewModelScope.launch {
            settingsRepository.updateReminderSettings {
                it.copy(secondOfDay = secondOfDay)
            }
        }
    }

    fun setThemeOption(themePreference: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.updateUiSettings {
                it.copy(themePreference = themePreference)
            }
        }
    }

    fun setUseDynamicColor(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUiSettings {
                (it.copy(useDynamicColor = enable))
            }
        }
    }

    fun setFullscreenMode(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUiSettings {
                it.copy(fullscreenMode = enable)
            }
        }
    }

    fun setTrueBlackMode(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUiSettings {
                it.copy(trueBlackMode = enable)
            }
        }
    }

    fun setScreensaverMode(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUiSettings {
                it.copy(screensaverMode = enable)
            }
        }
    }

    fun setWorkDayStart(secondOfDay: Int) {
        viewModelScope.launch {
            settingsRepository.setWorkDayStart(secondOfDay)
        }
    }

    fun setFirstDayOfWeek(dayOfWeek: Int) {
        viewModelScope.launch {
            settingsRepository.setFirstDayOfWeek(dayOfWeek)
        }
    }

    fun setLauncherNameIndex(index: Int) {
        viewModelScope.launch {
            settingsRepository.updateUiSettings {
                it.copy(launcherNameIndex = index)
            }
        }
    }

    fun setShowWorkdayStartPicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showWorkdayStartPicker = show)
    }

    fun setVibrationStrength(vibrationStrength: Int) {
        viewModelScope.launch {
            settingsRepository.setVibrationStrength(vibrationStrength)
        }
    }

    fun setEnableTorch(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEnableTorch(enable)
        }
    }

    fun setEnableFlashScreen(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEnableFlashScreen(enable)
        }
    }

    fun setInsistentNotification(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setInsistentNotification(enable)
            if (enable) {
                val settings = settingsRepository.settings.first()
                if (settings.autoStartFocus) {
                    setAutoStartWork(false)
                }
                if (settings.autoStartBreak) {
                    setAutoStartBreak(false)
                }
            }
        }
    }

    fun setAutoStartWork(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoStartWork(enable)
            if (enable) {
                val settings = settingsRepository.settings.first()
                if (settings.insistentNotification) {
                    setInsistentNotification(false)
                }
            }
        }
    }

    fun setAutoStartBreak(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoStartBreak(enable)
            if (enable) {
                val settings = settingsRepository.settings.first()
                if (settings.insistentNotification) {
                    setInsistentNotification(false)
                }
            }
        }
    }

    fun setDndDuringWork(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUiSettings {
                it.copy(dndDuringWork = enable)
            }
        }
    }

    fun setShowWhenLocked(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUiSettings {
                it.copy(showWhenLocked = enable)
            }
        }
    }

    fun setWorkFinishedSound(ringtone: String) {
        viewModelScope.launch {
            settingsRepository.setWorkFinishedSound(ringtone)
        }
    }

    fun setBreakFinishedSound(ringtone: String) {
        viewModelScope.launch {
            settingsRepository.setBreakFinishedSound(ringtone)
        }
    }

    fun setOverrideSoundProfile(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOverrideSoundProfile(enabled)
        }
    }

    fun setShowSelectWorkSoundPicker(show: Boolean) {
        _uiState.value =
            _uiState.value.copy(
                showSelectWorkSoundPicker = show,
                notificationSoundCandidate = null,
            )
    }

    fun setShowSelectBreakSoundPicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSelectBreakSoundPicker = show)
    }

    fun setNotificationSoundCandidate(uri: String) {
        _uiState.value = _uiState.value.copy(notificationSoundCandidate = uri)
    }

    // Timer style settings bellow

    fun setDefaultLabelColor(colorIndex: Int) {
        viewModelScope.launch {
            if (uiState.value.settings.isPro) {
                dataRepository.updateDefaultLabel(uiState.value.defaultLabel.copy(colorIndex = colorIndex))
            } else {
                _uiState.update {
                    it.copy(
                        lockedTimerStyle = it.lockedTimerStyle.copy(colorIndex = colorIndex),
                    )
                }
            }
        }
    }

    fun setTimerWeight(weight: Int) {
        viewModelScope.launch {
            if (uiState.value.settings.isPro) {
                settingsRepository.updateTimerStyle {
                    it.copy(fontWeight = weight)
                }
            } else {
                _uiState.update {
                    it.copy(lockedTimerStyle = it.lockedTimerStyle.copy(fontWeight = weight))
                }
            }
        }
    }

    fun setTimerSize(size: Float) {
        viewModelScope.launch {
            if (uiState.value.settings.isPro) {
                settingsRepository.updateTimerStyle {
                    it.copy(
                        fontSize = size,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(lockedTimerStyle = it.lockedTimerStyle.copy(fontSize = size))
                }
            }
        }
    }

    fun setTimerMinutesOnly(enabled: Boolean) {
        viewModelScope.launch {
            if (uiState.value.settings.isPro) {
                settingsRepository.updateTimerStyle {
                    it.copy(minutesOnly = enabled)
                }
            } else {
                _uiState.update {
                    it.copy(lockedTimerStyle = it.lockedTimerStyle.copy(minutesOnly = enabled))
                }
            }
        }
    }

    fun setShowStatus(showStatus: Boolean) {
        viewModelScope.launch {
            if (uiState.value.settings.isPro) {
                settingsRepository.updateTimerStyle {
                    it.copy(showStatus = showStatus)
                }
            } else {
                _uiState.update {
                    it.copy(lockedTimerStyle = it.lockedTimerStyle.copy(showStatus = showStatus))
                }
            }
        }
    }

    fun setShowStreak(showStreak: Boolean) {
        viewModelScope.launch {
            if (uiState.value.settings.isPro) {
                settingsRepository.updateTimerStyle {
                    it.copy(showStreak = showStreak)
                }
            } else {
                _uiState.update {
                    it.copy(lockedTimerStyle = it.lockedTimerStyle.copy(showStreak = showStreak))
                }
            }
        }
    }

    companion object {
        val firstDayOfWeekOptions =
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            )
    }
}
