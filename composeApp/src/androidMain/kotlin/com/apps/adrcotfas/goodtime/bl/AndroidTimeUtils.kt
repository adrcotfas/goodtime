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
package com.apps.adrcotfas.goodtime.bl

import android.content.Context
import android.text.format.DateFormat
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import java.time.DayOfWeek
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Instant

fun List<DayOfWeek>.localizedNames(
    locale: Locale,
    textStyle: TextStyle = TextStyle.FULL_STANDALONE,
): List<String> =
    this.map {
        it.getDisplayName(textStyle, locale)
    }

object AndroidTimeUtils {
    fun Long.formatToPrettyDateAndTime(
        context: Context,
        locale: Locale = Locale.getDefault(),
    ): Pair<String, String> {
        val instant = Instant.fromEpochMilliseconds(this)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val date =
            dateTime.date.toJavaLocalDate().format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale),
            )

        val is24Hour = DateFormat.is24HourFormat(context)
        val timePattern = if (is24Hour) "HH:mm" else "hh:mm a"
        val timeFormatter = DateTimeFormatter.ofPattern(timePattern, locale)
        val time = dateTime.time.toJavaLocalTime().format(timeFormatter)

        return Pair(date, time)
    }

    private fun List<Month>.localizedMonthNames(
        locale: Locale,
        textStyle: TextStyle = TextStyle.FULL_STANDALONE,
    ): List<String> =
        this.map {
            it.getDisplayName(textStyle, locale)
        }

    private fun localizedDayNamesNarrow(locale: Locale): List<String> = DayOfWeek.entries.toList().localizedNames(locale, TextStyle.NARROW)

    private fun localizedDayNamesShort(locale: Locale): List<String> = DayOfWeek.entries.toList().localizedNames(locale, TextStyle.SHORT)

    fun localizedDayNamesFull(locale: Locale): List<String> = DayOfWeek.entries.toList().localizedNames(locale, TextStyle.FULL_STANDALONE)

    private fun localizedMonthNamesNarrow(locale: Locale) = Month.entries.toList().localizedMonthNames(locale, TextStyle.NARROW)

    private fun localizedMonthNamesShort(locale: Locale) = Month.entries.toList().localizedMonthNames(locale, TextStyle.SHORT)

    fun localizedMonthNamesFull(locale: Locale) = Month.entries.toList().localizedMonthNames(locale, TextStyle.FULL_STANDALONE)

    fun getLocalizedDayNamesForStats(locale: Locale): List<String> {
        val localizedDayNamesShort = localizedDayNamesShort(locale)
        return if (localizedDayNamesShort.any { it.length > 3 }) {
            return localizedDayNamesNarrow(locale)
        } else {
            localizedDayNamesShort
        }
    }

    fun getLocalizedMonthNamesForStats(locale: Locale): List<String> {
        val localizedMonthNamesShort = localizedMonthNamesShort(locale)
        return if (localizedMonthNamesShort.any { it.length > 3 }) {
            return localizedMonthNamesNarrow(locale)
        } else {
            localizedMonthNamesShort
        }
    }
}
