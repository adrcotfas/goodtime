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
package com.apps.adrcotfas.goodtime.fakes

import com.apps.adrcotfas.goodtime.data.local.LocalTimerProfile
import com.apps.adrcotfas.goodtime.data.local.TimerProfileDao
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.model.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTimerProfileDao : TimerProfileDao {
    private val timerProfiles =
        MutableStateFlow(
            listOf(
                TimerProfile.default().copy(name = TimerProfile.DEFAULT_PROFILE_NAME).toLocal(),
            ),
        )

    override suspend fun insert(timerProfile: LocalTimerProfile) {
        val currentList = timerProfiles.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.name == timerProfile.name }
        if (existingIndex != -1) {
            currentList[existingIndex] = timerProfile
        } else {
            currentList.add(timerProfile)
        }
        timerProfiles.value = currentList
    }

    override suspend fun insertTimerProfileAndSetDefault(timerProfile: LocalTimerProfile) {
        insert(timerProfile)
    }

    override suspend fun setDefaultLabelProfileName(name: String) {
        // do nothing here
    }

    override suspend fun deleteByName(name: String) {
        val currentList = timerProfiles.value.toMutableList()
        currentList.removeAll { it.name == name }
        timerProfiles.value = currentList
    }

    override fun selectByName(name: String): Flow<LocalTimerProfile?> =
        timerProfiles.map { profiles ->
            profiles.firstOrNull { it.name == name }
        }

    override fun selectByNames(names: List<String>): Flow<List<LocalTimerProfile>> =
        timerProfiles.map { profiles ->
            profiles.filter { it.name in names }
        }

    override fun selectAll(): Flow<List<LocalTimerProfile>> = timerProfiles
}
