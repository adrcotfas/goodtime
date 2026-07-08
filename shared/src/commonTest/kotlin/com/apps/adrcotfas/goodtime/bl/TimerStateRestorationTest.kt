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
package com.apps.adrcotfas.goodtime.bl

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.apps.adrcotfas.goodtime.data.settings.PersistedTimerState
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TimerStateRestorationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private val timeProvider = FakeTimeProvider()
    private val logger = Logger(StaticConfig())
    private lateinit var settingsRepo: FakeSettingsRepository

    private var restored: TimerRuntimeState? = null

    @BeforeTest
    fun setup() {
        settingsRepo = FakeSettingsRepository()
        restored = null
    }

    private fun restoration() = TimerStateRestoration(
        settingsRepo = settingsRepo,
        timeProvider = timeProvider,
        log = logger,
        coroutineScope = testScope,
    )

    private fun runningState(
        startTime: Long,
        endTime: Long,
        savedAtWallClock: Long,
        endTimeWallClock: Long,
    ) = PersistedTimerState(
        state = TimerState.RUNNING.ordinal,
        type = TimerType.FOCUS.ordinal,
        startTime = startTime,
        lastStartTime = startTime,
        endTime = endTime,
        savedAtWallClock = savedAtWallClock,
        endTimeWallClock = endTimeWallClock,
    )

    @Test
    fun `expired running timer is not restored and state is cleared`() = runTest(testDispatcher) {
        settingsRepo.setPersistedTimerState(
            runningState(startTime = 0, endTime = 100_000, savedAtWallClock = 50_000, endTimeWallClock = 200_000),
        )
        timeProvider.elapsedRealtime = 300_000 // now() >= endTimeWallClock

        restoration().restoreTimerState { restored = it }

        assertNull(restored)
        assertNull(settingsRepo.settings.first().persistedTimerState)
    }

    @Test
    fun `state is restored directly when device was not rebooted`() = runTest(testDispatcher) {
        val persisted =
            runningState(startTime = 10_000, endTime = 100_000, savedAtWallClock = 50_000, endTimeWallClock = 200_000)
        settingsRepo.setPersistedTimerState(persisted)
        timeProvider.elapsedRealtime = 60_000 // >= startTime, so no reboot; < endTimeWallClock

        restoration().restoreTimerState { restored = it }

        assertEquals(persisted.toRuntimeState(), restored)
    }

    @Test
    fun `times are recalculated from wall clock after a reboot`() = runTest(testDispatcher) {
        // before reboot: started at elapsed 100_000, ends at elapsed 200_000 (duration 100_000)
        // wall clock: saved at 1_000_000, ends at 1_050_000
        settingsRepo.setPersistedTimerState(
            runningState(
                startTime = 100_000,
                endTime = 200_000,
                savedAtWallClock = 1_000_000,
                endTimeWallClock = 1_050_000,
            ),
        )
        // after reboot: elapsedRealtime restarted below startTime, wall clock advanced 20s
        timeProvider.elapsedRealtime = 5_000
        timeProvider.wallClock = 1_020_000

        restoration().restoreTimerState { restored = it }

        val result = restored
        assertNotNull(result)
        // remaining wall-clock time is 30_000, so new end = 5_000 + 30_000
        assertEquals(35_000, result.endTime)
        // original duration (100_000) is preserved relative to the new end time
        assertEquals(35_000 - 100_000, result.startTime)
        assertEquals(TimerState.RUNNING, result.state)
        assertEquals(TimerType.FOCUS, result.type)
    }
}
