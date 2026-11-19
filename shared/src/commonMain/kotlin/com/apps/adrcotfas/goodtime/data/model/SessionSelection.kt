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

sealed interface SessionSelection {
    data class Only(
        val ids: Set<Long> = emptySet(),
    ) : SessionSelection

    /** Every session matching the timeline filter it was made from, minus [ids]. */
    data class AllExcept(
        val ids: Set<Long> = emptySet(),
        val total: Int,
        val labels: List<String>,
        val showBreaks: Boolean,
    ) : SessionSelection

    val count: Int
        get() =
            when (this) {
                is Only -> ids.size
                is AllExcept -> total - ids.size
            }

    val isEmpty: Boolean get() = count <= 0

    operator fun contains(id: Long): Boolean = when (this) {
        is Only -> id in ids
        is AllExcept -> id !in ids
    }

    fun toggle(id: Long): SessionSelection = when (this) {
        is Only -> copy(ids = ids.toggle(id))
        is AllExcept -> copy(ids = ids.toggle(id)).let { if (it.isEmpty) Only() else it }
    }
}

private fun Set<Long>.toggle(id: Long) = if (id in this) this - id else this + id
