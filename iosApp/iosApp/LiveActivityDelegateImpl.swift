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

import Foundation
import ActivityKit
import ComposeApp

// Swift implementation of the Kotlin LiveActivityDelegate interface
class LiveActivityDelegateImpl: LiveActivityDelegate {

    func start(
        isFocus: Bool,
        isCountdown: Bool,
        durationSeconds: Double,
        labelName: String,
        isDefaultLabel: Bool
    ) {
        print("[LiveActivityDelegateImpl] start called - isFocus: \(isFocus), countdown: \(isCountdown), duration: \(durationSeconds)")
        GoodtimeLiveActivityBridge.shared.start(
            isFocus: isFocus,
            isCountdown: isCountdown,
            durationSeconds: durationSeconds,
            labelName: labelName,
            isDefaultLabel: isDefaultLabel
        )
    }

    func pause() {
        print("[LiveActivityDelegateImpl] pause called")
        GoodtimeLiveActivityBridge.shared.pause()
    }

    func resume() {
        print("[LiveActivityDelegateImpl] resume called")
        GoodtimeLiveActivityBridge.shared.resume()
    }

    func addOneMinute() {
        print("[LiveActivityDelegateImpl] addOneMinute called")
        GoodtimeLiveActivityBridge.shared.addOneMinute()
    }

    func end() {
        print("[LiveActivityDelegateImpl] end called")
        GoodtimeLiveActivityBridge.shared.end()
    }

    func isSupported() -> Bool {
        let supported = GoodtimeLiveActivityBridge.shared.isSupported()
        print("[LiveActivityDelegateImpl] isSupported: \(supported)")
        return supported
    }
}
