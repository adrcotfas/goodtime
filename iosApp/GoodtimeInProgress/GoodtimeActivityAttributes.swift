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

struct GoodtimeActivityAttributes: ActivityAttributes {

    // STATIC properties (don't change during the activity)
    let timerType: TimerType
    let isCountdown: Bool
    let labelName: String
    let isDefaultLabel: Bool

    // ContentState contains DYNAMIC properties (change during activity)
    struct ContentState: Codable, Hashable {
        // Timer interval boundaries
        let timerStartDate: Date
        let timerEndDate: Date

        // State flags
        let isPaused: Bool
        let isRunning: Bool

        // Paused state storage (different for countdown vs count-up)
        let pausedTimeRemaining: TimeInterval?  // For countdown pause
        let pausedElapsedTime: TimeInterval?    // For count-up pause
    }

    enum TimerType: String, Codable {
        case focus
        case shortBreak
        case longBreak
    }
}

// MARK: - Convenience Extensions

extension GoodtimeActivityAttributes.ContentState {
    /// For display purposes when paused
    var displayTime: TimeInterval {
        if let remaining = pausedTimeRemaining {
            return remaining
        }
        if let elapsed = pausedElapsedTime {
            return elapsed
        }
        return 0
    }
}
