/**
 *     Goodtime Productivity
 *     Copyright (C) Adrian Cotfas
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
import com.apps.adrcotfas.goodtime.data.local.ProductivityDatabase
import com.apps.adrcotfas.goodtime.data.local.RoomDatabaseTest
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerLabelRemovalTest : RoomDatabaseTest() {
    private lateinit var db: ProductivityDatabase
    private val dispatcher = UnconfinedTestDispatcher()

    @AfterTest
    fun after() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun `Deleting the active label mid-session saves the session to the default label`() = removeActiveLabelMidSession(
        expectedLabel = Label.DEFAULT_LABEL_NAME,
        expectedIsArchived = false,
    ) { repo, name -> repo.deleteLabel(name) }

    @Test
    fun `Archiving the active label mid-session saves the session to the archived label`() = removeActiveLabelMidSession(
        expectedLabel = countUpLabel.name,
        expectedIsArchived = true,
    ) { repo, name -> repo.updateLabelIsArchived(name, true) }

    private fun removeActiveLabelMidSession(
        expectedLabel: String,
        expectedIsArchived: Boolean,
        remove: suspend (LocalDataRepository, String) -> Unit,
    ) = runTest(dispatcher) {
        val errors = mutableListOf<Throwable>()
        val scope =
            CoroutineScope(
                dispatcher +
                    SupervisorJob() +
                    CoroutineExceptionHandler { _, e -> errors += e },
            )
        val logger = Logger(StaticConfig())
        val timeProvider = FakeTimeProvider()
        val settingsRepo = FakeSettingsRepository()
        db = getInMemoryDatabaseBuilder().build()
        val repo =
            LocalDataRepositoryImpl(
                sessionDao = db.sessionsDao(),
                labelDao = db.labelsDao(),
                timerProfileDao = db.timerProfileDao(),
                settingsRepo = settingsRepo,
                coroutineScope = scope,
            )
        repo.insertLabel(Label.defaultLabel())
        repo.insertLabel(countUpLabel)
        settingsRepo.update { it.copy(labelName = countUpLabel.name) }

        val timerManager =
            TimerManager(
                localDataRepo = repo,
                settingsRepo = settingsRepo,
                listeners = emptyList(),
                timeProvider,
                FinishedSessionsHandler(scope, repo, settingsRepo, logger),
                BreakBudgetManager(settingsRepo, timeProvider, scope, logger),
                StreakManager(settingsRepo, timeProvider, scope, logger),
                logger,
                coroutineScope = scope,
                timerStateRestoration = TimerStateRestoration(settingsRepo, timeProvider, logger),
            )
        timerManager.setup()
        timerManager.timerData.first { it.isReady && it.getLabelName() == countUpLabel.name }

        timerManager.start(TimerType.FOCUS)
        timeProvider.elapsedRealtime += 5.minutes.inWholeMilliseconds

        remove(repo, countUpLabel.name)
        settingsRepo.update { it.copy(labelName = Label.DEFAULT_LABEL_NAME) }
        timerManager.timerData.first { it.getLabelName() == Label.DEFAULT_LABEL_NAME }
        withContext(Dispatchers.Default) {
            withTimeoutOrNull(5.seconds) {
                while (errors.isEmpty() && repo.selectAllSessions().first().isEmpty()) delay(50)
            }
        }

        assertEquals(emptyList(), errors.map { it.toString() })
        val session = repo.selectAllSessions().first().single()
        assertEquals(expectedLabel, session.label)
        assertEquals(expectedIsArchived, session.isArchived)
        scope.cancel()
    }

    private val countUpLabel =
        Label(
            name = "flow",
            useDefaultTimeProfile = false,
            timerProfile = TimerProfile(isCountdown = false, workBreakRatio = 3),
        )
}
