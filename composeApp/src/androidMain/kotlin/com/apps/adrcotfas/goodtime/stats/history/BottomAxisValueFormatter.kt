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
package com.apps.adrcotfas.goodtime.stats.history

import com.apps.adrcotfas.goodtime.common.Time.toLocalDateTime
import com.apps.adrcotfas.goodtime.common.firstDayOfWeekInMonth
import com.apps.adrcotfas.goodtime.common.quarter
import com.apps.adrcotfas.goodtime.data.settings.HistoryIntervalType
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month

val extraBottomAxisStrings = ExtraStore.Key<BottomAxisStrings>()
val extraViewType = ExtraStore.Key<HistoryIntervalType>()
val extraFirstDayOfWeek = ExtraStore.Key<DayOfWeek>()
val timestampsKey = ExtraStore.Key<List<Long>>()

data class BottomAxisStrings(
    val dayOfWeekNames: List<String>,
    val monthsOfYearNames: List<String>,
)

val BottomAxisValueFormatter =
    CartesianValueFormatter { context, x, _ ->
        val value = context.model.extraStore[timestampsKey][x.toInt()]
        val strings = context.model.extraStore[extraBottomAxisStrings]
        val type = context.model.extraStore[extraViewType]
        val firstDayOfWeek = context.model.extraStore[extraFirstDayOfWeek]

        val localDate = toLocalDateTime(value).date
        val dayOfMonth = localDate.dayOfMonth

        when (type) {
            HistoryIntervalType.DAYS -> {
                if (dayOfMonth == 1) {
                    strings.monthsOfYearNames[localDate.month.ordinal] + "\n" + localDate.year
                } else {
                    dayOfMonth.toString()
                }
            }

            HistoryIntervalType.WEEKS -> {
                if (localDate.firstDayOfWeekInMonth(firstDayOfWeek) == localDate) {
                    strings.monthsOfYearNames[localDate.month.ordinal] + "\n" + localDate.year
                } else {
                    dayOfMonth.toString()
                }
            }

            HistoryIntervalType.MONTHS -> {
                val monthString = strings.monthsOfYearNames[localDate.month.ordinal]
                if (localDate.month == Month.JANUARY) {
                    monthString + "\n" + localDate.year.toString()
                } else {
                    monthString
                }
            }

            HistoryIntervalType.QUARTERS -> {
                val quarterOrdinal = localDate.quarter
                if (quarterOrdinal == 1) {
                    "Q1\n" + localDate.year.toString()
                } else {
                    "Q$quarterOrdinal"
                }
            }

            HistoryIntervalType.YEARS -> {
                localDate.year.toString()
            }
        }
    }
