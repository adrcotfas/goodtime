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

import com.apps.adrcotfas.goodtime.bl.LabelData
import kotlin.random.Random

data class Label(
    val name: String,
    val colorIndex: Long = DEFAULT_LABEL_COLOR_INDEX.toLong(),
    val orderIndex: Long = Long.MAX_VALUE,
    val useDefaultTimeProfile: Boolean = true,
    val timerProfile: TimerProfile = TimerProfile(),
    val isArchived: Boolean = false,
) {
    companion object {
        // the internal name of the default, built-in label which cannot be deleted
        const val DEFAULT_LABEL_NAME = "PRODUCTIVITY_DEFAULT_LABEL"

        // the internal name of the virtual label used for displaying aggregated data
        const val OTHERS_LABEL_NAME = "PRODUCTIVITY_OTHERS_LABEL"
        const val BREAK_COLOR_INDEX = 23
        const val DEFAULT_LABEL_COLOR_INDEX = 24
        const val OTHERS_LABEL_COLOR_INDEX = 24
        const val LABEL_NAME_MAX_LENGTH = 32

        fun defaultLabel() =
            Label(
                name = DEFAULT_LABEL_NAME,
                colorIndex = DEFAULT_LABEL_COLOR_INDEX.toLong(),
                orderIndex = 0,
                timerProfile = TimerProfile.default(),
            )

        fun newLabelWithRandomColorIndex(lastIndex: Int) = Label(name = "", colorIndex = Random.nextInt(lastIndex).toLong())
    }
}

fun Label.isDefault() = name == Label.DEFAULT_LABEL_NAME

fun Label.getLabelData() = LabelData(name = this.name, colorIndex = this.colorIndex)
