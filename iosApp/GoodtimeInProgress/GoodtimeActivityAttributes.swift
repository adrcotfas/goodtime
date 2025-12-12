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
import SwiftUI

struct GoodtimeActivityAttributes: ActivityAttributes {

    // STATIC properties (don't change during the activity)
    let timerType: TimerType
    let isCountdown: Bool
    let labelName: String
    let isDefaultLabel: Bool
    let labelColorHex: String

    // Localized strings (stored once, used everywhere)
    let strPause: String
    let strResume: String
    let strStop: String
    let strStartFocus: String
    let strStartBreak: String
    let strPlusOneMin: String
    let strFocusInProgress: String
    let strFocusPaused: String
    let strBreakInProgress: String

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

// MARK: - Color Extension

extension Color {
    /// Initialize a Color from a hex string (e.g., "#FF5733" or "FF5733")
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)

        let r, g, b: UInt64
        r = (int >> 16) & 0xFF
        g = (int >> 8) & 0xFF
        b = int & 0xFF

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: 1
        )
    }
}

// MARK: - Preview Helpers

extension GoodtimeActivityAttributes {
    // Preview Attributes
    static var previewFocusCountdown: GoodtimeActivityAttributes {
        GoodtimeActivityAttributes(
            timerType: .focus,
            isCountdown: true,
            labelName: "SomeLongLabelNameHere",
            isDefaultLabel: false,
            labelColorHex: "#5399d0",
            strPause: "Pause",
            strResume: "Resume",
            strStop: "Stop",
            strStartFocus: "Start Focus",
            strStartBreak: "Start Break",
            strPlusOneMin: "+1 Min",
            strFocusInProgress: "Focus in progress",
            strFocusPaused: "Focus paused",
            strBreakInProgress: "Break in progress"
        )
    }

    static var previewFocusCountUp: GoodtimeActivityAttributes {
        GoodtimeActivityAttributes(
            timerType: .focus,
            isCountdown: false,
            labelName: "Deep Work",
            isDefaultLabel: false,
            labelColorHex: "#4e9364",
            strPause: "Pause",
            strResume: "Resume",
            strStop: "Stop",
            strStartFocus: "Start Focus",
            strStartBreak: "Start Break",
            strPlusOneMin: "+1 Min",
            strFocusInProgress: "Focus in progress",
            strFocusPaused: "Focus paused",
            strBreakInProgress: "Break in progress"
        )
    }

    static var previewShortBreak: GoodtimeActivityAttributes {
        GoodtimeActivityAttributes(
            timerType: .shortBreak,
            isCountdown: true,
            labelName: "Break",
            isDefaultLabel: true,
            labelColorHex: "",
            strPause: "Pause",
            strResume: "Resume",
            strStop: "Stop",
            strStartFocus: "Start Focus",
            strStartBreak: "Start Break",
            strPlusOneMin: "+1 Min",
            strFocusInProgress: "Focus in progress",
            strFocusPaused: "Focus paused",
            strBreakInProgress: "Break in progress"
        )
    }

    static var previewLongBreak: GoodtimeActivityAttributes {
        GoodtimeActivityAttributes(
            timerType: .longBreak,
            isCountdown: true,
            labelName: "Long Break",
            isDefaultLabel: true,
            labelColorHex: "",
            strPause: "Pause",
            strResume: "Resume",
            strStop: "Stop",
            strStartFocus: "Start Focus",
            strStartBreak: "Start Break",
            strPlusOneMin: "+1 Min",
            strFocusInProgress: "Focus in progress",
            strFocusPaused: "Focus paused",
            strBreakInProgress: "Break in progress"
        )
    }
}

extension GoodtimeActivityAttributes.ContentState {
    // Preview Content States

    /// Running countdown timer (25 minutes remaining)
    static var previewRunning: GoodtimeActivityAttributes.ContentState {
        let now = Date()
        let endDate = now.addingTimeInterval(25 * 60) // 25 minutes from now
        return GoodtimeActivityAttributes.ContentState(
            timerStartDate: now,
            timerEndDate: endDate,
            isPaused: false,
            isRunning: true,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil
        )
    }

    /// Paused countdown timer (15 minutes remaining)
    static var previewPaused: GoodtimeActivityAttributes.ContentState {
        let now = Date()
        let endDate = now.addingTimeInterval(15 * 60) // Arbitrary end date
        return GoodtimeActivityAttributes.ContentState(
            timerStartDate: now,
            timerEndDate: endDate,
            isPaused: true,
            isRunning: false,
            pausedTimeRemaining: 15 * 60, // 15 minutes
            pausedElapsedTime: nil
        )
    }

    /// Running count-up timer (started 10 minutes ago)
    static var previewCountUpRunning: GoodtimeActivityAttributes.ContentState {
        let startDate = Date().addingTimeInterval(-10 * 60) // Started 10 minutes ago
        let endDate = Date().addingTimeInterval(1000 * 60) // Far future (not used for count-up)
        return GoodtimeActivityAttributes.ContentState(
            timerStartDate: startDate,
            timerEndDate: endDate,
            isPaused: false,
            isRunning: true,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil
        )
    }

    /// Paused count-up timer (5 minutes elapsed)
    static var previewCountUpPaused: GoodtimeActivityAttributes.ContentState {
        let startDate = Date().addingTimeInterval(-5 * 60) // Started 5 minutes ago
        let endDate = Date().addingTimeInterval(1000 * 60)
        return GoodtimeActivityAttributes.ContentState(
            timerStartDate: startDate,
            timerEndDate: endDate,
            isPaused: true,
            isRunning: false,
            pausedTimeRemaining: nil,
            pausedElapsedTime: 5 * 60 // 5 minutes elapsed
        )
    }
}
