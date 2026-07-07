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

import kotlin.test.Test
import kotlin.test.assertTrue

class NumberSuffixTest {
    @Test
    fun `Generate Unique Name For Duplicate`() {
        val existingNames = listOf("Default", "Label1", "Label2", "Label3", "Label666")
        var result = generateUniqueNameForDuplicate("Label", existingNames)
        assertTrue(result == "Label4")
        result = generateUniqueNameForDuplicate("Default", existingNames)
        assertTrue(result == "Default2")
        result = generateUniqueNameForDuplicate("Label666", existingNames)
        assertTrue(result == "Label667")
    }
}
