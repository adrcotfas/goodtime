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
import ComposeApp

@available(iOS 16.1, *)
class GoodtimeLiveActivityManager: NSObject, ObservableObject, LiveActivityDelegate {

    static let shared = GoodtimeLiveActivityManager()

    @Published private(set) var currentActivity: Activity<GoodtimeActivityAttributes>?

    private override init() {
        super.init()
    }

    // MARK: - Check Availability

    func areActivitiesEnabled() -> Bool {
        return ActivityAuthorizationInfo().areActivitiesEnabled
    }

    // MARK: - Start Activity

    func startActivity(
        timerType: Int32,
        isCountdown: Bool,
        duration: Double,
        labelName: String,
        isDefaultLabel: Bool,
        labelColorHex: String,
        localizedStrings: [String: String]
    ) {
        Task {
            try? await startActivityAsync(
                timerType: timerType == 0 ? .focus : .shortBreak,
                isCountdown: isCountdown,
                duration: duration,
                labelName: labelName,
                isDefaultLabel: isDefaultLabel,
                labelColorHex: labelColorHex,
                localizedStrings: localizedStrings
            )
        }
    }

    private func startActivityAsync(
        timerType: GoodtimeActivityAttributes.TimerType,
        isCountdown: Bool,
        duration: TimeInterval,
        labelName: String,
        isDefaultLabel: Bool,
        labelColorHex: String,
        localizedStrings: [String: String]
    ) async throws {

        await endAllActivities()

        let now = Date()
        let endDate: Date

        if isCountdown {
            // Countdown: timer counts down from duration to 0
            endDate = now.addingTimeInterval(duration)
            print("Goodtime: Starting COUNTDOWN - duration: \(duration)s, end: \(endDate)")
        } else {
            // Count-up: timer counts up from 0
            // Use 8 hours (Apple's recommended Live Activity limit)
            endDate = now.addingTimeInterval(28800) // 8 hours
            print("Goodtime: Starting COUNT-UP - start: \(now), end: \(endDate)")
        }

        // Extract localized strings from dictionary with fallbacks
        let strPause = localizedStrings["pause"] ?? "Pause"
        let strResume = localizedStrings["resume"] ?? "Resume"
        let strStop = localizedStrings["stop"] ?? "Stop"
        let strStartFocus = localizedStrings["start_focus"] ?? "Start Focus"
        let strStartBreak = localizedStrings["start_break"] ?? "Start Break"
        let strPlusOneMin = localizedStrings["plus_one_min"] ?? "+1 Min"
        let strFocusInProgress = localizedStrings["focus_in_progress"] ?? "Focus in progress"
        let strFocusPaused = localizedStrings["focus_paused"] ?? "Focus paused"
        let strBreakInProgress = localizedStrings["break_in_progress"] ?? "Break in progress"

        let attributes = GoodtimeActivityAttributes(
            timerType: timerType,
            isCountdown: isCountdown,
            labelName: labelName,
            isDefaultLabel: isDefaultLabel,
            labelColorHex: labelColorHex,
            strPause: strPause,
            strResume: strResume,
            strStop: strStop,
            strStartFocus: strStartFocus,
            strStartBreak: strStartBreak,
            strPlusOneMin: strPlusOneMin,
            strFocusInProgress: strFocusInProgress,
            strFocusPaused: strFocusPaused,
            strBreakInProgress: strBreakInProgress
        )

        let initialState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: now,
            timerEndDate: endDate,
            isPaused: false,
            isRunning: true,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil
        )

        print("Goodtime: Activity attributes - isCountdown: \(isCountdown), timerType: \(timerType)")

        let content = ActivityContent(
            state: initialState,
            staleDate: isCountdown ? endDate : nil
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

    func pauseActivity() {
        Task {
            await pauseActivityAsync()
        }
    }

    private func pauseActivityAsync() async {
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

    func resumeActivity() {
        Task {
            await resumeActivityAsync()
        }
    }

    private func resumeActivityAsync() async {
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
            // Use 8 hours (Apple's recommended Live Activity limit) from adjusted start
            newEndDate = newStartDate.addingTimeInterval(28800)
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
            ? newEndDate
            : nil

        let content = ActivityContent(
            state: updatedState,
            staleDate: staleDate
        )

        await activity.update(content)

        print("Goodtime: Activity resumed")
    }

    // MARK: - Add One Minute (only for countdown)

    func addOneMinute() {
        Task {
            await addOneMinuteAsync()
        }
    }

    private func addOneMinuteAsync() async {
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

        let newEndDate = currentState.timerEndDate

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
            staleDate: newEndDate
        )

        await activity.update(content)

        print("Goodtime: Added 1 minute")
    }

    // MARK: - End Activity

    func endActivity() {
        Task {
            await endActivityAsync()
        }
    }

    private func endActivityAsync() async {
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
