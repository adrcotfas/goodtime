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
final class LiveActivityIntentHandler {

    static let shared = LiveActivityIntentHandler()

    // MARK: - Notification Names

    private enum NotificationName {
        static let togglePause = NSNotification.Name("GoodtimeTogglePauseFromLiveActivity")
        static let stop = NSNotification.Name("GoodtimeStopFromLiveActivity")
        static let addMinute = NSNotification.Name("GoodtimeAddMinuteFromLiveActivity")
        static let startBreak = NSNotification.Name("GoodtimeStartBreakFromLiveActivity")
        static let startFocus = NSNotification.Name("GoodtimeStartFocusFromLiveActivity")
    }

    // MARK: - Initialization

    private init() {
        setupObservers()
    }

    // MARK: - Timer Manager

    private var timerManager: TimerManager? {
        LiveActivityIntentBridge().getTimerManager()
    }

    private var liveActivityManager: GoodtimeLiveActivityManager {
        .shared
    }

    // MARK: - Expired Activity Handling

    private func handleExpiredActivity() {
        print("[LiveActivityIntentHandler] Timer expired - updating to stale state")
        liveActivityManager.updateExpiredActivityToStale()
    }

    // MARK: - Action Handlers

    private func handleTogglePause() {
        print("[LiveActivityIntentHandler] Toggle pause/resume")

        guard !liveActivityManager.isActivityExpired() else {
            handleExpiredActivity()
            return
        }

        timerManager?.toggle()
    }

    private func handleStop() {
        print("[LiveActivityIntentHandler] Stop")
        if liveActivityManager.isActivityExpired() {
            handleExpiredActivity()
        }
        timerManager?.reset(actionType: .manualDoNothing)
    }

    private func handleAddMinute() {
        print("[LiveActivityIntentHandler] Add one minute")

        guard !liveActivityManager.isActivityExpired() else {
            handleExpiredActivity()
            return
        }

        timerManager?.addOneMinute()
    }

    private func handleSkip(action: String) {
        print("[LiveActivityIntentHandler] \(action)")

        if liveActivityManager.isActivityExpired() {
            timerManager?.finish(actionType: .forceFinish)
            timerManager?.next(actionType: .manualNext)
        } else {
            timerManager?.skip()
        }
    }

    // MARK: - Observer Setup

    private func setupObservers() {
        let center = NotificationCenter.default

        center.addObserver(
            forName: NotificationName.togglePause,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleTogglePause()
        }

        center.addObserver(
            forName: NotificationName.stop,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleStop()
        }

        center.addObserver(
            forName: NotificationName.addMinute,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleAddMinute()
        }

        center.addObserver(
            forName: NotificationName.startBreak,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleSkip(action: "Skip to break")
        }

        center.addObserver(
            forName: NotificationName.startFocus,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleSkip(action: "Skip to focus")
        }

        print("[LiveActivityIntentHandler] All observers registered")
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}
