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
package com.apps.adrcotfas.goodtime.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

suspend inline fun <reified T> DataStore<Preferences>.add(
    key: Preferences.Key<String>,
    value: T,
) {
    val json = Json { ignoreUnknownKeys = true }
    val existingValues =
        data
            .map {
                it[key]?.let { content -> json.decodeFromString<MutableSet<T>>(content) } ?: mutableSetOf()
            }.first()

    existingValues.add(value)
    edit { it[key] = json.encodeToString(existingValues) }
}

suspend inline fun <reified T> DataStore<Preferences>.remove(
    key: Preferences.Key<String>,
    value: T,
) {
    val json = Json { ignoreUnknownKeys = true }

    val existingValues =
        data
            .map {
                it[key]?.let { content -> json.decodeFromString<MutableSet<T>>(content) } ?: mutableSetOf()
            }.first()

    existingValues.remove(value)
    edit { it[key] = json.encodeToString(existingValues) }
}
