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
package com.apps.adrcotfas.goodtime.data.local

import androidx.paging.PagingSource
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for finished sessions and labels.
 */
interface LocalDataRepository {
    fun reinitDatabase(database: ProductivityDatabase)

    suspend fun insertSession(session: Session): Long

    suspend fun updateSession(
        id: Long,
        newSession: Session,
    )

    suspend fun updateSessionsLabelByIds(
        newLabel: String,
        ids: List<Long>,
    )

    suspend fun updateSessionsLabelByIdsExcept(
        newLabel: String,
        unselectedIds: List<Long>,
        selectedLabels: List<String>,
        considerBreaks: Boolean = true,
    )

    fun selectAllSessions(): Flow<List<Session>>

    fun selectSessionsAfter(timestamp: Long): Flow<List<Session>>

    fun selectSessionById(id: Long): Flow<Session>

    fun selectSessionsByIsArchived(isArchived: Boolean): Flow<List<Session>>

    fun selectSessionsByLabel(label: String): Flow<List<Session>>

    fun selectSessionsByLabels(labels: List<String>): Flow<List<Session>>

    fun selectSessionsByLabels(
        labels: List<String>,
        after: Long,
    ): Flow<List<Session>>

    fun selectSessionsForTimelinePaged(
        labels: List<String>,
        showBreaks: Boolean,
    ): PagingSource<Int, LocalSession>

    fun selectNumberOfSessionsAfter(timestamp: Long): Flow<Int>

    suspend fun deleteSessions(ids: List<Long>)

    suspend fun deleteSessionsExcept(
        unselectedIds: List<Long>,
        selectedLabels: List<String>,
        considerBreaks: Boolean = true,
    )

    suspend fun deleteAllSessions()

    suspend fun insertLabel(label: Label): Long

    suspend fun insertLabelAndBulkRearrange(
        label: Label,
        labelsToUpdate: List<Pair<String, Long>>,
    )

    suspend fun updateLabelOrderIndex(
        name: String,
        newOrderIndex: Long,
    )

    suspend fun bulkUpdateLabelOrderIndex(labelsToUpdate: List<Pair<String, Long>>)

    suspend fun updateLabelIsArchived(
        name: String,
        newIsArchived: Boolean,
    )

    suspend fun updateLabel(
        name: String,
        newLabel: Label,
    )

    suspend fun updateDefaultLabel(newDefaultLabel: Label)

    fun selectDefaultLabel(): Flow<Label?>

    fun selectLabelByName(name: String): Flow<Label?>

    fun selectAllLabels(): Flow<List<Label>>

    fun selectLabelsByArchived(isArchived: Boolean): Flow<List<Label>>

    suspend fun deleteLabel(name: String)

    suspend fun deleteAllLabels()

    suspend fun archiveAllButDefault()

    suspend fun insertTimerProfile(profile: TimerProfile)

    suspend fun insertTimerProfileAndSetDefault(profile: TimerProfile)

    suspend fun deleteTimerProfile(name: String)

    suspend fun selectTimerProfile(name: String): Flow<TimerProfile?>

    suspend fun selectAllTimerProfiles(): Flow<List<TimerProfile>>
}
