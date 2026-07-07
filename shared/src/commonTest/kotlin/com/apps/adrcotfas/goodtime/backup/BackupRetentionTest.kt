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
package com.apps.adrcotfas.goodtime.backup

import kotlin.test.Test
import kotlin.test.assertEquals

class BackupRetentionTest {
    @Test
    fun `itemsToDeleteForRetention returns items after keep`() {
        val items = listOf(5, 4, 3, 2, 1) // newest-first
        assertEquals(listOf(2, 1), itemsToDeleteForRetention(items, keep = 3))
    }

    @Test
    fun `itemsToDeleteForRetention returns empty when size is less or equal to keep`() {
        val items = listOf(3, 2, 1)
        assertEquals(emptyList(), itemsToDeleteForRetention(items, keep = 3))
    }
}
