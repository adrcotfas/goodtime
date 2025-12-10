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
import UIKit

@available(iOS 16.1, *)
class GoodtimeLiveActivityManager: ObservableObject {

    static let shared = GoodtimeLiveActivityManager()

    @Published private(set) var currentActivity: Activity<GoodtimeActivityAttributes>?

    private init() {
    }

    // MARK: - Check Availability

    var areActivitiesEnabled: Bool {
        ActivityAuthorizationInfo().areActivitiesEnabled
    }

    // MARK: - Start Activity

    func startActivity(
        timerType: GoodtimeActivityAttributes.TimerType,
        isCountdown: Bool,
        duration: TimeInterval,
        labelName: String,
        isDefaultLabel: Bool
    ) async throws {

        await endAllActivities()

        let now = Date()
        let endDate: Date

        if isCountdown {
            // Countdown: timer counts down from duration to 0
            endDate = now.addingTimeInterval(duration)
        } else {
            // Count-up: timer counts up from 0
            // For unbounded count-up, use distant future
            endDate = Date.distantFuture
        }

        let attributes = GoodtimeActivityAttributes(
            timerType: timerType,
            isCountdown: isCountdown,
            labelName: labelName,
            isDefaultLabel: isDefaultLabel
        )

        let initialState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: now,
            timerEndDate: endDate,
            isPaused: false,
            isRunning: true,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil
        )

        let content = ActivityContent(
            state: initialState,
            staleDate: isCountdown ? endDate.addingTimeInterval(60) : nil
        )

        do {
            let activity = try Activity.request(
                attributes: attributes,
                content: content,
                pushType: nil
            )

            await MainActor.run {
                self.currentActivity = activity
            }

            print("Goodtime: Live Activity started - ID: \(activity.id)")

        } catch {
            print("Goodtime: Failed to start Live Activity - \(error)")
            throw error
        }
    }

    // MARK: - Pause Activity

    func pauseActivity() async {
        guard let activity = currentActivity else {
            print("Goodtime: No active Live Activity to pause")
            return
        }

        let currentState = activity.content.state
        let now = Date()

        var pausedRemaining: TimeInterval? = nil
        var pausedElapsed: TimeInterval? = nil

        if activity.attributes.isCountdown {
            // COUNTDOWN: Store remaining time
            pausedRemaining = currentState.timerEndDate.timeIntervalSince(now)
            if pausedRemaining! < 0 { pausedRemaining = 0 }
        } else {
            // COUNT-UP: Store elapsed time
            pausedElapsed = now.timeIntervalSince(currentState.timerStartDate)
        }

        let updatedState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: currentState.timerStartDate,
            timerEndDate: currentState.timerEndDate,
            isPaused: true,
            isRunning: false,
            pausedTimeRemaining: pausedRemaining,
            pausedElapsedTime: pausedElapsed
        )

        let content = ActivityContent(
            state: updatedState,
            staleDate: nil  // No stale date when paused
        )

        await activity.update(content)

        print("Goodtime: Activity paused")
    }

    // MARK: - Resume Activity

    func resumeActivity() async {
        guard let activity = currentActivity else {
            print("Goodtime: No active Live Activity to resume")
            return
        }

        let currentState = activity.content.state

        // Only resume if actually paused - otherwise we'll mess up a running timer
        guard currentState.isPaused else {
            print("Goodtime: Activity is not paused, skipping resume")
            return
        }

        let now = Date()

        var newStartDate: Date
        var newEndDate: Date

        if activity.attributes.isCountdown {
            // COUNTDOWN: Adjust end date based on remaining time
            let remaining = currentState.pausedTimeRemaining ?? 0
            newStartDate = now
            newEndDate = now.addingTimeInterval(remaining)
        } else {
            // COUNT-UP: Adjust start date backwards based on elapsed time
            let elapsed = currentState.pausedElapsedTime ?? 0
            newStartDate = now.addingTimeInterval(-elapsed)
            newEndDate = Date.distantFuture
        }

        let updatedState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: newStartDate,
            timerEndDate: newEndDate,
            isPaused: false,
            isRunning: true,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil
        )

        let staleDate: Date? = activity.attributes.isCountdown
            ? newEndDate.addingTimeInterval(60)
            : nil

        let content = ActivityContent(
            state: updatedState,
            staleDate: staleDate
        )

        await activity.update(content)

        print("Goodtime: Activity resumed")
    }

    // MARK: - Add One Minute (only for countdown)

    func addOneMinute() async {
        guard let activity = currentActivity else { return }
        guard activity.attributes.isCountdown else {
            print("Goodtime: +1 minute only available for countdown timers")
            return
        }

        let currentState = activity.content.state

        if currentState.isPaused {
            // If paused, add to the remaining time
            let newRemaining = (currentState.pausedTimeRemaining ?? 0) + 60

            let updatedState = GoodtimeActivityAttributes.ContentState(
                timerStartDate: currentState.timerStartDate,
                timerEndDate: currentState.timerEndDate,
                isPaused: true,
                isRunning: false,
                pausedTimeRemaining: newRemaining,
                pausedElapsedTime: nil
            )

            await activity.update(ActivityContent(state: updatedState, staleDate: nil))
            return
        }

        let newEndDate = currentState.timerEndDate.addingTimeInterval(60)

        let updatedState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: currentState.timerStartDate,
            timerEndDate: newEndDate,
            isPaused: false,
            isRunning: true,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil
        )

        let content = ActivityContent(
            state: updatedState,
            staleDate: newEndDate.addingTimeInterval(60)
        )

        await activity.update(content)

        print("Goodtime: Added 1 minute")
    }

    // MARK: - End Activity

    func endActivity() async {
        guard let activity = currentActivity else { return }

        await activity.end(nil, dismissalPolicy: .immediate)

        await MainActor.run {
            self.currentActivity = nil
        }

        print("Goodtime: Activity ended")
    }

    func endAllActivities() async {
        for activity in Activity<GoodtimeActivityAttributes>.activities {
            await activity.end(nil, dismissalPolicy: .immediate)
        }
        await MainActor.run {
            self.currentActivity = nil
        }
    }
}
