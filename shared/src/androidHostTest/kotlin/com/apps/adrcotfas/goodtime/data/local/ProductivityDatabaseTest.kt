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
import androidx.test.platform.app.InstrumentationRegistry
import com.apps.adrcotfas.goodtime.data.local.migrations.MIGRATION_6_7
import com.apps.adrcotfas.goodtime.data.local.migrations.MIGRATION_7_8
import com.apps.adrcotfas.goodtime.data.local.migrations.MIGRATION_8_9
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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

    @Test
    @Throws(IOException::class)
    fun migrate7To8_remapsLegacyColorIndex() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL(
                "INSERT INTO localLabel (name, colorIndex, orderIndex, useDefaultTimeProfile, isArchived) " +
                    "VALUES ('Legacy', 42, 1, 1, 0)",
            )
            execSQL(
                "INSERT INTO localLabel (name, colorIndex, orderIndex, useDefaultTimeProfile, isArchived) " +
                    "VALUES ('Kept', 7, 2, 1, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)
        db.query("SELECT colorIndex FROM localLabel WHERE name = 'Legacy'").use {
            assertTrue(it.moveToFirst())
            assertEquals(24, it.getInt(0))
        }
        db.query("SELECT colorIndex FROM localLabel WHERE name = 'Kept'").use {
            assertTrue(it.moveToFirst())
            assertEquals(7, it.getInt(0))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9_introducesTimerProfilesAndKeepsLabels() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                "INSERT INTO localLabel (name, colorIndex, orderIndex, useDefaultTimeProfile, " +
                    "isCountdown, workDuration, isBreakEnabled, breakDuration, isLongBreakEnabled, " +
                    "longBreakDuration, sessionsBeforeLongBreak, workBreakRatio, isArchived) " +
                    "VALUES ('Study', 5, 1, 0, 1, 45, 1, 10, 1, 20, 3, 3, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)
        // custom label data is carried over
        db
            .query(
                "SELECT colorIndex, workDuration, breakDuration, sessionsBeforeLongBreak " +
                    "FROM localLabel WHERE name = 'Study'",
            ).use {
                assertTrue(it.moveToFirst())
                assertEquals(5, it.getInt(0))
                assertEquals(45, it.getInt(1))
                assertEquals(10, it.getInt(2))
                assertEquals(3, it.getInt(3))
            }
        // the four default timer profiles are seeded
        db.query("SELECT COUNT(*) FROM localTimerProfile").use {
            assertTrue(it.moveToFirst())
            assertEquals(4, it.getInt(0))
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
