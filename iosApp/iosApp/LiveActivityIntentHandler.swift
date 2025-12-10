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
import ComposeApp

/// Handles Live Activity button taps by listening to NotificationCenter
/// and calling the appropriate Kotlin timer manager functions
class LiveActivityIntentHandler {

    static let shared = LiveActivityIntentHandler()

    private init() {
        setupObservers()
    }

    /// Get TimerManager from Koin when needed
    private func getTimerManager() -> TimerManager? {
        let manager = LiveActivityIntentBridge().getTimerManager()
        return manager
    }

    private func setupObservers() {
        // Toggle Pause/Resume
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("GoodtimeTogglePauseFromLiveActivity"),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            print("[LiveActivityIntentHandler] Toggle pause/resume")
            self?.getTimerManager()?.toggle()
        }

        // Stop
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("GoodtimeStopFromLiveActivity"),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            print("[LiveActivityIntentHandler] Stop")
            self?.getTimerManager()?.reset(updateWorkTime: false, actionType: FinishActionType.manualDoNothing)
        }

        // Add One Minute
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("GoodtimeAddMinuteFromLiveActivity"),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            print("[LiveActivityIntentHandler] Add one minute")
            self?.getTimerManager()?.addOneMinute()
        }

        // Start Break / Start Focus (skip to next session)
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("GoodtimeStartBreakFromLiveActivity"),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            print("[LiveActivityIntentHandler] Skip to break")
            self?.getTimerManager()?.skip()
        }

        // Start Focus (skip to next session)
        NotificationCenter.default.addObserver(
            forName: NSNotification.Name("GoodtimeStartFocusFromLiveActivity"),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            print("[LiveActivityIntentHandler] Skip to focus")
            self?.getTimerManager()?.skip()
        }

        print("[LiveActivityIntentHandler] All observers registered")
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}
