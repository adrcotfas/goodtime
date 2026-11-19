/**
 *     Goodtime Productivity
 *     Copyright (C) Adrian Cotfas
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

import com.apps.adrcotfas.goodtime.data.model.SessionSelection.AllExcept
import com.apps.adrcotfas.goodtime.data.model.SessionSelection.Only
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionSelectionTest {
    @Test
    fun manualSelectionTogglesIds() {
        val selection = Only().toggle(1).toggle(2).toggle(1)
        assertEquals(1, selection.count)
        assertTrue(2L in selection)
        assertFalse(1L in selection)
        assertTrue(selection.toggle(2).isEmpty)
    }

    @Test
    fun selectAllCountsDownAsSessionsAreDeselected() {
        val selection = AllExcept(total = 3, labels = listOf("a"), showBreaks = false).toggle(1)
        assertEquals(2, selection.count)
        assertFalse(1L in selection)
        assertTrue(42L in selection)
        assertEquals(3, selection.toggle(1).count)
    }

    @Test
    fun deselectingEverythingLeavesSelectAll() {
        val selection = AllExcept(total = 2, labels = listOf("a"), showBreaks = true).toggle(1).toggle(2)
        assertEquals(Only(), selection)
        assertTrue(selection.isEmpty)
    }
}
