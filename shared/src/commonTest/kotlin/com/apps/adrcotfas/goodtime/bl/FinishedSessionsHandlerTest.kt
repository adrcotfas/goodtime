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
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepositoryImpl
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.fakes.FakeLabelDao
import com.apps.adrcotfas.goodtime.fakes.FakeSessionDao
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimerProfileDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FinishedSessionsHandlerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var repo: LocalDataRepository
    private lateinit var handler: FinishedSessionsHandler

    private val session =
        Session.create(
            timestamp = 1_000L,
            duration = 25,
            interruptions = 0,
            label = Label.DEFAULT_LABEL_NAME,
            isWork = true,
            notes = "",
        )

    @BeforeTest
    fun setup() =
        runTest(testDispatcher) {
            settingsRepo = FakeSettingsRepository()
            repo =
                LocalDataRepositoryImpl(
                    sessionDao = FakeSessionDao(),
                    labelDao = FakeLabelDao(),
                    timerProfileDao = FakeTimerProfileDao(),
                    settingsRepo = settingsRepo,
                    coroutineScope = testScope,
                )
            handler =
                FinishedSessionsHandler(
                    coroutineScope = testScope,
                    repo = repo,
                    settingsRepo = settingsRepo,
                    log = Logger(StaticConfig()),
                )
        }

    @Test
    fun `saveSession inserts and records the inserted id`() =
        runTest(testDispatcher) {
            handler.saveSession(session)

            val sessions = repo.selectAllSessions().first()
            assertEquals(1, sessions.size)
            assertEquals(sessions.first().id, settingsRepo.settings.first().lastInsertedSessionId)
        }

    @Test
    fun `updateLastFinishedSessionNotes only touches the notes`() =
        runTest(testDispatcher) {
            handler.saveSession(session)
            handler.updateLastFinishedSessionNotes("stayed focused")

            val stored = repo.selectAllSessions().first().single()
            assertEquals("stayed focused", stored.notes)
            assertEquals(session.duration, stored.duration)
        }

    @Test
    fun `updateSession replaces the last inserted session`() =
        runTest(testDispatcher) {
            handler.saveSession(session)
            handler.updateSession(session.copy(duration = 30, notes = "extended"))

            val stored = repo.selectAllSessions().first().single()
            assertEquals(30, stored.duration)
            assertEquals("extended", stored.notes)
        }

    @Test
    fun `updates are no-ops after resetLastInsertedSessionId`() =
        runTest(testDispatcher) {
            handler.saveSession(session)
            handler.resetLastInsertedSessionId()

            handler.updateSession(session.copy(duration = 30))
            handler.updateLastFinishedSessionNotes("ignored")

            val stored = repo.selectAllSessions().first().single()
            assertEquals(session.duration, stored.duration)
            assertEquals("", stored.notes)
        }
}
