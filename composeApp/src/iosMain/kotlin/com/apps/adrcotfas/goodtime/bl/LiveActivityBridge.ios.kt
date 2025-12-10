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

/**
 * Delegate interface that will be implemented by Swift code
 * to handle  Live Activity operations.
 */
interface LiveActivityDelegate {
    fun start(
        isFocus: Boolean,
        isCountdown: Boolean,
        durationSeconds: Double,
        labelName: String,
        isDefaultLabel: Boolean,
    )

    fun pause()
    fun resume()
    fun addOneMinute()
    fun end()
    fun isSupported(): Boolean
}

/**
 * iOS implementation of LiveActivityBridge that delegates to Swift code.
 * The Swift layer should call LiveActivityBridge.setDelegate() to provide the implementation.
 */
class LiveActivityBridge {

    fun start(
        isFocus: Boolean,
        isCountdown: Boolean,
        durationSeconds: Long,
        labelName: String,
        isDefaultLabel: Boolean,
    ) {
        println("[LiveActivityBridge] START called: isFocus=$isFocus, countdown=$isCountdown, duration=$durationSeconds secs")
        delegate?.start(isFocus, isCountdown, durationSeconds.toDouble(), labelName, isDefaultLabel)
            ?: println("[LiveActivityBridge] WARNING: No delegate set!")
    }

    fun pause() {
        println("[LiveActivityBridge] PAUSE called")
        delegate?.pause()
    }

    fun resume() {
        println("[LiveActivityBridge] RESUME called")
        delegate?.resume()
    }

    fun addOneMinute() {
        println("[LiveActivityBridge] ADD_ONE_MINUTE called")
        delegate?.addOneMinute()
    }

    fun end() {
        println("[LiveActivityBridge] END called")
        delegate?.end()
    }

    fun isSupported(): Boolean {
        return delegate?.isSupported() ?: false
    }

    companion object {
        private var delegate: LiveActivityDelegate? = null

        fun setDelegate(del: LiveActivityDelegate) {
            println("[LiveActivityBridge] Delegate set: $del")
            delegate = del
        }
    }
}
