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
package com.apps.adrcotfas.goodtime.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.apps.adrcotfas.goodtime.data.local.LocalTimerProfile

val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE labels_new (title TEXT NOT NULL, colorId INTEGER NOT NULL, 'order' INTEGER NOT NULL DEFAULT 0, archived INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(title, archived))",
            )
            connection.execSQL(
                "INSERT INTO labels_new (title, colorId) SELECT label, color FROM LabelAndColor",
            )
            connection.execSQL("DROP TABLE LabelAndColor")
            connection.execSQL("ALTER TABLE labels_new RENAME TO Label")
            connection.execSQL(
                "CREATE TABLE sessions_new (id INTEGER NOT NULL, timestamp INTEGER NOT NULL, duration INTEGER NOT NULL, label TEXT, archived INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id), FOREIGN KEY(label, archived) REFERENCES Label(title, archived) ON UPDATE CASCADE ON DELETE SET DEFAULT)",
            )
            connection.execSQL(
                "INSERT INTO sessions_new (timestamp, duration, label) SELECT endTime, totalTime, label FROM Session",
            )
            connection.execSQL("DROP TABLE Session")
            connection.execSQL("ALTER TABLE sessions_new RENAME TO Session")
        }
    }

val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE Profile (name TEXT NOT NULL, durationWork INTEGER NOT NULL" +
                    ", durationBreak INTEGER NOT NULL" +
                    ", enableLongBreak INTEGER NOT NULL" +
                    ", durationLongBreak INTEGER NOT NULL" +
                    ", sessionsBeforeLongBreak INTEGER NOT NULL" +
                    ", PRIMARY KEY(name))",
            )
        }
    }

val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE sessions_new (id INTEGER NOT NULL, timestamp INTEGER NOT NULL, duration INTEGER NOT NULL, label TEXT, archived INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id), FOREIGN KEY(label, archived) REFERENCES Label(title, archived) ON UPDATE CASCADE ON DELETE SET DEFAULT)",
            )
            connection.execSQL(
                "INSERT INTO sessions_new (id, timestamp, duration, label, archived) SELECT id, timestamp, duration, label, archived FROM Session",
            )
            connection.execSQL("DROP TABLE Session")
            connection.execSQL("ALTER TABLE sessions_new RENAME TO Session")
        }
    }

val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            // do nothing here; it seems to be needed by the switch to kapt room compiler
        }
    }

val MIGRATION_5_6: Migration =
    object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE SessionTmp (id INTEGER NOT NULL, timestamp INTEGER NOT NULL, duration INTEGER NOT NULL, label TEXT, archived INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id), FOREIGN KEY(label, archived) REFERENCES Label(title, archived) ON UPDATE CASCADE ON DELETE SET DEFAULT)",
            )
            connection.execSQL(
                "INSERT INTO SessionTmp (timestamp, duration, label) SELECT timestamp, duration, label FROM Session",
            )
            connection.execSQL("DROP TABLE Session")
            connection.execSQL("ALTER TABLE SessionTmp RENAME TO Session")

            connection.execSQL(
                "CREATE TABLE LabelTmp (title TEXT NOT NULL DEFAULT '', colorId INTEGER NOT NULL DEFAULT 0, 'order' INTEGER NOT NULL DEFAULT 0, archived INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(title, archived))",
            )
            connection.execSQL(
                "INSERT INTO LabelTmp (title, colorId, 'order', archived) SELECT title, colorId, 'order', archived FROM Label",
            )
            connection.execSQL("DROP TABLE Label")
            connection.execSQL("ALTER TABLE LabelTmp RENAME TO Label")
        }
    }

val MIGRATION_6_7: Migration =
    object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("PRAGMA foreign_keys=off;")

            // labels
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS labelTmp (
                    name TEXT PRIMARY KEY NOT NULL,
                    colorIndex INTEGER NOT NULL DEFAULT 24,
                    orderIndex INTEGER NOT NULL DEFAULT ${Long.MAX_VALUE},
                    useDefaultTimeProfile INTEGER NOT NULL DEFAULT 1,

                    -- countdown columns
                    isCountdown INTEGER NOT NULL DEFAULT 1,
                    workDuration INTEGER NOT NULL DEFAULT 25,
                    isBreakEnabled INTEGER NOT NULL DEFAULT 1,
                    breakDuration INTEGER NOT NULL DEFAULT 5,
                    isLongBreakEnabled INTEGER NOT NULL DEFAULT 0,
                    longBreakDuration INTEGER NOT NULL DEFAULT 15,
                    sessionsBeforeLongBreak INTEGER NOT NULL DEFAULT 4,

                    -- flow column
                    workBreakRatio INTEGER NOT NULL DEFAULT 3,
                    isArchived INTEGER NOT NULL DEFAULT 0
                );
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO labelTmp(name, colorIndex, orderIndex, isArchived)
                SELECT COALESCE(title, 'PRODUCTIVITY_DEFAULT_LABEL'), colorId, 'order', archived FROM Label;
                """.trimIndent(),
            )
            connection.execSQL(
                """
                UPDATE labelTmp
                SET colorIndex = 24
                WHERE colorIndex = -1;
                """.trimIndent(),
            )

            connection.execSQL("DROP TABLE Label;")
            connection.execSQL("ALTER TABLE labelTmp RENAME TO localLabel;")

            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_localLabel_name_isArchived ON localLabel(name, isArchived);")

            // sessions
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sessionTmp (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    duration INTEGER NOT NULL,
                    interruptions INTEGER NOT NULL DEFAULT 0,
                    labelName TEXT NOT NULL DEFAULT 'PRODUCTIVITY_DEFAULT_LABEL',
                    notes TEXT NOT NULL DEFAULT '',
                    isWork INTEGER NOT NULL DEFAULT 1,
                    isArchived INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(labelName, isArchived) REFERENCES localLabel(name, isArchived)
                    ON UPDATE CASCADE
                    ON DELETE SET DEFAULT
                );
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO sessionTmp(id, timestamp, duration, interruptions, labelName, isArchived)
                SELECT id, timestamp, duration, 0, COALESCE(label, "PRODUCTIVITY_DEFAULT_LABEL"), archived
                FROM Session;
                """.trimIndent(),
            )
            connection.execSQL("DROP TABLE Session;")
            connection.execSQL("ALTER TABLE sessionTmp RENAME TO localSession;")

            // sessions indexes
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_localSession_isArchived ON localSession(isArchived);")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_localSession_labelName ON localSession(labelName);")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_localSession_isWork ON localSession(isWork);")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_localSession_labelName_isArchived ON localSession(labelName, isArchived);")

            connection.execSQL("PRAGMA foreign_keys=on;")
            // profile clean-up
            connection.execSQL("DROP TABLE Profile;")
        }
    }

val MIGRATION_7_8: Migration =
    object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                UPDATE localLabel
                SET colorIndex = 24
                WHERE colorIndex = 42;
                """.trimIndent(),
            )
        }
    }

val MIGRATION_8_9: Migration =
    object : Migration(8, 9) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("PRAGMA foreign_keys=off;")

            // Create LocalTimerProfile table
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS localTimerProfile (
                    name TEXT PRIMARY KEY NOT NULL,
                    isCountdown INTEGER NOT NULL DEFAULT 1,
                    workDuration INTEGER NOT NULL DEFAULT 25,
                    isBreakEnabled INTEGER NOT NULL DEFAULT 1,
                    breakDuration INTEGER NOT NULL DEFAULT 5,
                    isLongBreakEnabled INTEGER NOT NULL DEFAULT 0,
                    longBreakDuration INTEGER NOT NULL DEFAULT 15,
                    sessionsBeforeLongBreak INTEGER NOT NULL DEFAULT 4,
                    workBreakRatio INTEGER NOT NULL DEFAULT 3
                );
                """.trimIndent(),
            )

            // Insert default timer profiles matching the ones in insertDefaultLabel
            connection.execSQL(
                """
                INSERT INTO localTimerProfile (name, isCountdown, workDuration, isBreakEnabled, breakDuration, isLongBreakEnabled, longBreakDuration, sessionsBeforeLongBreak, workBreakRatio)
                VALUES ('${LocalTimerProfile.DEFAULT_PROFILE_NAME}', 1, 25, 1, 5, 0, 15, 4, 3);
                """.trimIndent(),
            )

            connection.execSQL(
                """
                INSERT INTO localTimerProfile (name, isCountdown, workDuration, isBreakEnabled, breakDuration, isLongBreakEnabled, longBreakDuration, sessionsBeforeLongBreak, workBreakRatio)
                VALUES ('${LocalTimerProfile.PROFILE_50_10_NAME}', 1, 50, 1, 10, 0, 15, 4, 3);
                """.trimIndent(),
            )

            connection.execSQL(
                """
                INSERT INTO localTimerProfile (name, isCountdown, workDuration, isBreakEnabled, breakDuration, isLongBreakEnabled, longBreakDuration, sessionsBeforeLongBreak, workBreakRatio)
                VALUES ('${LocalTimerProfile.POMODORO_PROFILE_NAME}', 1, 25, 1, 5, 1, 15, 4, 3);
                """.trimIndent(),
            )

            connection.execSQL(
                """
                INSERT INTO localTimerProfile (name, isCountdown, workDuration, isBreakEnabled, breakDuration, isLongBreakEnabled, longBreakDuration, sessionsBeforeLongBreak, workBreakRatio)
                VALUES ('${LocalTimerProfile.THIRD_TIME_PROFILE_NAME}', 0, 25, 1, 5, 0, 15, 4, 3);
                """.trimIndent(),
            )

            connection.execSQL(
                """
                CREATE TABLE localLabel_new (
                    name TEXT PRIMARY KEY NOT NULL,
                    colorIndex INTEGER NOT NULL DEFAULT 24,
                    orderIndex INTEGER NOT NULL DEFAULT ${Long.MAX_VALUE},
                    useDefaultTimeProfile INTEGER NOT NULL DEFAULT 1,
                    timerProfileName TEXT DEFAULT '${LocalTimerProfile.DEFAULT_PROFILE_NAME}',
                    isCountdown INTEGER NOT NULL DEFAULT 1,
                    workDuration INTEGER NOT NULL DEFAULT 25,
                    isBreakEnabled INTEGER NOT NULL DEFAULT 1,
                    breakDuration INTEGER NOT NULL DEFAULT 5,
                    isLongBreakEnabled INTEGER NOT NULL DEFAULT 0,
                    longBreakDuration INTEGER NOT NULL DEFAULT 15,
                    sessionsBeforeLongBreak INTEGER NOT NULL DEFAULT 4,
                    workBreakRatio INTEGER NOT NULL DEFAULT 3,
                    isArchived INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (timerProfileName) REFERENCES localTimerProfile(name)
                    ON UPDATE CASCADE ON DELETE SET NULL
                );
                """.trimIndent(),
            )

            connection.execSQL(
                """
                INSERT INTO localLabel_new (
                    name, colorIndex, orderIndex, useDefaultTimeProfile, timerProfileName,
                    isCountdown, workDuration, isBreakEnabled, breakDuration,
                    isLongBreakEnabled, longBreakDuration, sessionsBeforeLongBreak,
                    workBreakRatio, isArchived
                )
                SELECT
                    name, colorIndex, orderIndex, useDefaultTimeProfile, '${LocalTimerProfile.DEFAULT_PROFILE_NAME}',
                    isCountdown, workDuration, isBreakEnabled, breakDuration,
                    isLongBreakEnabled, longBreakDuration, sessionsBeforeLongBreak,
                    workBreakRatio, isArchived
                FROM localLabel;
                """.trimIndent(),
            )

            connection.execSQL("DROP TABLE localLabel")

            connection.execSQL("ALTER TABLE localLabel_new RENAME TO localLabel")

            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_localLabel_name_isArchived ON localLabel(name, isArchived);")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_localLabel_timerProfileName ON localLabel(timerProfileName);")

            connection.execSQL("PRAGMA foreign_keys=on;")
        }
    }

val MIGRATIONS =
    arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
    )
