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
package com.apps.adrcotfas.goodtime.data.model

import com.apps.adrcotfas.goodtime.data.model.Label.Companion.DEFAULT_LABEL_NAME

data class Session(
    val id: Long,
    val timestamp: Long, // milliseconds since epoch
    val duration: Long, // minutes
    val interruptions: Long, // minutes
    val label: String = DEFAULT_LABEL_NAME,
    val notes: String,
    val isWork: Boolean,
    val isArchived: Boolean,
) {
    companion object {
        fun create(
            timestamp: Long,
            duration: Long,
            interruptions: Long,
            label: String,
            isWork: Boolean,
        ) = Session(
            id = 0,
            timestamp = timestamp,
            duration = duration,
            interruptions = interruptions,
            label = label,
            notes = "",
            isWork = isWork,
            isArchived = false,
        )

        fun default() =
            Session(
                id = 0,
                timestamp = 0,
                duration = 0,
                interruptions = 0,
                label = DEFAULT_LABEL_NAME,
                notes = "",
                isWork = true,
                isArchived = false,
            )
    }
}
