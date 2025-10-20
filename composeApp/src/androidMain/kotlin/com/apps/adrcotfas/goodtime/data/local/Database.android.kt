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
package com.apps.adrcotfas.goodtime.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.apps.adrcotfas.goodtime.data.local.migrations.MIGRATIONS

actual fun getRoomDatabase(builder: RoomDatabase.Builder<ProductivityDatabase>): ProductivityDatabase =
    builder
        .addMigrations(*MIGRATIONS)
        .setDriver(AndroidSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
