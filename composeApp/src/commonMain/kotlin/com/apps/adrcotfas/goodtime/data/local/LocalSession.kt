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
import com.apps.adrcotfas.goodtime.data.model.Label.Companion.DEFAULT_LABEL_NAME

@Entity(
    tableName = "localSession",
    foreignKeys = [
        ForeignKey(
            entity = LocalLabel::class,
            parentColumns = ["name", "isArchived"],
            childColumns = ["labelName", "isArchived"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_DEFAULT,
        ),
    ],
    indices = [
        Index(value = ["labelName", "isArchived"]),
        Index(value = ["isArchived"]),
        Index(value = ["labelName"]),
        Index(value = ["isWork"]),
    ],
)
data class LocalSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val timestamp: Long,
    val duration: Long,
    @ColumnInfo(defaultValue = "0")
    val interruptions: Long,
    @ColumnInfo(defaultValue = DEFAULT_LABEL_NAME)
    val labelName: String,
    @ColumnInfo(defaultValue = "")
    val notes: String,
    @ColumnInfo(defaultValue = "1")
    val isWork: Boolean,
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean,
)
