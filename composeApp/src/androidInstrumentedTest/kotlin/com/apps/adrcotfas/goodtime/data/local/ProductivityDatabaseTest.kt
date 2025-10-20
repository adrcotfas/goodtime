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

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apps.adrcotfas.goodtime.data.local.migrations.MIGRATION_6_7
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ProductivityDatabaseTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            ProductivityDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL("INSERT INTO Label (title, colorId, `order`, archived) VALUES ('Work', 1, 1, 0)")
            execSQL("INSERT INTO Label (title, colorId, `order`, archived) VALUES ('Personal', 2, 2, 0)")

            execSQL("INSERT INTO Session (timestamp, duration, label, archived) VALUES (1627849200000, 25, 'Work', 0)")
            execSQL("INSERT INTO Session (timestamp, duration, label, archived) VALUES (1627852800000, 30, 'Personal', 0)")

            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
