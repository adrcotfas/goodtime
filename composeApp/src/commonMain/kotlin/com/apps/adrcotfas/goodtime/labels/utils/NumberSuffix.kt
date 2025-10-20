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
package com.apps.adrcotfas.goodtime.labels.utils

fun generateUniqueNameForDuplicate(
    name: String,
    existingNames: List<String>,
): String = generateUniqueNameForDuplicate(name, existingNames, null)

private tailrec fun generateUniqueNameForDuplicate(
    name: String,
    existingNames: List<String>,
    suffix: Int? = null,
): String {
    val tmpSuffix = suffix ?: suffix ?: name.getNumberSuffix()
    val newName =
        tmpSuffix?.let {
            name.dropLast(tmpSuffix.toString().length) + (tmpSuffix + 1)
        } ?: (name + "2")
    return if (existingNames.any { it == newName }) {
        generateUniqueNameForDuplicate(newName, existingNames, tmpSuffix?.let { it + 1 } ?: 2)
    } else {
        newName
    }
}

private fun String.getNumberSuffix(): Int? {
    val matchResult = "\\d+$".toRegex().find(this)
    return matchResult?.value?.toInt()
}
