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
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: LocalSession): Long

    // TODO: should use Upsert here
    @Query(
        """
        UPDATE localSession
        SET timestamp = :newTimestamp, duration = :newDuration, interruptions = :newInterruptions, labelName = :newLabel, notes = :newNotes, isWork = :newIsWork
        WHERE id = :id
    """,
    )
    suspend fun update(
        newTimestamp: Long,
        newDuration: Long,
        newInterruptions: Long,
        newLabel: String,
        newNotes: String,
        newIsWork: Boolean,
        id: Long,
    )

    @Query("UPDATE localSession SET labelName = :newLabel WHERE id IN (:ids)")
    suspend fun updateLabelByIds(
        newLabel: String,
        ids: List<Long>,
    )

    @Query(
        """
        UPDATE localSession SET labelName = :newLabel
        WHERE isArchived = 0
        AND id NOT IN (:ids)
        AND labelName IN (:labels)
        AND (:considerBreaks = 1 OR isWork = 1)
        """,
    )
    suspend fun updateLabelByIdsExcept(
        newLabel: String,
        ids: List<Long>,
        labels: List<String>,
        considerBreaks: Boolean,
    )

    @Query("SELECT * FROM localSession ORDER BY timestamp DESC")
    fun selectAll(): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    fun selectAfter(timestamp: Long): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE id = :id")
    fun selectById(id: Long): Flow<LocalSession>

    @Query("SELECT * FROM localSession WHERE isArchived = :isArchived ORDER BY timestamp DESC")
    fun selectByIsArchived(isArchived: Boolean): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE labelName = :labelName ORDER BY timestamp DESC")
    fun selectByLabel(labelName: String): Flow<List<LocalSession>>

    @Query("SELECT * FROM localSession WHERE labelName IN (:labelNames) ORDER BY timestamp ASC")
    fun selectByLabels(labelNames: List<String>): Flow<List<LocalSession>>

    @Query(
        """
        SELECT * FROM localSession
        WHERE labelName IN (:labelNames)
        AND timestamp >= :after
        ORDER BY timestamp ASC
    """,
    )
    fun selectByLabels(
        labelNames: List<String>,
        after: Long,
    ): Flow<List<LocalSession>>

    @Query(
        """
        SELECT * FROM localSession
        WHERE labelName IN (:labelNames)
        AND (:considerBreaks = 1 OR isWork = 1)
        ORDER BY timestamp DESC
    """,
    )
    fun selectSessionsForTimelinePaged(
        labelNames: List<String>,
        considerBreaks: Boolean,
    ): PagingSource<Int, LocalSession>

    @Query("SELECT COUNT(*) FROM localSession WHERE isArchived == 0 AND isWork == 1 AND timestamp >= :timestamp")
    fun selectNumberOfSessionsAfter(timestamp: Long): Flow<Int>

    @Query("DELETE FROM localSession WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query(
        """
        DELETE FROM localSession
        WHERE id NOT IN (:ids)
        AND labelName IN (:labels)
        AND (:considerBreaks = 1 OR isWork = 1)
        """,
    )
    suspend fun deleteExcept(
        ids: List<Long>,
        labels: List<String>,
        considerBreaks: Boolean,
    )

    @Query("DELETE FROM localSession")
    suspend fun deleteAll()

    // TODO: probably not needed together with the TRUNCATE journaling mode
    @RawQuery
    suspend fun checkpoint(query: RoomRawQuery = RoomRawQuery("PRAGMA wal_checkpoint(FULL)")): Int
}
