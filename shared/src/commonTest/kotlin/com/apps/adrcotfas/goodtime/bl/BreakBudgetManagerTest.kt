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

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.settings.BreakBudgetData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class BreakBudgetManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var breakBudgetManager: BreakBudgetManager
    private val timeProvider = FakeTimeProvider()
    private val logger = Logger(StaticConfig())

    private val countdownProfile = TimerProfile(isCountdown = true)
    private val countUpProfile = TimerProfile(isCountdown = false, workBreakRatio = 3) // 3 min work = 1 min break

    @BeforeTest
    fun setup() {
        timeProvider.elapsedRealtime = 0L
        settingsRepo = FakeSettingsRepository() // Fresh repo for each test
        breakBudgetManager = BreakBudgetManager(
            settingsRepo = settingsRepo,
            timeProvider = timeProvider,
            log = logger,
            coroutineScope = testScope,
        )
    }

    @Test
    fun `Initial budget is zero`() = runTest {
        breakBudgetManager.breakBudgetData.test {
            assertEquals(BreakBudgetData(0.minutes, 0), awaitItem())
        }
        assertEquals(0.minutes, breakBudgetManager.getPersistedBreakBudgetAmount())
    }

    @Test
    fun `Budget is always zero for countdown profile`() = runTest {
        // Set some initial budget to ensure it gets ignored
        settingsRepo.setBreakBudgetData(BreakBudgetData(10.minutes, 0))
        breakBudgetManager = BreakBudgetManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val budget = breakBudgetManager.getCurrentBreakBudget(
            timerType = TimerType.WORK,
            timerState = TimerState.RUNNING,
            timerProfile = countdownProfile,
            lastStartTime = 0L,
        )
        assertEquals(0.minutes, budget)

        val persistedBudget = breakBudgetManager.updateAndPersistBreakBudget(
            timerType = TimerType.WORK,
            timerState = TimerState.RUNNING,
            timerProfile = countdownProfile,
            lastStartTime = 0L,
        )
        assertEquals(0.minutes, persistedBudget)
        assertEquals(0.minutes, breakBudgetManager.getPersistedBreakBudgetAmount())
    }

    @Test
    fun `Count-up work running earns budget`() = runTest {
        val workDuration = 6.minutes
        val expectedBudget = workDuration / countUpProfile.workBreakRatio // 6 / 3 = 2 minutes

        timeProvider.elapsedRealtime = 0 // Start time
        val lastStartTime = timeProvider.elapsedRealtime

        timeProvider.elapsedRealtime += workDuration.inWholeMilliseconds // Advance time
        testScope.advanceTimeBy(workDuration.inWholeMilliseconds)

        val currentBudget = breakBudgetManager.getCurrentBreakBudget(
            timerType = TimerType.WORK,
            timerState = TimerState.RUNNING,
            timerProfile = countUpProfile,
            lastStartTime = lastStartTime,
        )
        assertEquals(expectedBudget, currentBudget)
    }

    @Test
    fun `Count-up work running earns budget on top of existing budget`() = runTest {
        val initialBudget = 5.minutes
        settingsRepo.setBreakBudgetData(BreakBudgetData(initialBudget, 0))
        breakBudgetManager = BreakBudgetManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val workDuration = 6.minutes
        val earnedBudget = workDuration / countUpProfile.workBreakRatio // 2 minutes
        val expectedTotalBudget = initialBudget + earnedBudget // 5 + 2 = 7 minutes

        timeProvider.elapsedRealtime = 1000 // Start time slightly later
        val lastStartTime = timeProvider.elapsedRealtime

        timeProvider.elapsedRealtime += workDuration.inWholeMilliseconds // Advance time
        testScope.advanceTimeBy(workDuration.inWholeMilliseconds)

        val currentBudget = breakBudgetManager.getCurrentBreakBudget(
            timerType = TimerType.WORK,
            timerState = TimerState.RUNNING,
            timerProfile = countUpProfile,
            lastStartTime = lastStartTime,
        )
        assertEquals(expectedTotalBudget, currentBudget)
    }

    @Test
    fun `Count-up work paused decays budget`() = runTest {
        val initialBudget = 10.minutes
        val budgetStartTime = 1000L
        settingsRepo.setBreakBudgetData(BreakBudgetData(initialBudget, budgetStartTime))
        breakBudgetManager = BreakBudgetManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val pauseDuration = 3.minutes
        timeProvider.elapsedRealtime = budgetStartTime + pauseDuration.inWholeMilliseconds // Time is now 3 minutes after budget start
        testScope.advanceTimeBy(pauseDuration.inWholeMilliseconds)

        val currentBudget = breakBudgetManager.getCurrentBreakBudget(
            timerType = TimerType.WORK,
            timerState = TimerState.PAUSED, // Paused state
            timerProfile = countUpProfile,
            lastStartTime = 0L, // Not relevant for paused state
        )
        assertEquals(initialBudget - pauseDuration, currentBudget) // 10 - 3 = 7 minutes
    }

    @Test
    fun `Count-up work reset decays budget`() = runTest {
        val initialBudget = 10.minutes
        val budgetStartTime = 1000L
        settingsRepo.setBreakBudgetData(BreakBudgetData(initialBudget, budgetStartTime))
        breakBudgetManager = BreakBudgetManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val resetDuration = 4.minutes
        timeProvider.elapsedRealtime = budgetStartTime + resetDuration.inWholeMilliseconds
        testScope.advanceTimeBy(resetDuration.inWholeMilliseconds)

        val currentBudget = breakBudgetManager.getCurrentBreakBudget(
            timerType = TimerType.WORK,
            timerState = TimerState.RESET, // Reset state
            timerProfile = countUpProfile,
            lastStartTime = 0L, // Not relevant for reset state
        )
        assertEquals(initialBudget - resetDuration, currentBudget) // 10 - 4 = 6 minutes
    }

    @Test
    fun `Count-up break running decays budget`() = runTest {
        val initialBudget = 8.minutes
        val budgetStartTime = 500L
        settingsRepo.setBreakBudgetData(BreakBudgetData(initialBudget, budgetStartTime))
        breakBudgetManager = BreakBudgetManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val breakDuration = 2.minutes
        timeProvider.elapsedRealtime = budgetStartTime + breakDuration.inWholeMilliseconds
        testScope.advanceTimeBy(breakDuration.inWholeMilliseconds)

        val currentBudget = breakBudgetManager.getCurrentBreakBudget(
            timerType = TimerType.BREAK, // Break type
            timerState = TimerState.RUNNING, // Running state
            timerProfile = countUpProfile,
            lastStartTime = 0L, // Not relevant for break state
        )
        assertEquals(initialBudget - breakDuration, currentBudget) // 8 - 2 = 6 minutes
    }

    @Test
    fun `Budget decay stops at zero`() = runTest {
        val initialBudget = 5.minutes
        val budgetStartTime = 1000L
        settingsRepo.setBreakBudgetData(BreakBudgetData(initialBudget, budgetStartTime))
        breakBudgetManager = BreakBudgetManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val longDuration = 10.minutes // More than the initial budget
        timeProvider.elapsedRealtime = budgetStartTime + longDuration.inWholeMilliseconds
        testScope.advanceTimeBy(longDuration.inWholeMilliseconds)

        val currentBudget = breakBudgetManager.getCurrentBreakBudget(
            timerType = TimerType.BREAK,
            timerState = TimerState.RUNNING,
            timerProfile = countUpProfile,
            lastStartTime = 0L,
        )
        assertEquals(0.minutes, currentBudget) // Should not be negative
    }

    @Test
    fun `updateAndPersistBreakBudget calculates saves and returns budget`() = runTest {
        val workDuration = 9.minutes
        val expectedBudget = workDuration / countUpProfile.workBreakRatio // 9 / 3 = 3 minutes

        timeProvider.elapsedRealtime = 0
        val lastStartTime = timeProvider.elapsedRealtime

        timeProvider.elapsedRealtime += workDuration.inWholeMilliseconds // Advance time during work
        testScope.advanceTimeBy(workDuration.inWholeMilliseconds)

        val returnedBudget = breakBudgetManager.updateAndPersistBreakBudget(
            timerType = TimerType.WORK,
            timerState = TimerState.RUNNING,
            timerProfile = countUpProfile,
            lastStartTime = lastStartTime,
        )

        assertEquals(expectedBudget, returnedBudget)

        // Verify persisted data via flow
        breakBudgetManager.breakBudgetData.test {
            // Skip initial emission if any
            val emission = awaitItem()
            if (emission.breakBudget == 0.minutes && emission.breakBudgetStart == 0L) {
                assertEquals(BreakBudgetData(expectedBudget, timeProvider.elapsedRealtime), awaitItem())
            } else {
                assertEquals(BreakBudgetData(expectedBudget, timeProvider.elapsedRealtime), emission)
            }
            cancelAndIgnoreRemainingEvents()
        }

        // Verify persisted data via direct repo check (optional, but good for confirmation)
        settingsRepo.settings.test {
            assertEquals(BreakBudgetData(expectedBudget, timeProvider.elapsedRealtime), awaitItem().breakBudgetData)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(expectedBudget, breakBudgetManager.getPersistedBreakBudgetAmount())
    }

    @Test
    fun `updateAndPersistBreakBudget resets budget start time`() = runTest {
        val initialBudget = 5.minutes
        val initialStartTime = 1000L
        settingsRepo.setBreakBudgetData(BreakBudgetData(initialBudget, initialStartTime))
        breakBudgetManager = BreakBudgetManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val workDuration = 3.minutes
        val expectedEarned = workDuration / countUpProfile.workBreakRatio // 1 minute
        val expectedTotal = initialBudget + expectedEarned // 6 minutes

        timeProvider.elapsedRealtime = initialStartTime + 5.seconds.inWholeMilliseconds // Start work later
        val lastStartTime = timeProvider.elapsedRealtime

        timeProvider.elapsedRealtime += workDuration.inWholeMilliseconds // Advance time
        val persistTime = timeProvider.elapsedRealtime
        testScope.advanceTimeBy(workDuration.inWholeMilliseconds)

        breakBudgetManager.updateAndPersistBreakBudget(
            timerType = TimerType.WORK,
            timerState = TimerState.RUNNING,
            timerProfile = countUpProfile,
            lastStartTime = lastStartTime,
        )

        // Verify persisted data has the new start time
        breakBudgetManager.breakBudgetData.test {
            val emission = awaitItem()
            if (emission.breakBudget == initialBudget && emission.breakBudgetStart == initialStartTime) {
                assertEquals(BreakBudgetData(expectedTotal, persistTime), awaitItem())
            } else {
                assertEquals(BreakBudgetData(expectedTotal, persistTime), emission)
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(persistTime, breakBudgetManager.breakBudgetData.value.breakBudgetStart)
    }

    @Test
    fun `getPersistedBreakBudgetAmount returns current persisted value`() = runTest {
        assertEquals(0.minutes, breakBudgetManager.getPersistedBreakBudgetAmount())

        val budget = 7.minutes
        settingsRepo.setBreakBudgetData(BreakBudgetData(budget, 1234L))
        // No need to recreate manager, flow should update it
        testScope.advanceTimeBy(100) // Allow flow to emit

        assertEquals(budget, breakBudgetManager.getPersistedBreakBudgetAmount())
    }
}
