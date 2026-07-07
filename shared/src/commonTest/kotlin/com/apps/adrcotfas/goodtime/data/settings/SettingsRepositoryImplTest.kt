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

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.apps.adrcotfas.goodtime.RobolectricTest
import com.apps.adrcotfas.goodtime.bl.TimerRuntimeState
import com.apps.adrcotfas.goodtime.bl.TimerState
import com.apps.adrcotfas.goodtime.bl.TimerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

/**
 * Round-trip tests for the DataStore-backed settings, exercising the JSON
 * (de)serialization of the structured values in particular.
 */
class SettingsRepositoryImplTest : RobolectricTest() {
    private val tmpDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    private lateinit var scope: CoroutineScope
    private lateinit var repo: SettingsRepositoryImpl
    private lateinit var storePath: String

    @BeforeTest
    fun setup() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        storePath = "$tmpDir/settings-test-${Random.nextLong()}.preferences_pb"
        val dataStore =
            PreferenceDataStoreFactory.createWithPath(scope = scope) {
                storePath.toPath()
            }
        repo = SettingsRepositoryImpl(dataStore, Logger(StaticConfig()))
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        FileSystem.SYSTEM.delete(storePath.toPath(), mustExist = false)
    }

    @Test
    fun `defaults are emitted for an empty store`() =
        runTest {
            assertEquals(AppSettings(), repo.settings.first())
        }

    @Test
    fun `break budget data round-trips`() =
        runTest {
            val data = BreakBudgetData(breakBudget = 12.minutes, breakBudgetStart = 1234L, isAccumulating = true)
            repo.setBreakBudgetData(data)
            assertEquals(data, repo.settings.first().breakBudgetData)
        }

    @Test
    fun `long break data round-trips`() =
        runTest {
            val data = LongBreakData(streak = 3, lastWorkEndTime = 99_000L)
            repo.setLongBreakData(data)
            assertEquals(data, repo.settings.first().longBreakData)
        }

    @Test
    fun `persisted timer state round-trips and clears`() =
        runTest {
            val state =
                PersistedTimerState.from(
                    runtime =
                        TimerRuntimeState(
                            startTime = 1,
                            lastStartTime = 2,
                            endTime = 3,
                            state = TimerState.PAUSED,
                            type = TimerType.BREAK,
                            timeSpentPaused = 4,
                            timeAtPause = 5,
                            lastPauseTime = 6,
                        ),
                    savedAtWallClock = 7,
                    endTimeWallClock = 8,
                )
            repo.setPersistedTimerState(state)
            assertEquals(state, repo.settings.first().persistedTimerState)

            repo.clearPersistedTimerState()
            assertNull(repo.settings.first().persistedTimerState)
        }

    @Test
    fun `scalar settings round-trip`() =
        runTest {
            repo.setPro(true)
            repo.setLastInsertedSessionId(42L)
            repo.setAutoStartBreak(true)

            val settings = repo.settings.first()
            assertEquals(true, settings.isPro)
            assertEquals(42L, settings.lastInsertedSessionId)
            assertEquals(true, settings.autoStartBreak)
        }
}
