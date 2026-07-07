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
import com.apps.adrcotfas.goodtime.data.model.Label
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerProfileDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(timerProfile: LocalTimerProfile)

    @Query("UPDATE localLabel SET timerProfileName = :name WHERE name = '${Label.DEFAULT_LABEL_NAME}' ")
    suspend fun setDefaultLabelProfileName(name: String)

    @Transaction
    suspend fun insertTimerProfileAndSetDefault(timerProfile: LocalTimerProfile) {
        insert(timerProfile)
        setDefaultLabelProfileName(timerProfile.name)
    }

    @Query("DELETE FROM localTimerProfile WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT * FROM localTimerProfile WHERE name = :name")
    fun selectByName(name: String): Flow<LocalTimerProfile?>

    @Query("SELECT * FROM localTimerProfile WHERE name IN (:names)")
    fun selectByNames(names: List<String>): Flow<List<LocalTimerProfile>>

    @Query("SELECT * FROM localTimerProfile")
    fun selectAll(): Flow<List<LocalTimerProfile>>
}
