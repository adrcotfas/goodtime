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
package com.apps.adrcotfas.goodtime.common

import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.AmPmMarker
import kotlinx.datetime.format.char

fun secondsOfDayToTimerFormat(
    secondOfDay: Int,
    is24HourFormat: Boolean = true,
): String {
    val time = LocalTime.fromSecondOfDay(secondOfDay)
    return time.format(
        if (is24HourFormat) {
            LocalTime.Format {
                hour()
                char(':')
                minute()
            }
        } else {
            LocalTime.Format {
                amPmHour()
                char(':')
                minute()
                char(' ')
                amPmMarker(
                    AmPmMarker.AM.toString(),
                    AmPmMarker.PM.toString(),
                )
            }
        },
    )
}
