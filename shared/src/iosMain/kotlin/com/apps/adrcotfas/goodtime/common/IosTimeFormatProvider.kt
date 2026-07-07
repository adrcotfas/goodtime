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

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

class IosTimeFormatProvider : TimeFormatProvider {
    override fun is24HourFormat(): Boolean {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale
        formatter.dateStyle = platform.Foundation.NSDateFormatterNoStyle
        formatter.timeStyle = platform.Foundation.NSDateFormatterShortStyle

        // The format string for short time will contain 'a' (AM/PM marker) for 12-hour format
        val dateFormat = formatter.dateFormat ?: return true
        return !dateFormat.contains("a")
    }
}
