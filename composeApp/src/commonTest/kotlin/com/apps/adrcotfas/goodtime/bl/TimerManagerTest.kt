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
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.streakInUse
import com.apps.adrcotfas.goodtime.fakes.FakeEventListener
import com.apps.adrcotfas.goodtime.fakes.FakeLabelDao
import com.apps.adrcotfas.goodtime.fakes.FakeSessionDao
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import com.apps.adrcotfas.goodtime.fakes.FakeTimerProfileDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    private lateinit var timerManager: TimerManager

    private val timeProvider = FakeTimeProvider()
    private val fakeEventListener = FakeEventListener()
    private val logger = Logger(StaticConfig())

    private lateinit var finishedSessionsHandler: FinishedSessionsHandler

    @BeforeTest
    fun setup() =
        runTest(testDispatcher) {
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

            localDataRepo.updateDefaultLabel(defaultLabel)
            localDataRepo.insertLabel(customLabel)
            localDataRepo.insertLabel(countUpLabel)

            finishedSessionsHandler =
                FinishedSessionsHandler(
                    coroutineScope = testScope,
                    repo = localDataRepo,
                    settingsRepo = settingsRepo,
                    log = logger,
                )

            timerManager =
                TimerManager(
                    localDataRepo = localDataRepo,
                    settingsRepo = settingsRepo,
                    listeners = listOf(fakeEventListener),
                    timeProvider,
                    finishedSessionsHandler,
                    logger,
                    coroutineScope = testScope,
                )
            timerManager.setup()
        }

    @Test
    fun `Verify first run for default label and subsequently label changes`() =
        runTest {
            assertEquals(
                defaultLabel.name,
                timerManager.timerData.value.label
                    .getLabelName(),
            )
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

    @Test
    fun `Init persistent data only once`() =
        runTest {
            val customLongBreakData = LongBreakData(10, 42)
            settingsRepo.setLongBreakData(customLongBreakData)
            timerManager.restart()
            assertEquals(timerManager.timerData.value.longBreakData, customLongBreakData)

            timeProvider.elapsedRealtime = 10.minutes.inWholeMilliseconds
            val customBreakBudgetData = BreakBudgetData(10.minutes, timeProvider.elapsedRealtime)
            settingsRepo.setBreakBudgetData(customBreakBudgetData)

            timeProvider.elapsedRealtime = 0.minutes.inWholeMilliseconds
            timerManager.restart()
            timerManager.timerData.map { it.breakBudgetData }.test {
                assertEquals(awaitItem(), BreakBudgetData(10.minutes, 0.minutes.inWholeMilliseconds))
            }
        }

    @Test
    fun `Start then pause and resume a timer`() =
        runTest {
            timerManager.start(TimerType.FOCUS)
            val startTime = timeProvider.elapsedRealtime()
            assertEquals(
                timerManager.timerData.value,
                DomainTimerData(
                    isReady = true,
                    label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                    startTime = startTime,
                    lastStartTime = startTime,
                    endTime = startTime + DEFAULT_DURATION,
                    type = TimerType.FOCUS,
                    state = TimerState.RUNNING,
                ),
            )
            val elapsedTime = 1.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += elapsedTime
            timerManager.toggle()
            assertEquals(
                timerManager.timerData.value.timeAtPause,
                DEFAULT_DURATION - elapsedTime,
                "remaining time should be one minute less",
            )
            timeProvider.elapsedRealtime += elapsedTime
            val endTime =
                timerManager.timerData.value.timeAtPause + timeProvider.elapsedRealtime
            timerManager.toggle()
            assertEquals(
                timerManager.timerData.value.endTime,
                endTime,
                "the timer should end after 2 more minutes",
            )
        }

    @Test
    fun `Add one minute`() =
        runTest {
            timerManager.start(TimerType.FOCUS)
            val startTime = timeProvider.elapsedRealtime()
            assertEquals(
                timerManager.timerData.value,
                DomainTimerData(
                    isReady = true,
                    label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                    startTime = startTime,
                    lastStartTime = startTime,
                    endTime = startTime + DEFAULT_DURATION,
                    type = TimerType.FOCUS,
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
                    endTime = startTime + DEFAULT_DURATION + 1.minutes.inWholeMilliseconds,
                    type = TimerType.FOCUS,
                    state = TimerState.RUNNING,
                ),
                "the timer should have been prolonged by one minute",
            )
        }

    @Test
    fun `Add one minute while paused for one minute then finish`() =
        runTest {
            timerManager.start(TimerType.FOCUS)
            val startTime = timeProvider.elapsedRealtime()
            assertEquals(
                timerManager.timerData.value,
                DomainTimerData(
                    isReady = true,
                    label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                    startTime = startTime,
                    lastStartTime = startTime,
                    endTime = startTime + DEFAULT_DURATION,
                    type = TimerType.FOCUS,
                    state = TimerState.RUNNING,
                ),
                "the timer should have started",
            )
            val oneMinute = 1.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += oneMinute
            timerManager.toggle()
            assertEquals(
                timerManager.timerData.value.timeAtPause,
                DEFAULT_DURATION - oneMinute,
                "remaining time should be one minute less",
            )
            timeProvider.elapsedRealtime += oneMinute
            timerManager.addOneMinute()
            assertEquals(
                timerManager.timerData.value.timeAtPause,
                DEFAULT_DURATION - oneMinute + 1.minutes.inWholeMilliseconds,
                "remaining time should be one minute more",
            )
            timerManager.finish()
            assertEquals(
                timerManager.timerData.value.endTime,
                timeProvider.elapsedRealtime,
                "the timer should end after 1 more minute",
            )
            assertEquals(
                fakeEventListener.events,
                listOf(
                    Event.Start(isFocus = true, endTime = startTime + DEFAULT_DURATION, labelName = Label.DEFAULT_LABEL_NAME),
                    Event.Pause,
                    Event.AddOneMinute(endTime = startTime + DEFAULT_DURATION + oneMinute),
                    Event.Finished(type = TimerType.FOCUS),
                ),
            )
            val session = localDataRepo.selectAllSessions().first().last()
            assertEquals(session.duration.minutes.inWholeMilliseconds, oneMinute)
            assertEquals(session.timestamp, oneMinute + oneMinute)
        }

    @Test
    fun `Skip session after one minute`() =
        runTest {
            timerManager.start(TimerType.FOCUS)
            val startTime = timeProvider.elapsedRealtime()
            assertEquals(
                timerManager.timerData.value,
                DomainTimerData(
                    isReady = true,
                    label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                    startTime = startTime,
                    lastStartTime = startTime,
                    endTime = startTime + DEFAULT_DURATION,
                    type = TimerType.FOCUS,
                    state = TimerState.RUNNING,
                ),
                "the timer should have started",
            )
            val elapsedTime = 1.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += elapsedTime
            timerManager.next()
            assertEquals(timerManager.timerData.value.state, TimerState.RUNNING)
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)

            var session = localDataRepo.selectAllSessions().first().last()
            assertEquals(session.duration.minutes.inWholeMilliseconds, elapsedTime)
            assertEquals(session.timestamp, elapsedTime)
            assertEquals(session.isWork, true)

            timeProvider.elapsedRealtime += elapsedTime
            timerManager.next()
            assertEquals(timerManager.timerData.value.state, TimerState.RUNNING)
            assertEquals(timerManager.timerData.value.type, TimerType.FOCUS)
            session = localDataRepo.selectAllSessions().first().last()
            assertEquals(session.duration.minutes.inWholeMilliseconds, elapsedTime)
            assertEquals(session.timestamp, elapsedTime + elapsedTime)
            assertEquals(session.isWork, false)
        }

    @Test
    fun `Skip timer before one minute`() =
        runTest {
            timerManager.start(TimerType.FOCUS)
            val endTime = timerManager.timerData.value.endTime
            val duration = 45.seconds.inWholeMilliseconds
            timeProvider.elapsedRealtime = duration
            timerManager.next()
            assertEquals(
                fakeEventListener.events,
                listOf(
                    Event.Start(isFocus = true, endTime = endTime),
                    Event.Start(
                        isFocus = false,
                        endTime =
                            timeProvider.elapsedRealtime + TimerProfile.DEFAULT_BREAK_DURATION.minutes.inWholeMilliseconds,
                    ),
                ),
            )
            localDataRepo.selectAllSessions().test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `Timer finish`() =
        runTest {
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime = DEFAULT_DURATION
            timerManager.finish()
            assertEquals(
                timerManager.timerData.value,
                DomainTimerData(
                    isReady = true,
                    label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                    longBreakData =
                        LongBreakData(
                            streak = 1,
                            lastWorkEndTime = DEFAULT_DURATION,
                        ),
                    startTime = 0,
                    lastStartTime = 0,
                    endTime = DEFAULT_DURATION,
                    type = TimerType.FOCUS,
                    state = TimerState.FINISHED,
                    completedMinutes = DEFAULT_WORK_DURATION.toLong(),
                ),
                "the timer should have finished",
            )
            val session = localDataRepo.selectAllSessions().first().last()
            assertEquals(session.duration.minutes.inWholeMilliseconds, DEFAULT_DURATION)
            assertEquals(session.timestamp, DEFAULT_DURATION)
        }

    @Test
    fun `Timer reset after one minute`() =
        runTest {
            timerManager.start(TimerType.FOCUS)
            val endTime = timerManager.timerData.value.endTime
            val oneMinute = 1.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime = oneMinute
            timerManager.reset()
            assertEquals(
                timerManager.timerData.value,
                DomainTimerData(
                    isReady = true,
                    label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                    longBreakData =
                        LongBreakData(
                            streak = 0,
                            lastWorkEndTime = 0,
                        ),
                ),
                "the timer should have been reset",
            )
            assertEquals(
                fakeEventListener.events,
                listOf(Event.Start(isFocus = true, endTime = endTime), Event.Reset),
            )
            val session = localDataRepo.selectAllSessions().first().last()
            assertEquals(session.duration.minutes.inWholeMilliseconds, oneMinute)
            assertEquals(session.timestamp, oneMinute)
        }

    @Test
    fun `Timer reset before one minute`() =
        runTest {
            timerManager.start(TimerType.FOCUS)
            val endTime = timerManager.timerData.value.endTime
            val duration = 45.seconds.inWholeMilliseconds
            timeProvider.elapsedRealtime = duration
            timerManager.reset()
            assertEquals(
                timerManager.timerData.value,
                DomainTimerData(
                    isReady = true,
                    label = DomainLabel(defaultLabel, defaultLabel.timerProfile),
                    longBreakData =
                        LongBreakData(
                            streak = 0,
                            lastWorkEndTime = 0,
                        ),
                ),
                "the timer should have been reset",
            )
            assertEquals(
                fakeEventListener.events,
                listOf(Event.Start(isFocus = true, endTime = endTime), Event.Reset),
            )
            localDataRepo.selectAllSessions().test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `Streak increments when interrupting a work session`() =
        runTest {
            assertEquals(timerManager.timerData.value.longBreakData.streak, 0)
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += 1
            timerManager.skip()
            timerManager.reset()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 1)
            timeProvider.elapsedRealtime += 1
            timerManager.skip()
            timerManager.reset()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 1)
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += 1
            timerManager.skip()
            timerManager.reset()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 2)
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += 1
            timerManager.finish()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 3)
        }

    @Test
    fun `Pausing for a long time is not considered idle time for streak`() =
        runTest {
            assertEquals(timerManager.timerData.value.longBreakData.streak, 0)
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += 1
            timerManager.toggle()
            val twoHours = 2.hours.inWholeMilliseconds
            timeProvider.elapsedRealtime += twoHours
            timerManager.toggle()
            timerManager.finish()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 1)
        }

    @Test
    fun `Long break after 4 work sessions with finish and next`() =
        runTest {
            assertEquals(timerManager.timerData.value.longBreakData.streak, 0)
            timerManager.start(TimerType.FOCUS)
            val twoMinutes = 2.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.finish()
            timerManager.next()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 1)
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
            timerManager.next()
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.finish()
            timerManager.next()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 2)
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
            timerManager.next()
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.finish()
            timerManager.next()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 3)
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
            timerManager.next()
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.finish()
            timerManager.next()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 4)
            assertEquals(timerManager.timerData.value.type, TimerType.LONG_BREAK)
        }

    @Test
    fun `Long break after 4 work sessions with reset and next for the last`() =
        runTest {
            assertEquals(timerManager.timerData.value.longBreakData.streak, 0)
            timerManager.start(TimerType.FOCUS)
            val twoMinutes = 2.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.finish()
            timerManager.reset()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 1)
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.finish()
            timerManager.reset()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 2)
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.finish()
            timerManager.reset()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 3)
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.finish()
            timerManager.next()
            assertEquals(timerManager.timerData.value.longBreakData.streak, 4)
            assertEquals(timerManager.timerData.value.type, TimerType.LONG_BREAK)
        }

    @Test
    fun `No long break if idled`() =
        runTest {
            val sessionsBeforeLongBreak =
                timerManager.timerData.value.label.profile.sessionsBeforeLongBreak
            assertEquals(
                timerManager.timerData.value.longBreakData
                    .streakInUse(sessionsBeforeLongBreak),
                0,
            )
            timerManager.start(TimerType.FOCUS)
            val twoMinutes = 2.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.skip()
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
            assertEquals(
                timerManager.timerData.value.longBreakData
                    .streakInUse(sessionsBeforeLongBreak),
                1,
            )
            timerManager.skip()
            assertEquals(timerManager.timerData.value.type, TimerType.FOCUS)
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.skip()
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
            assertEquals(
                timerManager.timerData.value.longBreakData
                    .streakInUse(sessionsBeforeLongBreak),
                2,
            )
            timerManager.skip()
            assertEquals(timerManager.timerData.value.type, TimerType.FOCUS)
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.skip()
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
            assertEquals(
                timerManager.timerData.value.longBreakData
                    .streakInUse(sessionsBeforeLongBreak),
                3,
            )
            timerManager.reset()
            val oneHour = 1.hours.inWholeMilliseconds
            timeProvider.elapsedRealtime += oneHour
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.skip()
            assertEquals(
                timerManager.timerData.value.longBreakData
                    .streakInUse(sessionsBeforeLongBreak),
                1,
            )
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
        }

    @Test
    fun `Reset streak after idle`() =
        runTest {
            assertEquals(timerManager.timerData.value.longBreakData.streak, 0)
            timerManager.start(TimerType.FOCUS)
            val twoMinutes = 2.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += twoMinutes
            timerManager.skip()
            assertEquals(1, timerManager.timerData.value.longBreakData.streak)
            val oneHour = 1.hours.inWholeMilliseconds
            timeProvider.elapsedRealtime += oneHour
            timerManager.start(TimerType.FOCUS)
            assertEquals(0, timerManager.timerData.value.longBreakData.streak)
        }

    @Test
    fun `Auto-start break`() =
        runTest {
            settingsRepo.setAutoStartBreak(true)
            timerManager.start(TimerType.FOCUS)
            val workDuration = DEFAULT_DURATION
            timeProvider.elapsedRealtime += workDuration
            timerManager.finish()
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
        }

    @Test
    fun `Work is next after work when break is disabled`() =
        runTest {
            settingsRepo.setAutoStartWork(true)
            localDataRepo.updateDefaultLabel(
                defaultLabel.copy(
                    timerProfile =
                        TimerProfile().copy(
                            isBreakEnabled = false,
                        ),
                ),
            )
            timerManager.start(TimerType.FOCUS)
            val workDuration = DEFAULT_WORK_DURATION.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += workDuration
            timerManager.next()
            assertEquals(timerManager.timerData.value.type, TimerType.FOCUS)
        }

    @Test
    fun `Count-up work then count-down the break budget`() =
        runTest {
            settingsRepo.activateLabelWithName(countUpLabel.name)

            timerManager.start(TimerType.FOCUS)
            val workDuration = 6.minutes.inWholeMilliseconds
            val expectedBreakBudget =
                workDuration.milliseconds.inWholeMinutes / countUpLabel.timerProfile.workBreakRatio
            timeProvider.elapsedRealtime += workDuration
            testScope.advanceTimeBy(workDuration)

            timerManager.next()
            assertEquals(
                timerManager.timerData.value
                    .getBreakBudget(
                        timeProvider.elapsedRealtime,
                    ).inWholeMinutes,
                expectedBreakBudget,
            )
            assertEquals(timerManager.timerData.value.type, TimerType.BREAK)
            timeProvider.elapsedRealtime += expectedBreakBudget.minutes.inWholeMilliseconds
            timerManager.next()
            assertEquals(
                timerManager.timerData.value
                    .getBreakBudget(
                        timeProvider.elapsedRealtime,
                    ).inWholeMinutes,
                0,
            )
            assertEquals(timerManager.timerData.value.type, TimerType.FOCUS)
        }

    @Test
    fun `Count-up then reset then wait for a while to start another count-up`() =
        runTest {
            settingsRepo.activateLabelWithName(countUpLabel.name)

            timerManager.start()
            val workTime = 12.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += workTime
            testScope.advanceTimeBy(workTime)

            var expectedBreakBudget =
                workTime.milliseconds.inWholeMinutes / countUpLabel.timerProfile.workBreakRatio
            timerManager.reset()
            assertEquals(
                timerManager.timerData.value
                    .getBreakBudget(
                        timeProvider.elapsedRealtime,
                    ).inWholeMinutes,
                expectedBreakBudget,
            )

            val idleTime = 3.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += idleTime
            testScope.advanceTimeBy(idleTime)

            expectedBreakBudget -= idleTime.milliseconds.inWholeMinutes.toInt()
            assertEquals(
                timerManager.timerData.value
                    .getBreakBudget(
                        timeProvider.elapsedRealtime,
                    ).inWholeMinutes,
                expectedBreakBudget,
                "The break budget should have decreased while idling",
            )

            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += workTime
            testScope.advanceTimeBy(workTime)

            timerManager.reset()
            val extraBreakBudget =
                workTime.milliseconds.inWholeMinutes / countUpLabel.timerProfile.workBreakRatio

            val breakBudgetAtTheEnd = expectedBreakBudget + extraBreakBudget
            assertEquals(
                timerManager.timerData.value
                    .getBreakBudget(
                        timeProvider.elapsedRealtime,
                    ).inWholeMinutes,
                breakBudgetAtTheEnd,
                "The previous unused break budget should have been added to the total",
            )
            timerManager.start(TimerType.FOCUS)
            timeProvider.elapsedRealtime += idleTime
            testScope.advanceTimeBy(idleTime)
            timerManager.finish()

            assertEquals(
                timerManager.timerData.value
                    .getBreakBudget(
                        timeProvider.elapsedRealtime,
                    ).inWholeMinutes,
                breakBudgetAtTheEnd - idleTime.milliseconds.inWholeMinutes.toInt(),
                "The break budget should have decreased while idling",
            )
        }

    @Test
    fun `Count-up then start a break with budget already there`() =
        runTest {
            settingsRepo.setBreakBudgetData(BreakBudgetData(10.minutes, 0))
            settingsRepo.activateLabelWithName(countUpLabel.name)
            timerManager.restart()

            timerManager.start()
            timerManager.next()

            assertEquals(
                timerManager.timerData.value.endTime,
                timerManager.timerData.value.breakBudgetData.breakBudget.inWholeMilliseconds,
            )
            assertEquals(
                listOf(
                    Event.Start(isFocus = true, endTime = COUNT_UP_HARD_LIMIT),
                    Event.Start(isFocus = false, endTime = 10.minutes.inWholeMilliseconds),
                ),
                fakeEventListener.events,
            )
        }

    @Test
    fun `Count-up then start break then auto-start work`() =
        runTest {
            val breakBudget = 3.minutes
            val breakBudgetMillis = breakBudget.inWholeMilliseconds

            settingsRepo.setBreakBudgetData(BreakBudgetData(breakBudget, 0))
            settingsRepo.activateLabelWithName(countUpLabel.name)
            settingsRepo.setAutoStartWork(true)
            timerManager.restart()

            timerManager.start()
            timerManager.next()
            timeProvider.elapsedRealtime += breakBudgetMillis
            testScope.advanceTimeBy(breakBudgetMillis)
            timerManager.finish()

            assertEquals(
                expected =
                    listOf(
                        Event.Start(isFocus = true, endTime = COUNT_UP_HARD_LIMIT),
                        Event.Start(isFocus = false, endTime = breakBudgetMillis),
                        Event.Finished(type = TimerType.BREAK, autostartNextSession = true),
                        Event.Start(isFocus = true, endTime = COUNT_UP_HARD_LIMIT + breakBudgetMillis, autoStarted = true),
                    ),
                actual = fakeEventListener.events,
            )
        }

    @Test
    fun `Count-up then start break then observe remaining budget`() =
        runTest {
            val breakBudget = 3.minutes
            val oneMinute = 1.minutes.inWholeMilliseconds

            settingsRepo.setBreakBudgetData(BreakBudgetData(breakBudget, 0))
            settingsRepo.activateLabelWithName(countUpLabel.name)
            settingsRepo.setAutoStartWork(true)
            timerManager.restart()

            timerManager.start()
            timerManager.next()
            timeProvider.elapsedRealtime += oneMinute
            testScope.advanceTimeBy(oneMinute)
            assertEquals(
                timerManager.timerData.value.getBreakBudget(timeProvider.elapsedRealtime),
                breakBudget - 1.minutes,
            )
            timeProvider.elapsedRealtime += oneMinute
            testScope.advanceTimeBy(oneMinute)
            assertEquals(
                timerManager.timerData.value.getBreakBudget(timeProvider.elapsedRealtime),
                breakBudget - 2.minutes,
            )
            timeProvider.elapsedRealtime += oneMinute
            testScope.advanceTimeBy(oneMinute)
            assertEquals(
                timerManager.timerData.value.getBreakBudget(timeProvider.elapsedRealtime),
                breakBudget - 3.minutes,
            )
        }

    @Test
    fun `Count-up then work for a while then change the work break ratio`() =
        runTest {
            settingsRepo.activateLabelWithName(countUpLabel.name)

            timerManager.start()
            val workTime = 12.minutes.inWholeMilliseconds
            timeProvider.elapsedRealtime += workTime
            testScope.advanceTimeBy(workTime)

            val expectedBreakBudget =
                workTime.milliseconds / countUpLabel.timerProfile.workBreakRatio
            assertEquals(
                timerManager.timerData.value.getBreakBudget(timeProvider.elapsedRealtime),
                expectedBreakBudget,
            )
            timerManager.reset()

            val newWorkBreakRatio = 1
            localDataRepo.updateLabel(
                countUpLabel.name,
                countUpLabel.copy(timerProfile = countUpLabel.timerProfile.copy(workBreakRatio = newWorkBreakRatio)),
            )
            timerManager.start()
            timeProvider.elapsedRealtime += workTime
            testScope.advanceTimeBy(workTime)

            val extraBreakBudget =
                workTime.milliseconds / newWorkBreakRatio
            timerManager.reset()

            assertEquals(
                timerManager.timerData.value.breakBudgetData.breakBudget,
                expectedBreakBudget + extraBreakBudget,
                "The break budget should have been recalculated",
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

        private val countUpLabel =
            Label(
                name = "flow",
                useDefaultTimeProfile = false,
                timerProfile = TimerProfile(isCountdown = false, workBreakRatio = 3),
            )
    }
}
