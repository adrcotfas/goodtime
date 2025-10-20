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
package com.apps.adrcotfas.goodtime.stats

import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepositoryImpl
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.toLocal
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeLabelDao
import com.apps.adrcotfas.goodtime.fakes.FakeSessionDao
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import com.apps.adrcotfas.goodtime.fakes.FakeTimerProfileDao
import com.apps.adrcotfas.goodtime.testutil.retryTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsViewModelTest {
    private lateinit var fakeSessionDao: FakeSessionDao
    private lateinit var fakeLabelDao: FakeLabelDao
    private lateinit var fakeTimerProfileDao: FakeTimerProfileDao
    private lateinit var localDataRepository: LocalDataRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var timeProvider: FakeTimeProvider

    private lateinit var viewModel: StatisticsViewModel

    @BeforeTest
    fun setup() =
        runTest {
            retryTest {
                fakeSessionDao = FakeSessionDao()
                fakeLabelDao = FakeLabelDao()
                timeProvider = FakeTimeProvider()
                fakeTimerProfileDao = FakeTimerProfileDao()
                settingsRepository = FakeSettingsRepository()
                localDataRepository =
                    LocalDataRepositoryImpl(
                        fakeSessionDao,
                        fakeLabelDao,
                        fakeTimerProfileDao,
                        settingsRepository,
                        this,
                    )
                viewModel = StatisticsViewModel(localDataRepository, settingsRepository, timeProvider)
                populateRepo()

                viewModel.uiState.test {
                    assertTrue { awaitItem() == StatisticsUiState() }
                    awaitItem()
                    assertTrue { awaitItem().labels.isNotEmpty() }
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    @AfterTest
    fun tearDown() =
        runTest {
            fakeSessionDao.deleteAll()
            fakeLabelDao.deleteAll()
        }

    @Test
    fun `Add and delete the same session`() =
        runTest {
            retryTest {
                var sessions = viewModel.pagedSessions.asSnapshot()
                assertTrue { sessions.isNotEmpty() }
                val sessionsSize = sessions.size
                viewModel.onAddEditSession()
                viewModel.saveSession()
                advanceUntilIdle()
                sessions = viewModel.pagedSessions.asSnapshot()
                assertTrue { sessionsSize + 1 == sessions.size }

                viewModel.toggleSessionIsSelected(0)
                viewModel.deleteSelectedSessions()
                advanceUntilIdle()
                sessions = viewModel.pagedSessions.asSnapshot()
                assertTrue { sessionsSize == sessions.size }
            }
        }

    @Test
    fun `Select all sessions and delete`() =
        runTest {
            retryTest {
                var sessions = viewModel.pagedSessions.asSnapshot()
                val sessionsSize = sessions.size
                viewModel.selectAllSessions(sessionsSize)
                viewModel.deleteSelectedSessions()
                advanceUntilIdle()
                sessions = viewModel.pagedSessions.asSnapshot()
                assertTrue { sessions.isEmpty() }
            }
        }

    @Test
    fun `Select all sessions except one and delete`() =
        runTest {
            retryTest {
                var sessions = viewModel.pagedSessions.asSnapshot()
                val sessionsSize = sessions.size
                viewModel.selectAllSessions(sessionsSize)
                viewModel.toggleSessionIsSelected(1)
                viewModel.deleteSelectedSessions()
                advanceUntilIdle()
                sessions = viewModel.pagedSessions.asSnapshot()
                assertTrue { sessions.first().id == 1L }
            }
        }

    @Test
    fun `Select label A and delete all`() =
        runTest {
            retryTest {
                viewModel.setSelectedLabels(listOf("A"))
                delay(10)
                val selected = viewModel.uiState.value.selectedLabels
                assertEquals(selected, listOf("A"))
                var sessions = viewModel.pagedSessions.asSnapshot()

                assertTrue { sessions.size == 3 }
                viewModel.selectAllSessions(sessions.size)
                viewModel.deleteSelectedSessions()
                advanceUntilIdle()
                sessions = viewModel.pagedSessions.asSnapshot()
                assertTrue { sessions.isEmpty() }

                viewModel.setSelectedLabels(listOf("A", "B", "C"))
                sessions = viewModel.pagedSessions.asSnapshot()
                assertTrue { sessions.firstOrNull { it.label == "A" } == null }
                assertTrue { sessions.size == 6 }
            }
        }

    @Test
    fun `Bulk edit label`() =
        runTest {
            retryTest {
                viewModel.selectAllSessions(viewModel.pagedSessions.asSnapshot().size)
                viewModel.setSelectedLabelToBulkEdit("B")
                viewModel.bulkEditLabel()
                advanceUntilIdle()
                val sessions = viewModel.pagedSessions.asSnapshot()
                assertTrue { sessions.firstOrNull { it.label == "A" } == null }
                assertTrue { sessions.size == 9 }
                assertTrue { sessions.firstOrNull { it.label != "B" } == null }
            }
        }

    private suspend fun populateRepo() {
        val labelA = Label.defaultLabel().copy(name = "A").toLocal()
        val labelB = labelA.copy(name = "B")
        val labelC = labelA.copy(name = "C")

        listOf(labelA, labelB, labelC).forEach {
            fakeLabelDao.insert(it)
        }

        val sessionA1 = Session.default().copy(id = 1, label = labelA.name).toLocal()
        val sessionA2 = sessionA1.copy(id = 2)
        val sessionA3 = sessionA1.copy(id = 3)

        val sessionB1 = sessionA1.copy(id = 4, labelName = labelB.name)
        val sessionB2 = sessionB1.copy(id = 5)
        val sessionB3 = sessionB1.copy(id = 6)

        val sessionC1 = sessionA1.copy(id = 7, labelName = labelC.name)
        val sessionC2 = sessionC1.copy(id = 8)
        val sessionC3 = sessionC1.copy(id = 9)

        listOf(
            sessionA1,
            sessionA2,
            sessionA3,
            sessionB1,
            sessionB2,
            sessionB3,
            sessionC1,
            sessionC2,
            sessionC3,
        ).forEach {
            fakeSessionDao.insert(it)
        }
    }
}
