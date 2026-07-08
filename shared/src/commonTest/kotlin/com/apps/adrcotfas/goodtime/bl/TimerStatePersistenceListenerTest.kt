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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TimerStatePersistenceListenerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private val timeProvider = FakeTimeProvider()
    private val logger = Logger(StaticConfig())
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var listener: TimerStatePersistenceListener

    @BeforeTest
    fun setup() {
        settingsRepo = FakeSettingsRepository()
        listener =
            TimerStatePersistenceListener(
                settingsRepo = settingsRepo,
                timeProvider = timeProvider,
                coroutineScope = testScope,
                log = logger,
            )
    }

    private val runningRuntime =
        TimerRuntimeState(
            startTime = 10_000,
            lastStartTime = 10_000,
            endTime = 110_000,
            state = TimerState.RUNNING,
            type = TimerType.FOCUS,
        )

    private suspend fun persisted() = settingsRepo.settings.first().persistedTimerState

    @Test
    fun `Start with active runtime persists state`() = runTest(testDispatcher) {
        timeProvider.elapsedRealtime = 20_000
        timeProvider.wallClock = 1_000_000

        listener.onEvent(Event.Start(runtimeState = runningRuntime))

        val state = persisted()
        assertEquals(runningRuntime.state.ordinal, state?.state)
        assertEquals(runningRuntime.endTime, state?.endTime)
        assertEquals(1_000_000, state?.savedAtWallClock)
        // endTimeWallClock = now - elapsedRealtime + endTime
        assertEquals(1_000_000 - 20_000 + 110_000, state?.endTimeWallClock)
    }

    @Test
    fun `Pause with active runtime persists state`() = runTest(testDispatcher) {
        listener.onEvent(Event.Pause(runtimeState = runningRuntime.copy(state = TimerState.PAUSED)))

        assertEquals(TimerState.PAUSED.ordinal, persisted()?.state)
    }

    @Test
    fun `inactive runtime is not persisted`() = runTest(testDispatcher) {
        listener.onEvent(Event.Start(runtimeState = TimerRuntimeState(state = TimerState.RESET)))

        assertNull(persisted())
    }

    @Test
    fun `Reset clears persisted state`() = runTest(testDispatcher) {
        listener.onEvent(Event.Start(runtimeState = runningRuntime))
        assertEquals(runningRuntime.state.ordinal, persisted()?.state)
        listener.onEvent(Event.Reset)

        assertNull(persisted())
    }

    @Test
    fun `Finished clears persisted state`() = runTest(testDispatcher) {
        listener.onEvent(Event.Start(runtimeState = runningRuntime))
        listener.onEvent(Event.Finished(type = TimerType.FOCUS))

        assertNull(persisted())
    }

    @Test
    fun `round trip - persisted state restores to the same runtime`() = runTest(testDispatcher) {
        timeProvider.elapsedRealtime = 20_000
        timeProvider.wallClock = 1_000_000

        listener.onEvent(Event.Start(runtimeState = runningRuntime))

        var restored: TimerRuntimeState? = null
        TimerStateRestoration(
            settingsRepo = settingsRepo,
            timeProvider = timeProvider,
            log = logger,
            coroutineScope = testScope,
        ).restoreTimerState { restored = it }

        assertEquals(runningRuntime, restored)
    }
}
