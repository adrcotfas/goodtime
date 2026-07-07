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
package com.apps.adrcotfas.goodtime.settings.notifications

import android.app.Activity
import android.media.RingtoneManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SoundsViewModel(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _soundData = MutableStateFlow<Set<SoundData>>(emptySet())
    val soundData = _soundData.asStateFlow()

    private val _userSoundData = MutableStateFlow<Set<SoundData>>(emptySet())
    val userSoundData = _userSoundData.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.map { it.userSounds }.collect { sounds ->
                _userSoundData.update { sounds }
            }
        }
    }

    suspend fun fetchNotificationSounds(activity: Activity) {
        withContext(defaultDispatcher) {
            val manager = RingtoneManager(activity)
            manager.setType(RingtoneManager.TYPE_NOTIFICATION)
            val cursor = manager.cursor

            val list = mutableSetOf<SoundData>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX)
                val uri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX)
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                list.add(SoundData(name = title, uriString = "$uri/$id"))
            }
            _soundData.update { list }
        }
    }

    fun saveUserSound(userSoundData: SoundData) {
        viewModelScope.launch {
            settingsRepository.addUserSound(userSoundData)
        }
    }

    fun removeUserSound(userSoundData: SoundData) {
        viewModelScope.launch {
            settingsRepository.removeUserSound(userSoundData)
        }
    }
}
