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
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.settings.BreakBudgetData
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
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class BreakBudgetManagerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private val timeProvider = FakeTimeProvider()
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var breakBudgetManager: BreakBudgetManager

    private val countUpProfile = TimerProfile(isCountdown = false, workBreakRatio = 3)
    private val countUpLabel = DomainLabel(Label.defaultLabel(), countUpProfile)

    @BeforeTest
    fun setup() {
        settingsRepo = FakeSettingsRepository()
        breakBudgetManager =
            BreakBudgetManager(
                settingsRepo = settingsRepo,
                timeProvider = timeProvider,
                coroutineScope = testScope,
                log = Logger(StaticConfig()),
            )
    }

    @Test
    fun `initial budget is read as persisted when the device was not rebooted`() = runTest(testDispatcher) {
        val persisted = BreakBudgetData(breakBudget = 10.minutes, breakBudgetStart = 5_000)
        settingsRepo.setBreakBudgetData(persisted)
        timeProvider.elapsedRealtime = 10_000

        assertEquals(persisted, breakBudgetManager.initialBreakBudget())
    }

    @Test
    fun `initial budget start is reset and persisted after a reboot`() = runTest(testDispatcher) {
        settingsRepo.setBreakBudgetData(BreakBudgetData(breakBudget = 10.minutes, breakBudgetStart = 50_000))
        timeProvider.elapsedRealtime = 10_000 // below the persisted start -> reboot

        val result = breakBudgetManager.initialBreakBudget()

        assertEquals(BreakBudgetData(breakBudget = 10.minutes, breakBudgetStart = 10_000), result)
        assertEquals(result, settingsRepo.settings.first().breakBudgetData)
    }

    @Test
    fun `no budget update for countdown profiles`() {
        val data = DomainTimerData(isReady = true, label = DomainLabel())

        assertNull(breakBudgetManager.updatedBreakBudget(data))
    }

    @Test
    fun `running focus accumulates budget at the work-break ratio`() = runTest(testDispatcher) {
        timeProvider.elapsedRealtime = 0
        val data =
            DomainTimerData(
                isReady = true,
                label = countUpLabel,
                runtime =
                TimerRuntimeState(
                    startTime = 0,
                    lastStartTime = 0,
                    state = TimerState.RUNNING,
                    type = TimerType.FOCUS,
                ),
            )
        timeProvider.elapsedRealtime = 9.minutes.inWholeMilliseconds

        val result = breakBudgetManager.updatedBreakBudget(data)

        assertNotNull(result)
        // 9 minutes of focus at ratio 3 -> 3 minutes of budget
        assertEquals(3.minutes, result.breakBudget)
        assertEquals(9.minutes.inWholeMilliseconds, result.breakBudgetStart)
        assertEquals(result, settingsRepo.settings.first().breakBudgetData)
    }
}
