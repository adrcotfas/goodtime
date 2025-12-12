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

import AppIntents
import Foundation
import UserNotifications

// MARK: - Pause/Resume Intent (Toggle)

struct GoodtimeTogglePauseIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Toggle Pause"
    static var description = IntentDescription("Pauses or resumes the timer")

    func perform() async throws -> some IntentResult {
        print("[Intent] Toggle Pause/Resume")

        // Clear all delivered notifications
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()

        // Post notification to trigger toggle in the app
        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeTogglePauseFromLiveActivity"),
            object: nil
        )
        return .result()
    }
}

// MARK: - Stop Intent

struct GoodtimeStopIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Stop Timer"
    static var description = IntentDescription("Stops the current timer")

    func perform() async throws -> some IntentResult {
        print("[Intent] Stop timer")

        // Clear all delivered notifications
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()

        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeStopFromLiveActivity"),
            object: nil
        )
        return .result()
    }
}

// MARK: - Add One Minute Intent

struct GoodtimeAddMinuteIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Add 1 Minute"
    static var description = IntentDescription("Adds one minute to the timer")

    func perform() async throws -> some IntentResult {
        print("[Intent] Add one minute")

        // Clear all delivered notifications
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()

        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeAddMinuteFromLiveActivity"),
            object: nil
        )
        return .result()
    }
}

// MARK: - Start Break Intent

struct GoodtimeStartBreakIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Start Break"
    static var description = IntentDescription("Starts a break session")

    func perform() async throws -> some IntentResult {
        print("[Intent] Start break")

        // Clear all delivered notifications
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()

        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeStartBreakFromLiveActivity"),
            object: nil
        )
        return .result()
    }
}

// MARK: - Start Focus Intent

struct GoodtimeStartFocusIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Start Focus"
    static var description = IntentDescription("Starts a focus session")

    func perform() async throws -> some IntentResult {
        print("[Intent] Start focus")

        // Clear all delivered notifications
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()

        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeStartFocusFromLiveActivity"),
            object: nil
        )
        return .result()
    }
}
