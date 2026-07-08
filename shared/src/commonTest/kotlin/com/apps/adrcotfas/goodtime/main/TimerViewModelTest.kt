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
package com.apps.adrcotfas.goodtime.main

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.apps.adrcotfas.goodtime.bl.BreakBudgetManager
import com.apps.adrcotfas.goodtime.bl.FinishedSessionsHandler
import com.apps.adrcotfas.goodtime.bl.StreakManager
import com.apps.adrcotfas.goodtime.bl.TimerManager
import com.apps.adrcotfas.goodtime.bl.TimerStateRestoration
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepositoryImpl
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_WORK_DURATION
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeEventListener
import com.apps.adrcotfas.goodtime.fakes.FakeInstallDateProvider
import com.apps.adrcotfas.goodtime.fakes.FakeLabelDao
import com.apps.adrcotfas.goodtime.fakes.FakeSessionDao
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import com.apps.adrcotfas.goodtime.fakes.FakeTimerProfileDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Guards the split of the ticking time out of [TimerUiState] (review point #30): only the
 * display/idle time may tick per second; [TimerUiState] itself must stay stable while running.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())
    private val timeProvider = FakeTimeProvider()
    private val logger = Logger(StaticConfig())

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var localDataRepo: LocalDataRepository
    private lateinit var timerManager: TimerManager
    private lateinit var viewModel: TimerViewModel

    private val defaultDuration = DEFAULT_WORK_DURATION.minutes.inWholeMilliseconds

    @BeforeTest
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        timeProvider.elapsedRealtime = 0L
        settingsRepo = FakeSettingsRepository()
        localDataRepo =
            LocalDataRepositoryImpl(
                sessionDao = FakeSessionDao(),
                labelDao = FakeLabelDao(),
                timerProfileDao = FakeTimerProfileDao(),
                settingsRepo = settingsRepo,
                coroutineScope = testScope,
            )
        localDataRepo.updateDefaultLabel(Label.defaultLabel())

        timerManager =
            TimerManager(
                localDataRepo = localDataRepo,
                settingsRepo = settingsRepo,
                listeners = listOf(FakeEventListener()),
                timeProvider = timeProvider,
                finishedSessionsHandler =
                FinishedSessionsHandler(testScope, localDataRepo, settingsRepo, logger),
                breakBudgetManager = BreakBudgetManager(settingsRepo, timeProvider, testScope, logger),
                streakManager = StreakManager(settingsRepo, timeProvider, testScope, logger),
                log = logger,
                coroutineScope = testScope,
                timerStateRestoration = TimerStateRestoration(settingsRepo, timeProvider, logger, testScope),
            )
        timerManager.setup()
        viewModel =
            TimerViewModel(timerManager, timeProvider, settingsRepo, localDataRepo, FakeInstallDateProvider())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `displayTime ticks down while running`() = runTest(testDispatcher) {
        timerManager.start(TimerType.FOCUS)
        viewModel.displayTime.test {
            var first = awaitItem()
            while (first == 0L) first = awaitItem() // skip the StateFlow seed
            assertEquals(defaultDuration, first)

            timeProvider.elapsedRealtime += 60_000
            advanceTimeBy(60_000)
            assertEquals(defaultDuration - 60_000, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `timerUiState does not re-emit on a one-second tick while running`() = runTest(testDispatcher) {
        timerManager.start(TimerType.FOCUS)
        viewModel.timerUiState.test {
            var state = awaitItem()
            while (!state.isActive) state = awaitItem() // reach the running snapshot

            // A second passes: the display ticks, but the stable state must not re-emit.
            timeProvider.elapsedRealtime += 1_000
            advanceTimeBy(1_000)
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `idleTime is measured from the finish time`() = runTest(testDispatcher) {
        timerManager.start(TimerType.FOCUS)
        timeProvider.elapsedRealtime = defaultDuration
        timerManager.finish()

        assertEquals(0L, viewModel.currentIdleTime())
        assertTrue(viewModel.isWithinInactivityTimeout())

        timeProvider.elapsedRealtime += 5_000
        assertEquals(5_000L, viewModel.currentIdleTime())
    }
}
