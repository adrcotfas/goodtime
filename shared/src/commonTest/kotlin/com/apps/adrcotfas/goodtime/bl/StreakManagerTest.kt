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
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class StreakManagerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private val timeProvider = FakeTimeProvider()
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var streakManager: StreakManager

    // countdown profile with long breaks every 4 sessions; work 25 + break 5 + 30 min grace
    private val profile = TimerProfile(isLongBreakEnabled = true, sessionsBeforeLongBreak = 4)
    private val maxIdleTime = (25 + 5 + 30).minutes.inWholeMilliseconds

    @BeforeTest
    fun setup() {
        settingsRepo = FakeSettingsRepository()
        streakManager =
            StreakManager(
                settingsRepo = settingsRepo,
                timeProvider = timeProvider,
                coroutineScope = testScope,
                log = Logger(StaticConfig()),
            )
    }

    @Test
    fun `incrementStreak bumps the count and persists it`() = runTest(testDispatcher) {
        timeProvider.elapsedRealtime = 42_000

        val result = streakManager.incrementStreak(LongBreakData(streak = 2, lastWorkEndTime = 0))

        assertEquals(LongBreakData(streak = 3, lastWorkEndTime = 42_000), result)
        assertEquals(result, settingsRepo.settings.first().longBreakData)
    }

    @Test
    fun `streak is kept when the last work session finished recently`() {
        val data = LongBreakData(streak = 2, lastWorkEndTime = 10_000)

        assertNull(streakManager.resetStreakIfNeeded(data, profile, millis = 10_000 + maxIdleTime - 1))
    }

    @Test
    fun `streak is reset after too much idle time`() = runTest(testDispatcher) {
        val data = LongBreakData(streak = 2, lastWorkEndTime = 10_000)

        val result = streakManager.resetStreakIfNeeded(data, profile, millis = 10_000 + maxIdleTime)

        assertEquals(LongBreakData(), result)
        assertEquals(LongBreakData(), settingsRepo.settings.first().longBreakData)
    }

    @Test
    fun `long break is due when the streak target is reached recently`() {
        // streak 4 with sessionsBeforeLongBreak 4 -> streakInUse == 0
        val data = LongBreakData(streak = 4, lastWorkEndTime = 10_000)

        assertTrue(streakManager.shouldConsiderStreak(data, profile, workEndTime = 20_000))
    }

    @Test
    fun `long break is not due mid-streak or for count-up profiles`() {
        val data = LongBreakData(streak = 3, lastWorkEndTime = 10_000)
        assertFalse(streakManager.shouldConsiderStreak(data, profile, workEndTime = 20_000))

        val countUp = profile.copy(isCountdown = false)
        assertFalse(
            streakManager.shouldConsiderStreak(
                LongBreakData(streak = 4, lastWorkEndTime = 10_000),
                countUp,
                workEndTime = 20_000,
            ),
        )
    }
}
