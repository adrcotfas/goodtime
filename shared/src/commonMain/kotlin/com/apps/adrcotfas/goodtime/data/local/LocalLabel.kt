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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.apps.adrcotfas.goodtime.data.model.Label.Companion.DEFAULT_LABEL_COLOR_INDEX
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_BREAK_DURATION
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_LONG_BREAK_DURATION
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_PROFILE_NAME
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_SESSIONS_BEFORE_LONG_BREAK
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_WORK_BREAK_RATIO
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_WORK_DURATION

@Entity(
    tableName = "localLabel",
    foreignKeys = [
        ForeignKey(
            entity = LocalTimerProfile::class,
            parentColumns = ["name"],
            childColumns = ["timerProfileName"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["name", "isArchived"], unique = true),
        Index(value = ["timerProfileName"]),
    ],
)
data class LocalLabel(
    @PrimaryKey
    val name: String,
    @ColumnInfo(defaultValue = "$DEFAULT_LABEL_COLOR_INDEX")
    val colorIndex: Int,
    @ColumnInfo(defaultValue = "${Long.MAX_VALUE}")
    val orderIndex: Long,
    @ColumnInfo(defaultValue = "1")
    val useDefaultTimeProfile: Boolean,
    @ColumnInfo(defaultValue = DEFAULT_PROFILE_NAME)
    val timerProfileName: String? = null,
    @ColumnInfo(defaultValue = "1")
    val isCountdown: Boolean,
    @ColumnInfo(defaultValue = "$DEFAULT_WORK_DURATION")
    val workDuration: Int,
    @ColumnInfo(defaultValue = "1")
    val isBreakEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "$DEFAULT_BREAK_DURATION")
    val breakDuration: Int,
    @ColumnInfo(defaultValue = "0")
    val isLongBreakEnabled: Boolean,
    @ColumnInfo(defaultValue = "$DEFAULT_LONG_BREAK_DURATION")
    val longBreakDuration: Int,
    @ColumnInfo(defaultValue = "$DEFAULT_SESSIONS_BEFORE_LONG_BREAK")
    val sessionsBeforeLongBreak: Int,
    @ColumnInfo(defaultValue = "$DEFAULT_WORK_BREAK_RATIO")
    val workBreakRatio: Int,
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean,
)
