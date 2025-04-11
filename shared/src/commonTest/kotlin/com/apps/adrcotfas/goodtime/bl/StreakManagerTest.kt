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
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class StreakManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var streakManager: StreakManager
    private val timeProvider = FakeTimeProvider()
    private val logger = Logger(StaticConfig())

    private val defaultProfile = TimerProfile(
        workDuration = 25,
        breakDuration = 5,
        longBreakDuration = 15,
        sessionsBeforeLongBreak = 4,
        isLongBreakEnabled = true,
        isCountdown = true,
    )

    private val countUpProfile = TimerProfile(
        isCountdown = false,
        isLongBreakEnabled = false, // Long break typically not used with count-up
    )

    @BeforeTest
    fun setup() {
        timeProvider.elapsedRealtime = 0L
        settingsRepo = FakeSettingsRepository()
        streakManager = StreakManager(
            settingsRepo = settingsRepo,
            timeProvider = timeProvider,
            log = logger,
            coroutineScope = testScope,
        )
    }

    @Test
    fun `Initial streak is zero`() = runTest {
        streakManager.longBreakData.test {
            assertEquals(LongBreakData(0, 0), awaitItem())
        }
    }

    @Test
    fun `Increment streak updates data and saves to repo`() = runTest {
        val initialTime = 1000L
        timeProvider.elapsedRealtime = initialTime

        streakManager.incrementStreak()

        streakManager.longBreakData.test {
            assertEquals(LongBreakData(1, initialTime), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        // Verify it was saved
        settingsRepo.settings.test {
            assertEquals(LongBreakData(1, initialTime), awaitItem().longBreakData)
            cancelAndIgnoreRemainingEvents()
        }

        val secondTime = initialTime + 5000L
        timeProvider.elapsedRealtime = secondTime
        streakManager.incrementStreak()

        streakManager.longBreakData.test {
            assertEquals(LongBreakData(2, secondTime), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        settingsRepo.settings.test {
            assertEquals(LongBreakData(2, secondTime), awaitItem().longBreakData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Reset streak if needed - resets when idle time exceeds limit`() = runTest {
        val lastWorkEndTime = 1000L
        val initialData = LongBreakData(streak = 2, lastWorkEndTime = lastWorkEndTime)
        settingsRepo.setLongBreakData(initialData)
        streakManager = StreakManager(settingsRepo, timeProvider, logger, testScope) // Recreate to pick up initial data

        streakManager.longBreakData.test {
            assertEquals(initialData, awaitItem()) // Ensure initial data is loaded

            // Calculate max idle time: work(25) + break(5) + buffer(30) = 60 minutes
            val maxIdleMillis = (25 + 5 + 30).minutes.inWholeMilliseconds
            val currentTime = lastWorkEndTime + maxIdleMillis + 1000L // Exceed max idle time
            timeProvider.elapsedRealtime = currentTime

            streakManager.resetStreakIfNeeded(defaultProfile)

            // Streak should reset
            assertEquals(LongBreakData(0, 0), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Verify saved data is reset
        settingsRepo.settings.test {
            assertEquals(LongBreakData(0, 0), awaitItem().longBreakData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Reset streak if needed - does not reset when within idle time limit`() = runTest {
        val lastWorkEndTime = 1000L
        val initialData = LongBreakData(streak = 2, lastWorkEndTime = lastWorkEndTime)
        settingsRepo.setLongBreakData(initialData)
        streakManager = StreakManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        streakManager.longBreakData.test {
            assertEquals(initialData, awaitItem())

            // Calculate max idle time: work(25) + break(5) + buffer(30) = 60 minutes
            val maxIdleMillis = (25 + 5 + 30).minutes.inWholeMilliseconds
            val currentTime = lastWorkEndTime + maxIdleMillis - 1000L // Within max idle time
            timeProvider.elapsedRealtime = currentTime

            streakManager.resetStreakIfNeeded(defaultProfile)

            // Streak should NOT reset, expect initial data again (or no new emission)
            // No new emission expected here as the state doesn't change.
            expectNoEvents()
        }

        // Verify saved data is NOT reset
        settingsRepo.settings.test {
            assertEquals(initialData, awaitItem().longBreakData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Reset streak if needed - does nothing for count-up profile`() = runTest {
        val lastWorkEndTime = 1000L
        val initialData = LongBreakData(streak = 2, lastWorkEndTime = lastWorkEndTime)
        settingsRepo.setLongBreakData(initialData)
        streakManager = StreakManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        streakManager.longBreakData.test {
            assertEquals(initialData, awaitItem())

            val currentTime = lastWorkEndTime + 2.hours.inWholeMilliseconds // Well over idle limit
            timeProvider.elapsedRealtime = currentTime

            streakManager.resetStreakIfNeeded(countUpProfile) // Use count-up profile

            // Streak should NOT reset for count-up
            expectNoEvents()
        }

        // Verify saved data is NOT reset
        settingsRepo.settings.test {
            assertEquals(initialData, awaitItem().longBreakData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should consider streak - returns true when streak count matches and within time`() = runTest {
        val sessionsBeforeLongBreak = defaultProfile.sessionsBeforeLongBreak // 4
        val lastWorkEndTime = 1000L
        // Streak is 4, meaning next break should be a long one
        val initialData = LongBreakData(streak = sessionsBeforeLongBreak, lastWorkEndTime = lastWorkEndTime)
        settingsRepo.setLongBreakData(initialData)
        streakManager = StreakManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val currentTime = lastWorkEndTime + 10.minutes.inWholeMilliseconds // Within idle limit

        assertTrue(streakManager.shouldConsiderStreak(defaultProfile, currentTime))
    }

    @Test
    fun `Should consider streak - returns false when streak count does not match`() = runTest {
        val sessionsBeforeLongBreak = defaultProfile.sessionsBeforeLongBreak // 4
        val lastWorkEndTime = 1000L
        // Streak is 3, not time for long break yet
        val initialData = LongBreakData(streak = sessionsBeforeLongBreak - 1, lastWorkEndTime = lastWorkEndTime)
        settingsRepo.setLongBreakData(initialData)
        streakManager = StreakManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val currentTime = lastWorkEndTime + 10.minutes.inWholeMilliseconds // Within idle limit

        assertFalse(streakManager.shouldConsiderStreak(defaultProfile, currentTime))
    }

    @Test
    fun `Should consider streak - returns false when outside idle time limit`() = runTest {
        val sessionsBeforeLongBreak = defaultProfile.sessionsBeforeLongBreak // 4
        val lastWorkEndTime = 1000L
        // Streak is 4
        val initialData = LongBreakData(streak = sessionsBeforeLongBreak, lastWorkEndTime = lastWorkEndTime)
        settingsRepo.setLongBreakData(initialData)
        streakManager = StreakManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        // Calculate max idle time: work(25) + break(5) + buffer(30) = 60 minutes
        val maxIdleMillis = (25 + 5 + 30).minutes.inWholeMilliseconds
        val currentTime = lastWorkEndTime + maxIdleMillis + 1000L // Exceed max idle time

        assertFalse(streakManager.shouldConsiderStreak(defaultProfile, currentTime))
    }

    @Test
    fun `Should consider streak - returns false when long breaks disabled`() = runTest {
        val profileWithLongBreakDisabled = defaultProfile.copy(isLongBreakEnabled = false)
        val sessionsBeforeLongBreak = profileWithLongBreakDisabled.sessionsBeforeLongBreak // 4
        val lastWorkEndTime = 1000L
        // Streak is 4
        val initialData = LongBreakData(streak = sessionsBeforeLongBreak, lastWorkEndTime = lastWorkEndTime)
        settingsRepo.setLongBreakData(initialData)
        streakManager = StreakManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val currentTime = lastWorkEndTime + 10.minutes.inWholeMilliseconds // Within idle limit

        assertFalse(streakManager.shouldConsiderStreak(profileWithLongBreakDisabled, currentTime))
    }

    @Test
    fun `Should consider streak - returns false for count-up profile`() = runTest {
        val lastWorkEndTime = 1000L
        // Streak is 4 (though irrelevant for count-up)
        val initialData = LongBreakData(streak = 4, lastWorkEndTime = lastWorkEndTime)
        settingsRepo.setLongBreakData(initialData)
        streakManager = StreakManager(settingsRepo, timeProvider, logger, testScope) // Recreate

        val currentTime = lastWorkEndTime + 10.minutes.inWholeMilliseconds // Within idle limit

        assertFalse(streakManager.shouldConsiderStreak(countUpProfile, currentTime))
    }
}
