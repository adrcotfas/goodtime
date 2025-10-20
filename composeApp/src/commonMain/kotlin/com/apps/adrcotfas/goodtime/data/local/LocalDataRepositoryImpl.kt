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

import androidx.paging.PagingSource
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.model.toExternal
import com.apps.adrcotfas.goodtime.data.model.toLocal
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class LocalDataRepositoryImpl(
    private var sessionDao: SessionDao,
    private var labelDao: LabelDao,
    private var timerProfileDao: TimerProfileDao,
    private val settingsRepo: SettingsRepository,
    private val coroutineScope: CoroutineScope,
) : LocalDataRepository {
    init {
        insertDefaultLabel()
    }

    override fun reinitDatabase(database: ProductivityDatabase) {
        sessionDao = database.sessionsDao()
        labelDao = database.labelsDao()
        timerProfileDao = database.timerProfileDao()
        insertDefaultLabel()
    }

    private fun insertDefaultLabel() {
        coroutineScope.launch {
            val insert = !settingsRepo.settings.map { it.timeProfilesInitialized }.first()
            if (insert) {
                timerProfileDao.insert(
                    TimerProfile(
                        name = LocalTimerProfile.DEFAULT_PROFILE_NAME,
                        workDuration = 25,
                        breakDuration = 5,
                        isLongBreakEnabled = false,
                    ).toLocal(),
                )
                timerProfileDao.insert(
                    TimerProfile(
                        name = LocalTimerProfile.PROFILE_50_10_NAME,
                        workDuration = 50,
                        breakDuration = 10,
                        isLongBreakEnabled = false,
                    ).toLocal(),
                )
                timerProfileDao.insert(
                    TimerProfile(
                        name = LocalTimerProfile.POMODORO_PROFILE_NAME,
                        workDuration = 25,
                        breakDuration = 5,
                        isLongBreakEnabled = true,
                        longBreakDuration = 15,
                        sessionsBeforeLongBreak = 4,
                    ).toLocal(),
                )
                timerProfileDao.insert(
                    TimerProfile(
                        name = LocalTimerProfile.THIRD_TIME_PROFILE_NAME,
                        isCountdown = false,
                        workBreakRatio = 3,
                    ).toLocal(),
                )
                settingsRepo.setTimeProfilesInitialized(true)
            }

            val localLabel = Label.defaultLabel().toLocal()
            labelDao.insert(localLabel)
        }
    }

    override suspend fun insertSession(session: Session): Long = sessionDao.insert(session.toLocal())

    override suspend fun updateSession(
        id: Long,
        newSession: Session,
    ) {
        val localSession = newSession.toLocal()
        sessionDao.update(
            newTimestamp = localSession.timestamp,
            newDuration = localSession.duration,
            newInterruptions = localSession.interruptions,
            newLabel = localSession.labelName,
            newNotes = localSession.notes,
            newIsWork = localSession.isWork,
            id = id,
        )
    }

    override suspend fun updateSessionsLabelByIds(
        newLabel: String,
        ids: List<Long>,
    ) {
        sessionDao.updateLabelByIds(newLabel, ids)
    }

    override suspend fun updateSessionsLabelByIdsExcept(
        newLabel: String,
        unselectedIds: List<Long>,
        selectedLabels: List<String>,
        considerBreaks: Boolean,
    ) {
        sessionDao.updateLabelByIdsExcept(newLabel, unselectedIds, selectedLabels, considerBreaks)
    }

    override fun selectAllSessions(): Flow<List<Session>> = sessionDao.selectAll().map { it.map { sessions -> sessions.toExternal() } }

    override fun selectSessionsAfter(timestamp: Long): Flow<List<Session>> =
        sessionDao
            .selectAfter(timestamp)
            .map { sessions -> sessions.map { it.toExternal() } }

    override fun selectSessionById(id: Long): Flow<Session> = sessionDao.selectById(id).map { it.toExternal() }

    override fun selectSessionsByIsArchived(isArchived: Boolean): Flow<List<Session>> =
        sessionDao
            .selectByIsArchived(isArchived)
            .map { sessions -> sessions.map { it.toExternal() } }

    override fun selectSessionsByLabel(label: String): Flow<List<Session>> =
        sessionDao.selectByLabel(label).map { sessions ->
            sessions.map {
                it.toExternal()
            }
        }

    override fun selectSessionsByLabels(labels: List<String>): Flow<List<Session>> =
        sessionDao
            .selectByLabels(labels)
            .map { sessions -> sessions.map { it.toExternal() } }

    override fun selectSessionsByLabels(
        labels: List<String>,
        after: Long,
    ): Flow<List<Session>> =
        sessionDao
            .selectByLabels(labels, after)
            .map { sessions -> sessions.map { it.toExternal() } }

    override fun selectSessionsForTimelinePaged(
        labels: List<String>,
        showBreaks: Boolean,
    ): PagingSource<Int, LocalSession> = sessionDao.selectSessionsForTimelinePaged(labels, showBreaks)

    override fun selectNumberOfSessionsAfter(timestamp: Long): Flow<Int> = sessionDao.selectNumberOfSessionsAfter(timestamp)

    override suspend fun deleteSessions(ids: List<Long>) {
        sessionDao.delete(ids)
    }

    override suspend fun deleteSessionsExcept(
        unselectedIds: List<Long>,
        selectedLabels: List<String>,
        considerBreaks: Boolean,
    ) {
        sessionDao.deleteExcept(unselectedIds, selectedLabels, considerBreaks)
    }

    override suspend fun deleteAllSessions() {
        sessionDao.deleteAll()
    }

    override suspend fun insertLabel(label: Label): Long = labelDao.insert(label.toLocal())

    override suspend fun insertLabelAndBulkRearrange(
        label: Label,
        labelsToUpdate: List<Pair<String, Long>>,
    ) {
        labelDao.insertLabelAndBulkRearrange(label.toLocal(), labelsToUpdate)
    }

    override suspend fun updateLabelOrderIndex(
        name: String,
        newOrderIndex: Long,
    ) {
        labelDao.updateOrderIndex(newOrderIndex.toInt(), name)
    }

    override suspend fun bulkUpdateLabelOrderIndex(labelsToUpdate: List<Pair<String, Long>>) {
        labelDao.bulkUpdateLabelOrderIndex(labelsToUpdate)
    }

    override suspend fun updateLabel(
        name: String,
        newLabel: Label,
    ) {
        if (newLabel.name.isEmpty()) return
        val localLabel = newLabel.toLocal()
        labelDao.updateLabel(
            newName = localLabel.name,
            newColorIndex = localLabel.colorIndex,
            newUseDefaultTimeProfile = localLabel.useDefaultTimeProfile,
            newTimerProfileName = localLabel.timerProfileName,
            newIsCountdown = localLabel.isCountdown,
            newWorkDuration = localLabel.workDuration,
            newIsBreakEnabled = localLabel.isBreakEnabled,
            newBreakDuration = localLabel.breakDuration,
            newIsLongBreakEnabled = localLabel.isLongBreakEnabled,
            newLongBreakDuration = localLabel.longBreakDuration,
            newSessionsBeforeLongBreak = localLabel.sessionsBeforeLongBreak,
            newWorkBreakRatio = localLabel.workBreakRatio,
            name = name,
        )
    }

    override suspend fun updateDefaultLabel(newDefaultLabel: Label) {
        updateLabel(Label.DEFAULT_LABEL_NAME, newDefaultLabel)
    }

    override fun selectDefaultLabel() = selectLabelByName(Label.DEFAULT_LABEL_NAME)

    override suspend fun updateLabelIsArchived(
        name: String,
        newIsArchived: Boolean,
    ) {
        labelDao.updateIsArchived(newIsArchived, name)
    }

    override fun selectLabelByName(name: String): Flow<Label?> =
        labelDao.selectByName(name).flatMapLatest { localLabel ->
            if (localLabel?.timerProfileName != null) {
                timerProfileDao.selectByName(localLabel.timerProfileName).map { timerProfile ->
                    localLabel.toExternal(timerProfile?.toExternal())
                }
            } else {
                flowOf(localLabel?.toExternal())
            }
        }

    override fun selectAllLabels(): Flow<List<Label>> =
        labelDao.selectAll().flatMapLatest { localLabels ->
            val timerProfileNames = localLabels.mapNotNull { it.timerProfileName }.distinct()
            if (timerProfileNames.isEmpty()) {
                flowOf(localLabels.map { it.toExternal() })
            } else {
                timerProfileDao.selectByNames(timerProfileNames).map { timerProfiles ->
                    localLabels.map { localLabel ->
                        val matchingProfile =
                            timerProfiles.find { it.name == localLabel.timerProfileName }
                        localLabel.toExternal(matchingProfile?.toExternal())
                    }
                }
            }
        }

    override fun selectLabelsByArchived(isArchived: Boolean): Flow<List<Label>> =
        labelDao
            .selectByArchived(isArchived)
            .map { labels -> labels.map { it.toExternal() } }

    override suspend fun deleteLabel(name: String) {
        labelDao.deleteByName(name)
    }

    override suspend fun deleteAllLabels() {
        labelDao.deleteAll()
    }

    override suspend fun archiveAllButDefault() {
        labelDao.archiveAllButDefault()
    }

    override suspend fun insertTimerProfile(profile: TimerProfile) {
        timerProfileDao.insert(profile.toLocal())
    }

    override suspend fun insertTimerProfileAndSetDefault(profile: TimerProfile) {
        timerProfileDao.insertTimerProfileAndSetDefault(profile.toLocal())
    }

    override suspend fun deleteTimerProfile(name: String) {
        timerProfileDao.deleteByName(name)
    }

    override suspend fun selectTimerProfile(name: String): Flow<TimerProfile?> = timerProfileDao.selectByName(name).map { it?.toExternal() }

    override suspend fun selectAllTimerProfiles(): Flow<List<TimerProfile>> =
        timerProfileDao.selectAll().map { profiles ->
            profiles.map {
                it.toExternal()
            }
        }
}
