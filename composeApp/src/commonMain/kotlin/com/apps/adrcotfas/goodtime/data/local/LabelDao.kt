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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(label: LocalLabel): Long

    // TODO: should use Upsert here
    @Query(
        """
        UPDATE localLabel SET
            name = :newName,
            colorIndex = :newColorIndex,
            useDefaultTimeProfile = :newUseDefaultTimeProfile,
            timerProfileName = :newTimerProfileName,
            isCountdown = :newIsCountdown,
            workDuration = :newWorkDuration,
            isBreakEnabled = :newIsBreakEnabled,
            breakDuration = :newBreakDuration,
            isLongBreakEnabled = :newIsLongBreakEnabled,
            longBreakDuration = :newLongBreakDuration,
            sessionsBeforeLongBreak = :newSessionsBeforeLongBreak,
            workBreakRatio = :newWorkBreakRatio
        WHERE name = :name
    """,
    )
    suspend fun updateLabel(
        newName: String,
        newColorIndex: Long,
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
    )

    @Query("UPDATE localLabel SET orderIndex = :newOrderIndex WHERE name = :name")
    suspend fun updateOrderIndex(
        newOrderIndex: Int,
        name: String,
    )

    @Query("UPDATE localLabel SET isArchived = :isArchived WHERE name = :name")
    suspend fun updateIsArchived(
        isArchived: Boolean,
        name: String,
    )

    @Transaction
    suspend fun insertLabelAndBulkRearrange(
        label: LocalLabel,
        labelsToUpdate: List<Pair<String, Long>>,
    ) {
        insert(label)
        labelsToUpdate.forEach { (name, newOrderIndex) ->
            updateOrderIndex(newOrderIndex.toInt(), name)
        }
    }

    @Transaction
    suspend fun bulkUpdateLabelOrderIndex(labelsToUpdate: List<Pair<String, Long>>) {
        labelsToUpdate.forEach { (name, newOrderIndex) ->
            updateOrderIndex(newOrderIndex.toInt(), name)
        }
    }

    @Query("SELECT * FROM localLabel ORDER BY orderIndex")
    fun selectAll(): Flow<List<LocalLabel>>

    @Query("SELECT * FROM localLabel WHERE isArchived = :isArchived ORDER BY orderIndex")
    fun selectByArchived(isArchived: Boolean): Flow<List<LocalLabel>>

    @Query("SELECT * FROM localLabel WHERE name = :name")
    fun selectByName(name: String): Flow<LocalLabel?>

    @Query("DELETE FROM localLabel WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM localLabel WHERE name != 'PRODUCTIVITY_DEFAULT_LABEL'")
    suspend fun deleteAll()

    @Query("UPDATE localLabel SET isArchived = 1 WHERE name != 'PRODUCTIVITY_DEFAULT_LABEL'")
    suspend fun archiveAllButDefault()
}
