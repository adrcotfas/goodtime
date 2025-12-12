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

/**
 * iOS implementation that calls a delegate (Swift GoodtimeLiveActivityManager)
 * to manage ActivityKit Live Activities.
 */
class LiveActivityBridge {
    private var delegate: LiveActivityDelegate? = null

    fun setDelegate(delegate: LiveActivityDelegate) {
        this.delegate = delegate
    }

    fun start(
        isFocus: Boolean,
        isCountdown: Boolean,
        durationSeconds: Long,
        labelName: String,
        isDefaultLabel: Boolean,
        labelColorHex: String,
        localizedStrings: Map<String, String>,
    ) {
        Logger.v(TAG) { "START: isFocus=$isFocus, countdown=$isCountdown, duration=$durationSeconds secs, color=$labelColorHex" }

        val timerType = if (isFocus) 0 else 1 // 0=focus, 1=shortBreak

        delegate?.startActivity(
            timerType = timerType,
            isCountdown = isCountdown,
            duration = durationSeconds.toDouble(),
            labelName = labelName,
            isDefaultLabel = isDefaultLabel,
            labelColorHex = labelColorHex,
            localizedStrings = localizedStrings,
        )
    }

    fun pause() {
        Logger.v(TAG) { "[LiveActivityBridge] PAUSE" }
        delegate?.pauseActivity()
    }

    fun resume() {
        Logger.v(TAG) { "[LiveActivityBridge] RESUME" }
        delegate?.resumeActivity()
    }

    fun addOneMinute() {
        Logger.v(TAG) { "[LiveActivityBridge] ADD ONE MINUTE" }
        delegate?.addOneMinute()
    }

    fun end() {
        Logger.v(TAG) { "[LiveActivityBridge] END" }
        delegate?.endActivity()
    }

    fun isSupported(): Boolean = delegate?.areActivitiesEnabled() ?: false

    companion object {
        val shared = LiveActivityBridge()
        private const val TAG = "LiveActivityBridge"
    }
}

/**
 * Protocol that Swift GoodtimeLiveActivityManager implements directly.
 * No separate delegate class needed.
 */
interface LiveActivityDelegate {
    fun startActivity(
        timerType: Int,
        isCountdown: Boolean,
        duration: Double,
        labelName: String,
        isDefaultLabel: Boolean,
        labelColorHex: String,
        localizedStrings: Map<String, String>,
    )

    fun pauseActivity()

    fun resumeActivity()

    fun addOneMinute()

    fun endActivity()

    fun areActivitiesEnabled(): Boolean
}
