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
package com.apps.adrcotfas.goodtime.data.local

import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.model.TimerProfile.Companion.DEFAULT_WORK_DURATION
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

class LocalDataRepositoryTest : RoomDatabaseTest() {
    private lateinit var repo: LocalDataRepository
    private lateinit var db: ProductivityDatabase

    private lateinit var settingsRepo: SettingsRepository

    @AfterTest
    fun after() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    @BeforeTest
    fun setup() =
        runTest {
            settingsRepo = FakeSettingsRepository()
            db = getInMemoryDatabaseBuilder().build()
            repo =
                LocalDataRepositoryImpl(
                    sessionDao = db.sessionsDao(),
                    labelDao = db.labelsDao(),
                    timerProfileDao = db.timerProfileDao(),
                    settingsRepo = settingsRepo,
                    coroutineScope = this,
                )
            repo.deleteAllSessions()
            repo.deleteAllLabels()
            repo.insertLabel(label)
            repo.insertLabel(Label.defaultLabel())
            repo.insertSession(session)
        }

    @Test
    fun selectByName() =
        runTest {
            val label = repo.selectLabelByName(LABEL_NAME).first()
            assertEquals(LABEL_NAME, label!!.name, "selectLabelByName failed")
        }

    @Test
    fun selectAllSessions() =
        runTest {
            val sessions = repo.selectAllSessions().first()
            assertTrue { sessions.size == 1 }
            val labels = repo.selectAllLabels().first()
            assertTrue { labels.size == 2 }
        }

    @Test
    fun verifyForeignKeyCascadeOnLabelChange() =
        runTest {
            val session = repo.selectAllSessions().first().first()
            repo.updateSession(
                session.id,
                Session(
                    id = session.id,
                    timestamp = 1.minutes.inWholeMilliseconds,
                    duration = 1.minutes.toLong(DurationUnit.MINUTES),
                    interruptions = 0,
                    label = LABEL_NAME,
                    notes = "",
                    isWork = true,
                    isArchived = false,
                ),
            )
            assertTrue(
                repo.selectAllSessions().first().first().run {
                    this.label == LABEL_NAME && !this.isArchived
                },
                "Cascade update of Session's label failed after updateSession",
            )

            val newLabelName = "new"
            repo.updateLabel(LABEL_NAME, label.copy(name = newLabelName))
            assertEquals(
                newLabelName,
                repo
                    .selectAllSessions()
                    .first()
                    .first()
                    .label,
                "Cascade update of Session's label failed after updateLabelName",
            )

            repo.updateLabelIsArchived(newLabelName, newIsArchived = true)
            assertEquals(
                true,
                repo
                    .selectAllSessions()
                    .first()
                    .first()
                    .isArchived,
                "Cascade update of Session's label failed after updateLabelIsArchived",
            )
            repo.deleteAllLabels()
            assertTrue(
                repo.selectAllSessions().first().first().run {
                    this.label == Label.DEFAULT_LABEL_NAME && !this.isArchived
                },
                "Cascade update of Session's label failed after deleteAllLabels",
            )
        }

    @Test
    fun selectEntitiesByLabel() =
        runTest {
            val filteredSessions = repo.selectSessionsByLabel(LABEL_NAME).first()
            assertEquals(0, filteredSessions.size, "There should be no sessions with this label")

            val allSessions = repo.selectAllSessions().first()
            repo.updateSession(
                allSessions.first().id,
                allSessions.first().copy(label = LABEL_NAME),
            )
            assertEquals(
                1,
                repo.selectSessionsByLabel(LABEL_NAME).first().size,
                "updateSession failed",
            )
        }

    @Test
    fun selectEntitiesByIsArchived() =
        runTest {
            val expectedArchivedSessions = 3
            repeat(expectedArchivedSessions) {
                repo.insertSession(session.copy(label = LABEL_NAME))
            }
            repo.updateLabelIsArchived(LABEL_NAME, true)

            val sessions = repo.selectSessionsByIsArchived(true).first()
            assertEquals(expectedArchivedSessions, sessions.size, "selectSessionsByIsArchived failed")

            repo.insertLabel(label.copy(name = "ceva", isArchived = true))
            repo.insertLabel(label.copy(name = "fin", isArchived = true))

            val labels = repo.selectLabelsByArchived(isArchived = true).first()
            assertEquals(3, labels.size, "selectLabelsByArchived failed")
        }

    @Test
    fun updateLabelProperties() =
        runTest {
            val expectedColorIndex = 9
            val expectedOrderIndex = 10L
            val expectedFollowDefaultTimeProfile = false
            repo.updateLabel(
                LABEL_NAME,
                label.copy(
                    colorIndex = expectedColorIndex,
                    orderIndex = expectedOrderIndex,
                    useDefaultTimeProfile = expectedFollowDefaultTimeProfile,
                ),
            )
            repo.updateLabelOrderIndex(LABEL_NAME, expectedOrderIndex)

            val labels = repo.selectAllLabels().first()
            val label = labels.firstOrNull { it.name == LABEL_NAME }
            assertNotNull(label, "label not found")
            assertEquals(expectedColorIndex, label.colorIndex, "updateLabelColorIndex failed")
            assertEquals(expectedOrderIndex, label.orderIndex, "updateLabelOrderIndex failed")
            assertEquals(
                expectedFollowDefaultTimeProfile,
                label.useDefaultTimeProfile,
                "updateShouldFollowDefaultTimeProfile failed",
            )
        }

    @Test
    fun deleteEntities() =
        runTest {
            repo.deleteAllSessions()
            val sessions = repo.selectAllSessions().first()
            assertTrue(sessions.isEmpty(), "deleteAllSessions failed")

            repo.insertSession(session)
            assertEquals(1, repo.selectAllSessions().first().size, "insertSession failed")

            val sessionId =
                repo
                    .selectAllSessions()
                    .first()
                    .first()
                    .id
            repo.deleteSessions(listOf(sessionId))
            assertEquals(0, repo.selectAllSessions().first().size, "deleteSession failed")

            repo.deleteAllLabels()
            val labels = repo.selectAllLabels().first()
            assertNotNull(
                labels.firstOrNull { it.name == Label.DEFAULT_LABEL_NAME },
                "default label should always be present",
            )

            val labelToDeleteName = "ceva"
            repo.insertLabel(label.copy(name = labelToDeleteName))
            assertEquals(2, repo.selectAllLabels().first().size, "insertLabel failed")
            repo.deleteLabel(labelToDeleteName)
            assertEquals(1, repo.selectAllLabels().first().size, "deleteLabel failed")
        }

    @Test
    fun editSessionLabelExcept() =
        runTest {
            repeat(3) {
                repo.insertLabel(Label.defaultLabel().copy(name = "label$it"))
            }
            repeat(21) {
                repo.insertSession(session.copy(id = it.toLong(), label = "label${it % 3}"))
            }

            val allSessions = repo.selectAllSessions().first()
            repo.updateSessionsLabelByIdsExcept(
                newLabel = "label1",
                unselectedIds = emptyList(),
                selectedLabels = emptyList(),
            )
            assertEquals(allSessions, repo.selectAllSessions().first())

            val label0SessionsSize =
                allSessions
                    .filter {
                        it.label == "label0"
                    }.size
            val label1SessionsSize =
                allSessions
                    .filter {
                        it.label == "label1"
                    }.size

            val label1SessionsIds =
                allSessions
                    .filter {
                        it.label == "label1"
                    }.map { it.id }

            repo.updateSessionsLabelByIdsExcept(
                newLabel = "label1",
                unselectedIds = label1SessionsIds,
                selectedLabels = listOf("label1"),
            )
            assertEquals(allSessions, repo.selectAllSessions().first())

            repo.updateSessionsLabelByIdsExcept(
                newLabel = "label1",
                unselectedIds = emptyList(),
                selectedLabels = listOf("label0"),
            )
            assertEquals(
                0,
                repo
                    .selectAllSessions()
                    .first()
                    .filter { it.label == "label0" }
                    .size,
            )
            assertEquals(
                label0SessionsSize + label1SessionsSize,
                repo
                    .selectAllSessions()
                    .first()
                    .filter { it.label == "label1" }
                    .size,
            )

            repo.updateSessionsLabelByIdsExcept(
                newLabel = "label2",
                unselectedIds = emptyList(),
                selectedLabels = listOf("label0", "label1", "label2"),
            )

            assertEquals(
                allSessions.size,
                repo
                    .selectAllSessions()
                    .first()
                    .filter { it.label == "label2" }
                    .size,
            )
        }

    @Test
    fun deleteSessionsExcept() =
        runTest {
            repeat(5) {
                repo.insertLabel(Label.defaultLabel().copy(name = "label$it"))
            }
            repeat(20) {
                repo.insertSession(session.copy(id = it.toLong(), label = "label${it % 5}"))
            }

            val allSessions = repo.selectAllSessions().first()

            repo.deleteSessionsExcept(
                unselectedIds = emptyList(),
                selectedLabels = emptyList(),
            )

            assertEquals(allSessions.size, repo.selectAllSessions().first().size)

            val label0SessionsIds =
                allSessions
                    .filter {
                        it.label == "label0"
                    }.map { it.id }

            val label1SessionsIds =
                allSessions
                    .filter {
                        it.label == "label1"
                    }.map { it.id }

            repo.deleteSessionsExcept(
                unselectedIds = label1SessionsIds,
                selectedLabels = listOf("label1"),
            )
            var remainingSessions = repo.selectAllSessions().first()
            assertEquals(
                allSessions.size,
                remainingSessions.size,
                "expecting that no sessions were deleted",
            )

            repo.deleteSessionsExcept(
                unselectedIds = listOf(),
                selectedLabels = listOf("label0"),
            )

            remainingSessions = repo.selectAllSessions().first()
            assertEquals(allSessions.size - label0SessionsIds.size, remainingSessions.size)

            val allLabelsNames = repo.selectAllLabels().first().map { it.name }
            repo.deleteSessionsExcept(
                unselectedIds = emptyList(),
                selectedLabels = allLabelsNames,
            )
            remainingSessions = repo.selectAllSessions().first()
            assertEquals(0, remainingSessions.size)
        }

    companion object {
        private const val LABEL_NAME = "label_name"
        private var label =
            Label(
                name = LABEL_NAME,
                colorIndex = 0,
                orderIndex = 0,
                useDefaultTimeProfile = false,
                timerProfile = TimerProfile(),
                isArchived = false,
            )

        private val DEFAULT_DURATION = DEFAULT_WORK_DURATION.minutes.inWholeMilliseconds

        private var session =
            Session(
                id = 0,
                timestamp = DEFAULT_DURATION,
                duration = 25,
                interruptions = 0,
                label = Label.DEFAULT_LABEL_NAME,
                notes = "",
                isWork = true,
                isArchived = false,
            )
    }
}
