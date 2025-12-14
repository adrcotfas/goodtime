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

import com.apps.adrcotfas.goodtime.data.local.LabelDao
import com.apps.adrcotfas.goodtime.data.local.LocalLabel
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeLabelDao : LabelDao {
    private val labels: MutableStateFlow<List<LocalLabel>> =
        MutableStateFlow(listOf(Label.defaultLabel().toLocal()))

    override suspend fun insert(label: LocalLabel): Long {
        labels.value += label
        return labels.value.size.toLong()
    }

    override suspend fun updateLabel(
        newName: String,
        newColorIndex: Int,
        newUseDefaultTimeProfile: Boolean,
        newTimerProfileName: String?,
        newIsCountdown: Boolean,
        newWorkDuration: Int,
        newIsBreakEnabled: Boolean,
        newBreakDuration: Int,
        newIsLongBreakEnabled: Boolean,
        newLongBreakDuration: Int,
        newSessionsBeforeLongBreak: Int,
        newWorkBreakRatio: Int,
        name: String,
    ) {
        labels.value =
            labels.value.map {
                if (it.name == name) {
                    it.copy(
                        name = newName,
                        colorIndex = newColorIndex,
                        timerProfileName = newTimerProfileName,
                        useDefaultTimeProfile = newUseDefaultTimeProfile,
                        isCountdown = newIsCountdown,
                        workDuration = newWorkDuration,
                        isBreakEnabled = newIsBreakEnabled,
                        breakDuration = newBreakDuration,
                        isLongBreakEnabled = newIsLongBreakEnabled,
                        longBreakDuration = newLongBreakDuration,
                        sessionsBeforeLongBreak = newSessionsBeforeLongBreak,
                        workBreakRatio = newWorkBreakRatio,
                    )
                } else {
                    it
                }
            }
    }

    override suspend fun updateOrderIndex(
        newOrderIndex: Int,
        name: String,
    ) {
        labels.value =
            labels.value.map {
                if (it.name == name) {
                    it.copy(orderIndex = newOrderIndex.toLong())
                } else {
                    it
                }
            }
    }

    override suspend fun updateIsArchived(
        isArchived: Boolean,
        name: String,
    ) {
        labels.value =
            labels.value.map {
                if (it.name == name) {
                    it.copy(isArchived = isArchived)
                } else {
                    it
                }
            }
    }

    override fun selectAll(): Flow<List<LocalLabel>> = labels

    override fun selectByArchived(isArchived: Boolean): Flow<List<LocalLabel>> =
        labels.map { labels ->
            labels.filter {
                it.isArchived == isArchived
            }
        }

    override fun selectByName(name: String): Flow<LocalLabel?> = labels.map { labels -> labels.find { it.name == name } }

    override suspend fun deleteByName(name: String) {
        labels.value = labels.value.filter { it.name != name }
    }

    override suspend fun deleteAll() {
        labels.value = labels.value.filter { it.name == Label.DEFAULT_LABEL_NAME }
    }

    override suspend fun archiveAllButDefault() {
        labels.value =
            labels.value.map {
                if (it.name != Label.DEFAULT_LABEL_NAME) {
                    it.copy(isArchived = true)
                } else {
                    it
                }
            }
    }
}
