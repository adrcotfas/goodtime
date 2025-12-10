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

@objc public class GoodtimeLiveActivityBridge: NSObject {

    @objc public static let shared = GoodtimeLiveActivityBridge()

    private override init() {
        super.init()
    }

    @objc public func start(
        isFocus: Bool,
        isCountdown: Bool,
        durationSeconds: Double,
        labelName: String,
        isDefaultLabel: Bool
    ) {
        guard #available(iOS 16.1, *) else {
            return
        }

        let timerType: GoodtimeActivityAttributes.TimerType = isFocus ? .focus : .shortBreak

        Task {
            try? await GoodtimeLiveActivityManager.shared.startActivity(
                timerType: timerType,
                isCountdown: isCountdown,
                duration: durationSeconds,
                labelName: labelName,
                isDefaultLabel: isDefaultLabel
            )
        }
    }

    @objc public func pause() {
        guard #available(iOS 16.1, *) else {
            return
        }
        Task {
            await GoodtimeLiveActivityManager.shared.pauseActivity()
        }
    }

    @objc public func resume() {
        guard #available(iOS 16.1, *) else {
            return
        }
        Task {
            await GoodtimeLiveActivityManager.shared.resumeActivity()
        }
    }

    @objc public func addOneMinute() {
        guard #available(iOS 16.1, *) else {
            return
        }
        Task {
            await GoodtimeLiveActivityManager.shared.addOneMinute()
        }
    }

    @objc public func end() {
        guard #available(iOS 16.1, *) else {
            return
        }
        Task {
            await GoodtimeLiveActivityManager.shared.endActivity()
        }
    }

    @objc public func isSupported() -> Bool {
        guard #available(iOS 16.1, *) else {
            return false
        }
        return GoodtimeLiveActivityManager.shared.areActivitiesEnabled
    }
}
