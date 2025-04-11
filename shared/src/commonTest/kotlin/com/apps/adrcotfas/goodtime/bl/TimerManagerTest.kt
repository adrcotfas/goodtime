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
import com.apps.adrcotfas.goodtime.bl.TimerManager.Companion.COUNT_UP_HARD_LIMIT
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepositoryImpl
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_WORK_DURATION
import com.apps.adrcotfas.goodtime.data.settings.BreakBudgetData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeEventListener
import com.apps.adrcotfas.goodtime.fakes.FakeLabelDao
import com.apps.adrcotfas.goodtime.fakes.FakeSessionDao
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var localDataRepo: LocalDataRepository
    private lateinit var streakManager: StreakManager
    private lateinit var breakBudgetManager: BreakBudgetManager // Added BreakBudgetManager

    private lateinit var timerManager: TimerManager

    private val timeProvider = FakeTimeProvider()
    private val fakeEventListener = FakeEventListener()
    private val logger = Logger(StaticConfig())

    private lateinit var finishedSessionsHandler: FinishedSessionsHandler

    @BeforeTest
    fun setup() = runTest(testDispatcher) {
        timeProvider.elapsedRealtime = 0L
        localDataRepo = LocalDataRepositoryImpl(
            sessionDao = FakeSessionDao(),
            labelDao = FakeLabelDao(),
            coroutineScope = testScope,
        )

        localDataRepo.updateDefaultLabel(defaultLabel)
        localDataRepo.insertLabel(customLabel)
        localDataRepo.insertLabel(countUpLabel)

        settingsRepo = FakeSettingsRepository()

        // Initialize StreakManager
        streakManager = StreakManager(
            settingsRepo = settingsRepo,
            timeProvider = timeProvider,
            log = logger,
            coroutineScope = testScope,
        )

        // Initialize BreakBudgetManager
        breakBudgetManager = BreakBudgetManager(
            settingsRepo = settingsRepo,
            timeProvider = timeProvider,
            log = logger,
            coroutineScope = testScope,
        )

        finishedSessionsHandler = FinishedSessionsHandler(
            coroutineScope = testScope,
            repo = localDataRepo,
            settingsRepo = settingsRepo,
            log = logger,
        )

        timerManager = TimerManager(
            localDataRepo = localDataRepo,
            settingsRepo = settingsRepo,
            listeners = listOf(fakeEventListener),
            timeProvider = timeProvider,
            finishedSessionsHandler = finishedSessionsHandler,
            streakManager = streakManager,
            breakBudgetManager = breakBudgetManager, // Inject BreakBudgetManager
            log = logger,
            coroutineScope = testScope,
        )
        timerManager.setup()
    }

    @Test
    fun `Verify first run for default label and subsequently label changes`() = runTest {
        assertEquals(defaultLabel.name, timerManager.timerData.value.label.getLabelName())
        assertEquals(defaultLabel.timerProfile, timerManager.timerData.value.getTimerProfile())

        settingsRepo.activateLabelWithName(customLabel.name)

        assertEquals(customLabel.name, timerManager.timerData.value.getLabelName())
        assertEquals(customLabel.timerProfile, timerManager.timerData.value.getTimerProfile())

        settingsRepo.activateDefaultLabel()
        assertEquals(timerManager.timerData.value.getLabelName(), defaultLabel.name)
        assertEquals(timerManager.timerData.value.getTimerProfile(), defaultLabel.timerProfile)

        val newTimerProfile = TimerProfile().copy(isCountdown = false, workBreakRatio = 42)
        localDataRepo.updateDefaultLabel(defaultLabel.copy(timerProfile = newTimerProfile))
        assertEquals(
            timerManager.timerData.value.getTimerProfile(),
            newTimerProfile,
            "Modifying the label did not trigger an update",
        )
    }

    // Removed test `Init persistent break budget data only once` as it's implicitly tested by BreakBudgetManager tests

    @Test
    fun `Start then pause and resume a timer`() = runTest {
        timerManager.start(TimerType.WORK)
        val startTime = timeProvider.elapsedRealtime()
        // Expected end time calculation needs break budget for count-up breaks
        val expectedEndTime = timerManager.timerData.value.getEndTime(
            TimerType.WORK,
            startTime,
            breakBudgetManager.getPersistedBreakBudgetAmount(),
        )
        assertEquals(
            timerManager.timerData.value,
            DomainTimerData(
                isReady = true,
                label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                startTime = startTime,
                lastStartTime = startTime,
                endTime = expectedEndTime, // Use calculated end time
                type = TimerType.WORK,
                state = TimerState.RUNNING,
            ),
        )
        val elapsedTime = 1.minutes.inWholeMilliseconds
        timeProvider.elapsedRealtime += elapsedTime
        timerManager.toggle() // Pause
        assertEquals(
            timerManager.timerData.value.timeAtPause,
            expectedEndTime - elapsedTime, // Compare against calculated end time
            "remaining time should be one minute less",
        )
        timeProvider.elapsedRealtime += elapsedTime // Advance time while paused
        val resumeTime = timeProvider.elapsedRealtime
        val expectedEndTimeAfterResume =
            timerManager.timerData.value.timeAtPause + resumeTime // Calculate new end time
        timerManager.toggle() // Resume
        assertEquals(
            timerManager.timerData.value.endTime,
            expectedEndTimeAfterResume,
            "the timer should end after 2 more minutes from start",
        )
    }

    @Test
    fun `Add one minute`() = runTest {
        timerManager.start(TimerType.WORK)
        val startTime = timeProvider.elapsedRealtime()
        val initialEndTime = timerManager.timerData.value.endTime
        assertEquals(
            timerManager.timerData.value,
            DomainTimerData(
                isReady = true,
                label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                startTime = startTime,
                lastStartTime = startTime,
                endTime = initialEndTime,
                type = TimerType.WORK,
                state = TimerState.RUNNING,
            ),
            "the timer should have started",
        )
        timerManager.addOneMinute()
        assertEquals(
            timerManager.timerData.value,
            DomainTimerData(
                isReady = true,
                label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                startTime = startTime,
                lastStartTime = startTime,
                endTime = initialEndTime + 1.minutes.inWholeMilliseconds, // Check against initial end time
                type = TimerType.WORK,
                state = TimerState.RUNNING,
            ),
            "the timer should have been prolonged by one minute",
        )
    }

    @Test
    fun `Add one minute while paused for one minute then finish`() = runTest {
        timerManager.start(TimerType.WORK)
        val startTime = timeProvider.elapsedRealtime()
        val initialEndTime = timerManager.timerData.value.endTime
        assertEquals(
            timerManager.timerData.value,
            DomainTimerData(
                isReady = true,
                label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                startTime = startTime,
                lastStartTime = startTime,
                endTime = initialEndTime,
                type = TimerType.WORK,
                state = TimerState.RUNNING,
            ),
            "the timer should have started",
        )
        val oneMinute = 1.minutes.inWholeMilliseconds
        timeProvider.elapsedRealtime += oneMinute
        timerManager.toggle() // Pause
        assertEquals(
            timerManager.timerData.value.timeAtPause,
            initialEndTime - oneMinute,
            "remaining time should be one minute less",
        )
        timeProvider.elapsedRealtime += oneMinute // Advance time while paused
        timerManager.addOneMinute()
        assertEquals(
            timerManager.timerData.value.timeAtPause,
            initialEndTime - oneMinute + 1.minutes.inWholeMilliseconds, // Check remaining time after adding minute
            "remaining time should be one minute more",
        )
        timerManager.finish()
        assertEquals(
            timerManager.timerData.value.endTime,
            timeProvider.elapsedRealtime,
            "the timer should end now",
        )
        assertEquals(
            fakeEventListener.events,
            listOf(
                Event.Start(endTime = initialEndTime),
                Event.Pause,
                Event.AddOneMinute(endTime = initialEndTime + oneMinute), // Event should reflect new end time
                Event.Finished(type = TimerType.WORK),
            ),
        )
        val session = localDataRepo.selectAllSessions().first().last()
        assertEquals(session.duration.minutes.inWholeMilliseconds, oneMinute) // Only 1 minute of work was done
        assertEquals(session.timestamp, timeProvider.elapsedRealtime) // Timestamp is when finished
    }

    @Test
    fun `Skip session after one minute`() = runTest {
        timerManager.start(TimerType.WORK)
        val startTime = timeProvider.elapsedRealtime()
        val initialEndTime = timerManager.timerData.value.endTime
        assertEquals(
            timerManager.timerData.value,
            DomainTimerData(
                isReady = true,
                label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                startTime = startTime,
                lastStartTime = startTime,
                endTime = initialEndTime,
                type = TimerType.WORK,
                state = TimerState.RUNNING,
            ),
            "the timer should have started",
        )
        val elapsedTime = 1.minutes.inWholeMilliseconds
        timeProvider.elapsedRealtime += elapsedTime
        timerManager.next() // Skip work, start break
        assertEquals(TimerState.RUNNING, timerManager.timerData.value.state)
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type)

        var session = localDataRepo.selectAllSessions().first().last()
        assertEquals(elapsedTime, session.duration.minutes.inWholeMilliseconds)
        assertEquals(elapsedTime, session.timestamp)
        assertEquals(true, session.isWork)

        timeProvider.elapsedRealtime += elapsedTime // Advance time during break
        timerManager.next() // Skip break, start work
        assertEquals(TimerState.RUNNING, timerManager.timerData.value.state)
        assertEquals(TimerType.WORK, timerManager.timerData.value.type)
        session = localDataRepo.selectAllSessions().first().last()
        assertEquals(elapsedTime, session.duration.minutes.inWholeMilliseconds) // Break duration
        assertEquals(elapsedTime + elapsedTime, session.timestamp) // Timestamp after break
        assertEquals(false, session.isWork)
    }

    @Test
    fun `Skip timer before one minute`() = runTest {
        timerManager.start(TimerType.WORK)
        val endTime = timerManager.timerData.value.endTime
        val duration = 45.seconds.inWholeMilliseconds
        timeProvider.elapsedRealtime = duration
        timerManager.next() // Skip work, start break
        val breakEndTime = timeProvider.elapsedRealtime + TimerProfile.DEFAULT_BREAK_DURATION.minutes.inWholeMilliseconds
        assertEquals(
            fakeEventListener.events,
            listOf(
                Event.Start(endTime = endTime),
                Event.Start(endTime = breakEndTime), // Check break end time
            ),
        )
        localDataRepo.selectAllSessions().test {
            assertTrue(awaitItem().isEmpty()) // No session saved
        }
    }

    @Test
    fun `Timer finish`() = runTest {
        timerManager.start(TimerType.WORK)
        val startTime = timeProvider.elapsedRealtime()
        val initialEndTime = timerManager.timerData.value.endTime
        timeProvider.elapsedRealtime = initialEndTime // Advance time to end
        timerManager.finish()
        // Check streak via StreakManager
        assertEquals(1, streakManager.longBreakData.value.streak)
        assertEquals(initialEndTime, streakManager.longBreakData.value.lastWorkEndTime)

        assertEquals(
            timerManager.timerData.value,
            DomainTimerData(
                isReady = true,
                label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                // breakBudgetData removed
                startTime = startTime,
                lastStartTime = startTime,
                endTime = initialEndTime, // End time is when it finished
                type = TimerType.WORK,
                state = TimerState.FINISHED,
                completedMinutes = DEFAULT_WORK_DURATION.toLong(),
            ),
            "the timer should have finished",
        )
        val session = localDataRepo.selectAllSessions().first().last()
        assertEquals(DEFAULT_DURATION, session.duration.minutes.inWholeMilliseconds)
        assertEquals(initialEndTime, session.timestamp)
    }

    @Test
    fun `Timer reset after one minute`() = runTest {
        timerManager.start(TimerType.WORK)
        val endTime = timerManager.timerData.value.endTime
        val oneMinute = 1.minutes.inWholeMilliseconds
        timeProvider.elapsedRealtime = oneMinute
        timerManager.reset()
        // Check streak via StreakManager
        assertEquals(0, streakManager.longBreakData.value.streak)
        assertEquals(0, streakManager.longBreakData.value.lastWorkEndTime)

        assertEquals(
            timerManager.timerData.value,
            DomainTimerData(
                isReady = true,
                label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                // breakBudgetData removed
            ),
            "the timer should have been reset",
        )
        assertEquals(
            fakeEventListener.events,
            listOf(Event.Start(endTime = endTime), Event.Reset),
        )
        val session = localDataRepo.selectAllSessions().first().last()
        assertEquals(oneMinute, session.duration.minutes.inWholeMilliseconds)
        assertEquals(oneMinute, session.timestamp)
    }

    @Test
    fun `Timer reset before one minute`() = runTest {
        timerManager.start(TimerType.WORK)
        val endTime = timerManager.timerData.value.endTime
        val duration = 45.seconds.inWholeMilliseconds
        timeProvider.elapsedRealtime = duration
        timerManager.reset()
        // Check streak via StreakManager
        assertEquals(0, streakManager.longBreakData.value.streak)
        assertEquals(0, streakManager.longBreakData.value.lastWorkEndTime)

        assertEquals(
            timerManager.timerData.value,
            DomainTimerData(
                isReady = true,
                label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                // breakBudgetData removed
            ),
            "the timer should have been reset",
        )
        assertEquals(
            fakeEventListener.events,
            listOf(Event.Start(endTime = endTime), Event.Reset),
        )
        localDataRepo.selectAllSessions().test {
            assertTrue(awaitItem().isEmpty()) // No session saved
        }
    }

    // Note: Streak tests now rely on checking streakManager.longBreakData.value
    // instead of timerManager.timerData.value.longBreakData

    @Test
    fun `Streak increments when interrupting a work session`() = runTest {
        assertEquals(0, streakManager.longBreakData.value.streak)
        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += 1
        timerManager.skip() // Saves session, increments streak
        timerManager.reset() // Does not save session
        assertEquals(1, streakManager.longBreakData.value.streak)

        timeProvider.elapsedRealtime += 1
        timerManager.skip() // Saves session (break), does not increment streak
        timerManager.reset() // Does not save session
        assertEquals(1, streakManager.longBreakData.value.streak)

        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += 1
        timerManager.skip() // Saves session, increments streak
        timerManager.reset() // Does not save session
        assertEquals(2, streakManager.longBreakData.value.streak)

        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += 1
        timerManager.finish() // Saves session, increments streak
        assertEquals(3, streakManager.longBreakData.value.streak)
    }

    @Test
    fun `Pausing for a long time is not considered idle time for streak`() = runTest {
        assertEquals(0, streakManager.longBreakData.value.streak)
        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += 1
        timerManager.toggle() // Pause
        val twoHours = 2.hours.inWholeMilliseconds
        timeProvider.elapsedRealtime += twoHours
        timerManager.toggle() // Resume
        timerManager.finish() // Should increment streak because pause time doesn't count as idle
        assertEquals(1, streakManager.longBreakData.value.streak)
    }

    @Test
    fun `Long break after 4 work sessions with finish and next`() = runTest {
        assertEquals(0, streakManager.longBreakData.value.streak)
        val twoMinutes = 2.minutes.inWholeMilliseconds

        // Session 1
        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.finish() // Streak = 1
        timerManager.next() // Starts Break
        assertEquals(1, streakManager.longBreakData.value.streak)
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type)

        // Session 2
        timerManager.next() // Starts Work
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.finish() // Streak = 2
        timerManager.next() // Starts Break
        assertEquals(2, streakManager.longBreakData.value.streak)
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type)

        // Session 3
        timerManager.next() // Starts Work
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.finish() // Streak = 3
        timerManager.next() // Starts Break
        assertEquals(3, streakManager.longBreakData.value.streak)
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type)

        // Session 4
        timerManager.next() // Starts Work
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.finish() // Streak = 4
        timerManager.next() // Starts Long Break
        assertEquals(4, streakManager.longBreakData.value.streak)
        assertEquals(TimerType.LONG_BREAK, timerManager.timerData.value.type)
    }

    @Test
    fun `Long break after 4 work sessions with reset and next for the last`() = runTest {
        assertEquals(0, streakManager.longBreakData.value.streak)
        val twoMinutes = 2.minutes.inWholeMilliseconds

        // Session 1
        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.finish()
        timerManager.reset() // Streak = 1
        assertEquals(1, streakManager.longBreakData.value.streak)

        // Session 2
        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.finish()
        timerManager.reset() // Streak = 2
        assertEquals(2, streakManager.longBreakData.value.streak)

        // Session 3
        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.finish()
        timerManager.reset() // Streak = 3
        assertEquals(3, streakManager.longBreakData.value.streak)

        // Session 4
        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.finish() // Streak = 4
        timerManager.next() // Starts Long Break
        assertEquals(4, streakManager.longBreakData.value.streak)
        assertEquals(TimerType.LONG_BREAK, timerManager.timerData.value.type)
    }

    @Test
    fun `No long break if idled`() = runTest {
        val sessionsBeforeLongBreak =
            timerManager.timerData.value.label.profile.sessionsBeforeLongBreak
        assertEquals(0, streakManager.longBreakData.value.streakInUse(sessionsBeforeLongBreak))

        val twoMinutes = 2.minutes.inWholeMilliseconds

        // Session 1
        timerManager.start(TimerType.WORK)
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.skip() // Streak = 1, Starts Break
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type)
        assertEquals(1, streakManager.longBreakData.value.streakInUse(sessionsBeforeLongBreak))

        // Session 2
        timerManager.skip() // Starts Work
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.skip() // Streak = 2, Starts Break
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type)
        assertEquals(2, streakManager.longBreakData.value.streakInUse(sessionsBeforeLongBreak))

        // Session 3
        timerManager.skip() // Starts Work
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.skip() // Streak = 3, Starts Break
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type)
        assertEquals(3, streakManager.longBreakData.value.streakInUse(sessionsBeforeLongBreak))

        // Idle for an hour
        timerManager.reset() // Saves break session, doesn't affect streak
        val oneHour = 1.hours.inWholeMilliseconds
        timeProvider.elapsedRealtime += oneHour

        // Session 4 (after idle)
        timerManager.start(TimerType.WORK) // Streak should reset here because of idle time check in handlePersistentDataAtStart
        assertEquals(0, streakManager.longBreakData.value.streakInUse(sessionsBeforeLongBreak))
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.skip() // Streak = 1, Starts Break
        assertEquals(1, streakManager.longBreakData.value.streakInUse(sessionsBeforeLongBreak))
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type) // Should be normal break, not long break
    }

    @Test
    fun `Reset streak after idle`() = runTest {
        assertEquals(0, streakManager.longBreakData.value.streak)
        timerManager.start(TimerType.WORK)
        val twoMinutes = 2.minutes.inWholeMilliseconds
        timeProvider.elapsedRealtime += twoMinutes
        timerManager.skip() // Streak = 1
        assertEquals(1, streakManager.longBreakData.value.streak)

        // Idle
        val oneHour = 1.hours.inWholeMilliseconds
        timeProvider.elapsedRealtime += oneHour

        // Start new work session - streak should reset
        timerManager.start(TimerType.WORK)
        assertEquals(0, streakManager.longBreakData.value.streak)
    }

    @Test
    fun `Auto-start break`() = runTest {
        settingsRepo.setAutoStartBreak(true)
        timerManager.start(TimerType.WORK)
        val workDuration = DEFAULT_DURATION
        timeProvider.elapsedRealtime += workDuration
        timerManager.finish()
        assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
    }

    @Test
    fun `Work is next after work when break is disabled`() = runTest {
        settingsRepo.setAutoStartWork(true)
        localDataRepo.updateDefaultLabel(
            defaultLabel.copy(
                timerProfile = TimerProfile().copy(
                    isBreakEnabled = false,
                ),
            ),
        )
        timerManager.start(TimerType.WORK)
        val workDuration = DEFAULT_WORK_DURATION.minutes.inWholeMilliseconds
        timeProvider.elapsedRealtime += workDuration
        timerManager.next()
        assertEquals(timerManager.timerData.value.type, TimerType.WORK)
    }

    // Tests related to count-up and break budget need adjustment
    @Test
    fun `Count-up work then count-down the break budget`() = runTest {
        settingsRepo.activateLabelWithName(countUpLabel.name)

        timerManager.start(TimerType.WORK)
        val workDuration = 6.minutes.inWholeMilliseconds
        val expectedBreakBudgetMinutes =
            workDuration.milliseconds.inWholeMinutes / countUpLabel.timerProfile.workBreakRatio
        timeProvider.elapsedRealtime += workDuration
        testScope.advanceTimeBy(workDuration) // Ensure coroutines run

        timerManager.next() // Should calculate and persist budget, then start break

        // Check budget via BreakBudgetManager
        assertEquals(
            expectedBreakBudgetMinutes,
            breakBudgetManager.getPersistedBreakBudgetAmount().inWholeMinutes,
        )
        assertEquals(TimerType.BREAK, timerManager.timerData.value.type)
        // Check break end time based on persisted budget
        assertEquals(
            timeProvider.elapsedRealtime + breakBudgetManager.getPersistedBreakBudgetAmount().inWholeMilliseconds,
            timerManager.timerData.value.endTime,
        )

        timeProvider.elapsedRealtime += breakBudgetManager.getPersistedBreakBudgetAmount().inWholeMilliseconds
        timerManager.next() // Should start work, budget should decay to 0

        assertEquals(
            0,
            breakBudgetManager.getCurrentBreakBudget( // Check current calculated budget
                timerManager.timerData.value.type,
                timerManager.timerData.value.state,
                timerManager.timerData.value.label.profile,
                timerManager.timerData.value.lastStartTime,
            ).inWholeMinutes,
        )
        assertEquals(TimerType.WORK, timerManager.timerData.value.type)
    }

    @Test
    fun `Count-up then reset then wait for a while to start another count-up`() =
        runTest {
            settingsRepo.activateLabelWithName(countUpLabel.name)

            timerManager.start() // Start count-up work
            val workTime = 12.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += workTime
            testScope.advanceTimeBy(workTime)

            var expectedBreakBudgetMinutes =
                workTime.milliseconds.inWholeMinutes / countUpLabel.timerProfile.workBreakRatio
            timerManager.reset() // Resets timer, calculates and persists budget

            // Check persisted budget via manager
            assertEquals(
                expectedBreakBudgetMinutes,
                breakBudgetManager.getPersistedBreakBudgetAmount().inWholeMinutes,
            )

            val idleTime = 3.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += idleTime
            testScope.advanceTimeBy(idleTime) // Let time pass

            // Calculate expected decayed budget
            expectedBreakBudgetMinutes -= idleTime.milliseconds.inWholeMinutes.toInt()
            assertEquals(
                expectedBreakBudgetMinutes,
                breakBudgetManager.getCurrentBreakBudget( // Check current calculated budget (should decay)
                    timerManager.timerData.value.type, // WORK (but reset)
                    timerManager.timerData.value.state, // RESET
                    timerManager.timerData.value.label.profile,
                    timerManager.timerData.value.lastStartTime, // Not relevant for reset state
                ).inWholeMinutes,
                "The break budget should have decreased while idling",
            )

            timerManager.start(TimerType.WORK) // Start another work session
            timeProvider.elapsedRealtime += workTime
            testScope.advanceTimeBy(workTime)

            timerManager.reset() // Reset again, calculates and persists new total budget
            val extraBreakBudgetMinutes =
                workTime.milliseconds.inWholeMinutes / countUpLabel.timerProfile.workBreakRatio

            val breakBudgetAtTheEndMinutes = expectedBreakBudgetMinutes + extraBreakBudgetMinutes
            assertEquals(
                breakBudgetAtTheEndMinutes,
                breakBudgetManager.getPersistedBreakBudgetAmount().inWholeMinutes, // Check final persisted budget
                "The previous unused break budget should have been added to the total",
            )

            // Start work, let some idle time pass, then finish (finish doesn't apply to count-up work)
            timerManager.start(TimerType.WORK)
            timeProvider.elapsedRealtime += idleTime
            testScope.advanceTimeBy(idleTime)
            // 'finish()' is for countdown. For count-up work, we'd typically use 'next()' or 'reset()'.
            // Let's simulate stopping work via reset to check budget decay.
            timerManager.reset()

            assertEquals(
                breakBudgetAtTheEndMinutes - idleTime.milliseconds.inWholeMinutes.toInt(),
                breakBudgetManager.getCurrentBreakBudget( // Check current calculated budget after reset
                    timerManager.timerData.value.type, // WORK (but reset)
                    timerManager.timerData.value.state, // RESET
                    timerManager.timerData.value.label.profile,
                    timerManager.timerData.value.lastStartTime, // Not relevant
                ).inWholeMinutes,
                "The break budget should have decreased while idling before reset",
            )
        }

    @Test
    fun `Count-up then start a break with budget already there`() = runTest {
        val initialBudget = 10.minutes
        settingsRepo.setBreakBudgetData(BreakBudgetData(initialBudget, 0))
        settingsRepo.activateLabelWithName(countUpLabel.name)
        timerManager.restart() // Restart to pick up settings

        timerManager.start() // Start count-up work (doesn't affect budget yet)
        timerManager.next() // Start break using the pre-existing budget

        assertEquals(
            timeProvider.elapsedRealtime + initialBudget.inWholeMilliseconds,
            timerManager.timerData.value.endTime, // End time should be based on initial budget
        )
        assertEquals(
            listOf(
                Event.Start(endTime = COUNT_UP_HARD_LIMIT), // Count-up work start
                Event.Start(endTime = timeProvider.elapsedRealtime + initialBudget.inWholeMilliseconds), // Break start
            ),
            fakeEventListener.events,
        )
    }

    @Test
    fun `Count-up then start break then auto-start work`() = runTest {
        val breakBudget = 3.minutes
        val breakBudgetMillis = breakBudget.inWholeMilliseconds

        settingsRepo.setBreakBudgetData(BreakBudgetData(breakBudget, 0))
        settingsRepo.activateLabelWithName(countUpLabel.name)
        settingsRepo.setAutoStartWork(true)
        timerManager.restart()

        timerManager.start() // Start count-up work
        val workStartTime = timeProvider.elapsedRealtime
        timerManager.next() // Start break
        val breakStartTime = timeProvider.elapsedRealtime
        timeProvider.elapsedRealtime += breakBudgetMillis
        testScope.advanceTimeBy(breakBudgetMillis) // Let break finish
        timerManager.finish() // Finish break

        assertEquals(
            expected = listOf(
                Event.Start(endTime = workStartTime + COUNT_UP_HARD_LIMIT), // Work start
                Event.Start(endTime = breakStartTime + breakBudgetMillis), // Break start
                Event.Finished(type = TimerType.BREAK, autostartNextSession = true), // Break finish
                Event.Start(endTime = timeProvider.elapsedRealtime + COUNT_UP_HARD_LIMIT, autoStarted = true), // Auto-start work
            ),
            actual = fakeEventListener.events,
        )
        assertEquals(TimerType.WORK, timerManager.timerData.value.type) // Should be back to work
    }

    @Test
    fun `Count-up then start break then observe remaining budget`() = runTest {
        val breakBudget = 3.minutes
        val oneMinute = 1.minutes.inWholeMilliseconds

        settingsRepo.setBreakBudgetData(BreakBudgetData(breakBudget, 0))
        settingsRepo.activateLabelWithName(countUpLabel.name)
        settingsRepo.setAutoStartWork(true) // Auto-start doesn't matter here
        timerManager.restart()

        timerManager.start() // Start work
        timerManager.next() // Start break

        timeProvider.elapsedRealtime += oneMinute
        testScope.advanceTimeBy(oneMinute)
        assertEquals(
            breakBudget - 1.minutes,
            breakBudgetManager.getCurrentBreakBudget( // Check remaining budget via manager
                timerManager.timerData.value.type,
                timerManager.timerData.value.state,
                timerManager.timerData.value.label.profile,
                timerManager.timerData.value.lastStartTime,
            ),
        )
        timeProvider.elapsedRealtime += oneMinute
        testScope.advanceTimeBy(oneMinute)
        assertEquals(
            breakBudget - 2.minutes,
            breakBudgetManager.getCurrentBreakBudget(
                timerManager.timerData.value.type,
                timerManager.timerData.value.state,
                timerManager.timerData.value.label.profile,
                timerManager.timerData.value.lastStartTime,
            ),
        )
        timeProvider.elapsedRealtime += oneMinute
        testScope.advanceTimeBy(oneMinute)
        assertEquals(
            breakBudget - 3.minutes,
            breakBudgetManager.getCurrentBreakBudget(
                timerManager.timerData.value.type,
                timerManager.timerData.value.state,
                timerManager.timerData.value.label.profile,
                timerManager.timerData.value.lastStartTime,
            ),
        )
    }

    @Test
    fun `Count-up then work for a while then change the work break ratio`() = runTest {
        settingsRepo.activateLabelWithName(countUpLabel.name)

        timerManager.start() // Start work
        val workTime = 12.minutes.inWholeMilliseconds
        timeProvider.elapsedRealtime += workTime
        testScope.advanceTimeBy(workTime)

        val expectedBudgetMinutes =
            workTime.milliseconds.inWholeMinutes / countUpLabel.timerProfile.workBreakRatio
        // Check current budget calculation before reset
        assertEquals(
            expectedBudgetMinutes,
            breakBudgetManager.getCurrentBreakBudget(
                timerManager.timerData.value.type,
                timerManager.timerData.value.state,
                timerManager.timerData.value.label.profile,
                timerManager.timerData.value.lastStartTime,
            ).inWholeMinutes,
        )
        timerManager.reset() // Persists the calculated budget

        // Verify persisted budget
        assertEquals(
            expectedBudgetMinutes,
            breakBudgetManager.getPersistedBreakBudgetAmount().inWholeMinutes,
        )

        // Change the ratio in the label profile
        val newWorkBreakRatio = 1
        val updatedLabel = countUpLabel.copy(
            timerProfile = countUpLabel.timerProfile.copy(workBreakRatio = newWorkBreakRatio),
        )
        localDataRepo.updateLabel(countUpLabel.name, updatedLabel)
        // Need to wait for the label change to propagate through the flow to TimerManager
        testScope.advanceTimeBy(100) // Give flow time to update

        timerManager.start() // Start work again with the new ratio
        timeProvider.elapsedRealtime += workTime
        testScope.advanceTimeBy(workTime)

        val extraBudgetMinutes = workTime.milliseconds.inWholeMinutes / newWorkBreakRatio
        timerManager.reset() // Persists budget calculated with new ratio + old budget

        assertEquals(
            expectedBudgetMinutes + extraBudgetMinutes,
            breakBudgetManager.getPersistedBreakBudgetAmount().inWholeMinutes, // Check final persisted budget
            "The break budget should have been recalculated with the new ratio",
        )
    }

    companion object {
        private const val CUSTOM_LABEL_NAME = "dummy"
        private val dummyTimerProfile =
            TimerProfile().copy(isLongBreakEnabled = true, isCountdown = false, workBreakRatio = 5)

        private val DEFAULT_DURATION = DEFAULT_WORK_DURATION.minutes.inWholeMilliseconds

        private var defaultLabel =
            Label.defaultLabel().copy(timerProfile = TimerProfile(isLongBreakEnabled = true))
        private var customLabel =
            Label.defaultLabel().copy(
                name = CUSTOM_LABEL_NAME,
                timerProfile = dummyTimerProfile,
                useDefaultTimeProfile = false,
            )

        private val countUpLabel = Label(
            name = "flow",
            useDefaultTimeProfile = false,
            timerProfile = TimerProfile(isCountdown = false, workBreakRatio = 3),
        )
    }
}
