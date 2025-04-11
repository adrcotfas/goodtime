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
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.settings.AppSettings
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Manages the timer state and provides methods to start, pause, resume and finish the timer.
 */
class TimerManager(
    private val localDataRepo: LocalDataRepository,
    private val settingsRepo: SettingsRepository,
    private val listeners: List<EventListener>,
    private val timeProvider: TimeProvider,
    private val finishedSessionsHandler: FinishedSessionsHandler,
    private val streakManager: StreakManager,
    private val breakBudgetManager: BreakBudgetManager,
    private val log: Logger,
    private val coroutineScope: CoroutineScope,
) {

    private var mainJob: Job? = null

    private val _timerData: MutableStateFlow<DomainTimerData> = MutableStateFlow(DomainTimerData())
    private lateinit var settings: AppSettings

    val timerData: StateFlow<DomainTimerData> = _timerData

    init {
        setup()
    }

    fun setup() {
        mainJob = coroutineScope.launch {
            initAndObserveLabelChange()
        }
    }

    fun restart() {
        mainJob?.cancel()
        setup()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun initAndObserveLabelChange() {
        settingsRepo.settings.map {
            settings = it
            it.labelName
        }.distinctUntilChanged().flatMapLatest { labelName ->
            localDataRepo.selectLabelByName(labelName)
                .combine(
                    localDataRepo.selectDefaultLabel().filterNotNull(),
                ) { label, defaultLabel ->
                    val defaultTimerProfile = defaultLabel.timerProfile
                    if (label == null) {
                        settingsRepo.activateDefaultLabel()
                        DomainLabel(defaultLabel, defaultTimerProfile)
                    } else {
                        DomainLabel(
                            label,
                            if (label.useDefaultTimeProfile) defaultTimerProfile else label.timerProfile,
                        )
                    }
                }
        }.distinctUntilChanged()
            .collect {
                log.i { "new timerProfile: $it" }
                val value = _timerData.value
                val isActive = value.state.isActive
                val isCountdown = value.label.isCountdown
                _timerData.update { data ->
                    data.copy(
                        isReady = true,
                        label = it,
                    )
                }
                if (isActive && isCountdown != it.isCountdown) {
                    log.i { "restarting the timer because the profile type changed" }
                    reset()
                    start()
                }
            }
    }

    fun start(timerType: TimerType = timerData.value.type, autoStarted: Boolean = false) {
        log.i { "Starting timer..." }
        val data = timerData.value
        if (!data.isReady) {
            log.e { "timer data not ready" }
            return
        }

        val elapsedRealTime = timeProvider.elapsedRealtime()
        val profile = data.getTimerProfile()

        // Update budget *before* calculating end time for count-up breaks
        val currentBreakBudget = if (data.state.isReset && !profile.isCountdown) {
            updateBreakBudgetIfNeeded()
        } else {
            breakBudgetManager.getPersistedBreakBudgetAmount() // Use existing budget if not reset
        }

        val newTimerData = timerData.value.copy(
            startTime = elapsedRealTime,
            lastStartTime = elapsedRealTime,
            // Pass break budget to getEndTime for count-up breaks
            endTime = data.getEndTime(timerType, elapsedRealTime, currentBreakBudget),
            state = TimerState.RUNNING,
            type = timerType,
            timeSpentPaused = 0,
        )

        _timerData.update { newTimerData }

        handlePersistentDataAtStart()
        finishedSessionsHandler.resetLastInsertedSessionId()

        val timerData = _timerData.value
        val isCountdown = timerData.isCurrentSessionCountdown()
        val countUpEndTime =
            computeCountUpEndTime(timerData.getBaseTime(timeProvider))

        listeners.forEach {
            it.onEvent(
                Event.Start(
                    autoStarted = autoStarted,
                    endTime = if (isCountdown) timerData.endTime else countUpEndTime,
                ),
            )
        }
    }

    private fun updateBreakBudgetIfNeeded(): Duration {
        val data = timerData.value
        return breakBudgetManager.updateAndPersistBreakBudget(
            timerType = data.type,
            timerState = data.state,
            timerProfile = data.label.profile,
            lastStartTime = data.lastStartTime,
        )
    }

    fun addOneMinute() {
        val data = timerData.value
        if (!data.state.isActive) {
            log.e { "Trying to add one minute when the timer is not running" }
            return
        }
        if (!data.getTimerProfile().isCountdown) {
            log.e { "Trying to add a minute to a timer that is not a countdown" }
            return
        }
        val newEndTime = data.endTime + 1.minutes.inWholeMilliseconds
        val newRemainingTimeAtPause = if (data.state.isPaused) {
            data.timeAtPause + 1.minutes.inWholeMilliseconds
        } else {
            0
        }

        _timerData.update {
            it.copy(
                endTime = newEndTime,
                timeAtPause = newRemainingTimeAtPause,
            )
        }
        log.i { "Added one minute" }
        listeners.forEach { it.onEvent(Event.AddOneMinute(newEndTime)) }
    }

    fun toggle() {
        when (timerData.value.state) {
            TimerState.RUNNING -> pause()
            TimerState.PAUSED -> resume()
            else -> log.e { "Trying to toggle the timer when it is not running or paused" }
        }
    }

    private fun pause() {
        val elapsedRealtime = timeProvider.elapsedRealtime()
        updateBreakBudgetIfNeeded()
        _timerData.update {
            it.copy(
                timeAtPause =
                if (it.label.profile.isCountdown) {
                    it.endTime - elapsedRealtime
                } else {
                    elapsedRealtime - it.startTime - it.timeSpentPaused
                },
                lastPauseTime = elapsedRealtime,
                state = TimerState.PAUSED,
            )
        }
        log.i { "Paused: ${timerData.value}" }
        listeners.forEach { it.onEvent(Event.Pause) }
    }

    private fun resume() {
        val elapsedRealTime = timeProvider.elapsedRealtime()
        updateBreakBudgetIfNeeded()
        updatePausedTime()
        val isCountdown = timerData.value.label.profile.isCountdown
        _timerData.update {
            it.copy(
                lastStartTime = elapsedRealTime,
                endTime = if (isCountdown) {
                    it.timeAtPause + elapsedRealTime
                } else {
                    it.endTime
                },
                state = TimerState.RUNNING,
                timeAtPause = 0,
            )
        }
        log.i { "Resumed: ${timerData.value}" }

        val timerData = _timerData.value
        val countUpEndTime =
            computeCountUpEndTime(timerData.getBaseTime(timeProvider))
        val isCurrentSessionCountdown = timerData.isCurrentSessionCountdown()
        listeners.forEach {
            it.onEvent(Event.Start(endTime = if (isCurrentSessionCountdown) timerData.endTime else countUpEndTime))
        }
    }

    private fun updatePausedTime() {
        val data = timerData.value
        if (data.lastPauseTime != 0L) {
            val elapsedRealTime = timeProvider.elapsedRealtime()
            val pausedTime = data.timeSpentPaused + elapsedRealTime - data.lastPauseTime
            log.i { "updatePausedTime: ${pausedTime.milliseconds}" }
            _timerData.update {
                it.copy(timeSpentPaused = pausedTime, lastPauseTime = 0)
            }
        }
    }

    /**
     * Skips the current session and starts the next one.
     * This is called manually by the user before a session is finished, interrupting the current session.
     */
    fun skip() {
        nextInternal(updateWorkTime = false, finishActionType = FinishActionType.MANUAL_SKIP)
    }

    fun next(
        updateWorkTime: Boolean = false,
        finishActionType: FinishActionType = FinishActionType.MANUAL_NEXT,
    ) {
        nextInternal(updateWorkTime, finishActionType)
    }

    fun updateNotesForLastCompletedSession(notes: String) {
        if (notes.isNotEmpty()) {
            finishedSessionsHandler.updateLastFinishedSessionNotes(notes.trim())
        }
    }

    /**
     * Called automatically when autoStart is enabled and the time is up or manually at the end of a session.
     */
    private fun nextInternal(
        updateWorkTime: Boolean = false,
        finishActionType: FinishActionType,
    ) {
        val data = timerData.value
        if (!data.isReady) {
            log.e { "timer data not ready" }
            return
        }
        val state = data.state
        val timerProfile = data.label

        if (state == TimerState.RESET) {
            log.e { "Trying to start the next session but the timer is reset" }
            return
        }

        val isWork = data.type.isWork
        val isCountDown = data.getTimerProfile().isCountdown

        val breakBudget = updateBreakBudgetIfNeeded()

        if (isWork && !isCountDown && breakBudget < 1.minutes) {
            log.e { "Break budget is depleted, cannot start break" }
            return
        }

        if (finishActionType != FinishActionType.AUTO) {
            handleFinishedSession(updateWorkTime, finishActionType = finishActionType)
        }

        _timerData.update { it.reset() }

        val nextType = when {
            !isWork || (isWork && !timerProfile.profile.isBreakEnabled) -> TimerType.WORK
            !isCountDown -> TimerType.BREAK
            streakManager.shouldConsiderStreak(timerProfile.profile, timeProvider.elapsedRealtime()) -> TimerType.LONG_BREAK
            else -> TimerType.BREAK
        }
        log.i { "Next: $nextType" }

        val autoStarted = (nextType.isWork && settings.autoStartWork) ||
            (nextType.isBreak && settings.autoStartBreak)

        start(nextType, autoStarted)
    }

    /**
     * Called when the time is up for countdown timers.
     * A finished [Session] is created and sent to the listeners.
     */
    fun finish() {
        val data = timerData.value
        if (!data.isReady) {
            log.e { "timer data not ready" }
            return
        }

        val state = data.state
        val timerProfile = data.label
        val type = data.type

        if (state.isReset || state.isFinished) {
            log.e { "Trying to finish the timer when it is reset or finished" }
            return
        }

        val endTimeInMillis = timeProvider.elapsedRealtime()
        _timerData.update { it.copy(state = TimerState.FINISHED, endTime = endTimeInMillis) }
        log.i { "Finish: $data" }

        updateBreakBudgetIfNeeded()
        handleFinishedSession(finishActionType = FinishActionType.AUTO)

        val autoStart =
            settings.autoStartWork && (type.isBreak || !timerProfile.profile.isBreakEnabled) ||
                settings.autoStartBreak && type.isWork && timerProfile.profile.isBreakEnabled
        log.i { "AutoStart: $autoStart" }
        listeners.forEach {
            it.onEvent(
                Event.Finished(
                    type = type,
                    autostartNextSession = autoStart,
                ),
            )
        }
        if (autoStart) {
            next(finishActionType = FinishActionType.AUTO)
        }
    }

    /**
     * Resets(stops) the timer.
     * This is also part of the flow after [finish] when the user has the option of starting a new session.
     * @param updateWorkTime if true, the duration of the already saved session will be updated.
     *                       This is useful when the user missed the notification and continued working.
     * @see [finish]
     */
    fun reset(
        updateWorkTime: Boolean = false,
        actionType: FinishActionType = FinishActionType.MANUAL_RESET,
    ) {
        val data = timerData.value
        if (data.state == TimerState.RESET) {
            log.w { "Trying to reset the timer when it is already reset" }
            return
        }
        log.i { "Reset: $data" }
        updateBreakBudgetIfNeeded()

        if (actionType != FinishActionType.MANUAL_DO_NOTHING) {
            handleFinishedSession(updateWorkTime, finishActionType = actionType)
        }

        listeners.forEach { it.onEvent(Event.Reset) }
        _timerData.update { it.reset() }
    }

    private fun handlePersistentDataAtStart() {
        if (timerData.value.type == TimerType.WORK) {
            streakManager.resetStreakIfNeeded(timerData.value.label.profile, timerData.value.endTime)
        }
    }

    private fun handleFinishedSession(
        updateWorkTime: Boolean = false,
        finishActionType: FinishActionType,
    ) {
        val data = timerData.value
        val isWork = data.type.isWork
        val isFinished = data.state.isFinished
        val isCountDown = data.getTimerProfile().isCountdown
        val longBreakEnabled = data.getTimerProfile().isLongBreakEnabled

        val session = createFinishedSession()
        session?.let {
            if (isFinished && updateWorkTime) {
                finishedSessionsHandler.updateSession(it)
                return
            } else if (!isFinished ||
                (isFinished && (finishActionType != FinishActionType.MANUAL_NEXT))
            ) {
                finishedSessionsHandler.saveSession(it)
            }
        }

        if (isWork && isCountDown && longBreakEnabled &&
            (
                finishActionType == FinishActionType.AUTO ||
                    finishActionType == FinishActionType.MANUAL_SKIP
                )
        ) {
            streakManager.incrementStreak()
        }
    }

    private fun createFinishedSession(): Session? {
        updatePausedTime()
        val data = timerData.value
        val isWork = data.type == TimerType.WORK

        val totalDuration = timeProvider.elapsedRealtime() - data.startTime
        val interruptions = data.timeSpentPaused

        val durationToSave = if (isWork) {
            val justWorkTime =
                (totalDuration - interruptions + WIGGLE_ROOM_MILLIS).milliseconds
            justWorkTime
        } else {
            totalDuration.milliseconds
        }

        val durationToSaveMinutes = durationToSave.inWholeMinutes
        if (durationToSaveMinutes < 1) {
            log.i { "The session was shorter than 1 minute: $durationToSave" }
            return null
        }

        _timerData.update {
            it.copy(completedMinutes = durationToSaveMinutes)
        }

        val now = timeProvider.now()

        return Session.create(
            timestamp = now,
            duration = durationToSaveMinutes,
            interruptions = if (isWork) interruptions.milliseconds.inWholeMinutes else 0,
            label = data.getLabelName(),
            isWork = isWork,
        )
    }

    fun onSendToBackground() {
        val timerData = _timerData.value
        val isCountdown = timerData.isCurrentSessionCountdown()
        val countUpEndTime =
            computeCountUpEndTime(timerData.getBaseTime(timeProvider))

        listeners.forEach {
            it.onEvent(
                Event.SendToBackground(
                    isTimerRunning = timerData.state.isRunning,
                    endTime = if (isCountdown) {
                        timerData.endTime
                    } else {
                        countUpEndTime
                    },
                ),
            )
        }
    }

    fun onBringToForeground() {
        listeners.forEach {
            it.onEvent(Event.BringToForeground)
        }
    }

    private fun computeCountUpEndTime(baseTime: Long) =
        timeProvider.elapsedRealtime() + (COUNT_UP_HARD_LIMIT - baseTime)

    companion object {
        const val WIGGLE_ROOM_MILLIS = 5000L
        val COUNT_UP_HARD_LIMIT = 900.minutes.inWholeMilliseconds
    }
}

enum class FinishActionType {
    MANUAL_RESET,
    MANUAL_SKIP, // increment streak even if session is shorter than 1 minute
    MANUAL_NEXT,
    MANUAL_DO_NOTHING,
    AUTO,
}
