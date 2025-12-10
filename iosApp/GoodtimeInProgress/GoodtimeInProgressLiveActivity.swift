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

import ActivityKit
import WidgetKit
import SwiftUI
import AppIntents

struct GoodtimeLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: GoodtimeActivityAttributes.self) { context in
            // LOCK SCREEN / BANNER VIEW
            GoodtimeLockScreenView(context: context)
                .activityBackgroundTint(backgroundTint(for: context.attributes.timerType))
                .activitySystemActionForegroundColor(.white)

        } dynamicIsland: { context in
            DynamicIsland {
                // EXPANDED VIEW (long-press)
                DynamicIslandExpandedRegion(.leading) {
                    timerTypeIcon(context.attributes.timerType)
                        .font(.title2)
                        .opacity(context.isStale ? 0.5 : 1.0)
                }

                DynamicIslandExpandedRegion(.trailing) {
                    GoodtimeTimerDisplay(context: context, style: .expanded)
                }

                DynamicIslandExpandedRegion(.center) {
                    if context.isStale {
                        VStack(spacing: 2) {
                            Text("Session Ended")
                                .font(.headline)
                                .fontWeight(.semibold)

                            Text("Open app")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    } else {
                        VStack(spacing: 2) {
                            Text(timerTypeLabel(context.attributes.timerType))
                                .font(.headline)
                                .fontWeight(.semibold)

                            if !context.attributes.isCountdown {
                                Text("Count Up")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }

                DynamicIslandExpandedRegion(.bottom) {
                    if !context.isStale {
                        VStack(spacing: 8) {
                            Text(stateText(for: context))
                                .font(.caption)
                                .foregroundColor(.secondary)

                            // Action buttons
                            GoodtimeActionButtons(context: context)
                        }
                    }
                }

            } compactLeading: {
                // COMPACT LEFT
                timerTypeIcon(context.attributes.timerType)
                    .font(.caption)

            } compactTrailing: {
                // COMPACT RIGHT - Timer display
                GoodtimeTimerDisplay(context: context, style: .compact)
                    .frame(width: 50)

            } minimal: {
                // MINIMAL (when multiple activities)
                timerTypeIcon(context.attributes.timerType)
                    .font(.caption2)
            }
            .keylineTint(keylineTint(for: context.attributes.timerType))
        }
    }

    // MARK: - Helper Functions

    private func timerTypeIcon(_ type: GoodtimeActivityAttributes.TimerType) -> some View {
        Image(systemName: iconName(for: type))
    }

    private func iconName(for type: GoodtimeActivityAttributes.TimerType) -> String {
        switch type {
        case .focus: return "brain.head.profile"
        case .shortBreak, .longBreak: return "cup.and.saucer.fill"
        }
    }

    private func timerTypeLabel(_ type: GoodtimeActivityAttributes.TimerType) -> String {
        switch type {
        case .focus: return "Focus"
        case .shortBreak: return "Short Break"
        case .longBreak: return "Long Break"
        }
    }

    private func stateText(for context: ActivityViewContext<GoodtimeActivityAttributes>) -> String {
        let type = context.attributes.timerType
        let isPaused = context.state.isPaused

        if type == .focus {
            return isPaused ? "Focus session paused" : "Focus session in progress"
        } else {
            return "Break in progress"
        }
    }

    private func backgroundTint(for type: GoodtimeActivityAttributes.TimerType) -> Color {
        switch type {
        case .focus: return .indigo
        case .shortBreak, .longBreak: return .green
        }
    }

    private func keylineTint(for type: GoodtimeActivityAttributes.TimerType) -> Color {
        backgroundTint(for: type)
    }
}

// MARK: - Timer Display Component (Handles Countdown AND Count-Up)

struct GoodtimeTimerDisplay: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>
    let style: DisplayStyle

    enum DisplayStyle {
        case compact
        case expanded
        case lockScreen
    }

    var body: some View {
        Group {
            if context.isStale {
                // STALE: Show placeholder
                Text("--:--")
                    .foregroundColor(.gray)
            } else if context.state.isPaused {
                // PAUSED: Show static time
                pausedTimeText
                    .foregroundColor(.yellow)
            } else {
                // RUNNING: Auto-updating timer
                Text(
                    timerInterval: context.state.timerStartDate...context.state.timerEndDate,
                    countsDown: context.attributes.isCountdown
                )
            }
        }
        .monospacedDigit()
        .multilineTextAlignment(.trailing)
        .font(fontForStyle)
    }

    @ViewBuilder
    private var pausedTimeText: some View {
        let time = context.state.displayTime
        Text(formatTime(time))
    }

    private var fontForStyle: Font {
        switch style {
        case .compact: return .caption2
        case .expanded: return .title2
        case .lockScreen: return .title
        }
    }

    private func formatTime(_ seconds: TimeInterval) -> String {
        let totalSeconds = Int(abs(seconds))
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let secs = totalSeconds % 60

        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, secs)
        } else {
            return String(format: "%02d:%02d", minutes, secs)
        }
    }
}

// MARK: - Lock Screen View

struct GoodtimeLockScreenView: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>

    var body: some View {
        if context.isStale {
            // STALE STATE UI
            VStack(spacing: 12) {
                HStack(spacing: 16) {
                    Image(systemName: iconName)
                        .font(.largeTitle)
                        .foregroundColor(.white.opacity(0.6))

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Session Ended")
                            .font(.headline)
                            .foregroundColor(.white)

                        Text("Open app to continue")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))
                    }

                    Spacer()
                }
            }
            .padding()
        } else {
            // ACTIVE STATE UI
            VStack(spacing: 12) {
                HStack(spacing: 16) {
                    // Left: Timer type icon
                    Image(systemName: iconName)
                        .font(.largeTitle)
                        .foregroundColor(.white)

                    // Center: Info
                    VStack(alignment: .leading, spacing: 4) {
                        Text(timerLabel)
                            .font(.headline)
                            .foregroundColor(.white)

                        // Show label name if not default
                        if !context.attributes.isDefaultLabel {
                            Text(context.attributes.labelName)
                                .font(.caption)
                                .foregroundColor(.white.opacity(0.8))
                        }

                        HStack(spacing: 8) {
                            if !context.attributes.isCountdown {
                                Text("↑")
                                    .font(.caption)
                                    .padding(.horizontal, 4)
                                    .background(Color.white.opacity(0.2))
                                    .cornerRadius(4)
                            }

                            if context.state.isPaused {
                                Text("Paused")
                                    .font(.caption)
                                    .foregroundColor(.yellow)
                            }
                        }
                    }

                    Spacer()

                    // Right: Timer
                    GoodtimeTimerDisplay(context: context, style: .lockScreen)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                }

                // Action buttons at bottom
                GoodtimeActionButtons(context: context, style: .lockScreen)
            }
            .padding()
        }
    }

    private var iconName: String {
        switch context.attributes.timerType {
        case .focus: return "brain.head.profile"
        case .shortBreak, .longBreak: return "cup.and.saucer.fill"
        }
    }

    private var timerLabel: String {
        let type = context.attributes.timerType
        let isPaused = context.state.isPaused

        if type == .focus {
            return isPaused ? "Focus session paused" : "Focus session in progress"
        } else {
            return "Break in progress"
        }
    }
}

// MARK: - Action Buttons Component

struct GoodtimeActionButtons: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>
    var style: ButtonStyle = .dynamicIsland

    enum ButtonStyle {
        case dynamicIsland
        case lockScreen
    }

    var body: some View {
        // Hide buttons when stale
        if context.isStale {
            EmptyView()
        } else {
            let timerType = context.attributes.timerType
            let isPaused = context.state.isPaused
            let isCountdown = context.attributes.isCountdown

            HStack(spacing: style == .lockScreen ? 8 : 6) {
                if isCountdown {
                    // COUNTDOWN MODE
                    if timerType == .focus {
                        // FOCUS SESSION
                        if isPaused {
                            // Paused Focus: Resume, Stop, Start Break
                            actionButton("play.fill", intent: GoodtimeTogglePauseIntent())
                            actionButton("stop.fill", intent: GoodtimeStopIntent())
                            actionButton("cup.and.saucer.fill", intent: GoodtimeStartBreakIntent())
                        } else {
                            // Running Focus: Pause, +1 Min, Start Break
                            actionButton("pause.fill", intent: GoodtimeTogglePauseIntent())
                            actionButton("plus", intent: GoodtimeAddMinuteIntent())
                            actionButton("cup.and.saucer.fill", intent: GoodtimeStartBreakIntent())
                        }
                    } else {
                        // BREAK SESSION: Stop, +1 Min, Start Focus
                        actionButton("stop.fill", intent: GoodtimeStopIntent())
                        actionButton("plus", intent: GoodtimeAddMinuteIntent())
                        actionButton("brain.head.profile", intent: GoodtimeStartFocusIntent())
                    }
                } else {
                    // COUNT-UP MODE: Just Stop
                    actionButton("stop.fill", intent: GoodtimeStopIntent())
                }
            }
        }
    }

    @ViewBuilder
    private func actionButton<Intent: AppIntent>(_ systemImage: String, intent: Intent) -> some View {
        Button(intent: intent) {
            Image(systemName: systemImage)
                .font(style == .lockScreen ? .body : .caption)
                .foregroundColor(.white)
                .frame(width: style == .lockScreen ? 50 : 36, height: style == .lockScreen ? 36 : 28)
                .background(Color.white.opacity(0.2))
                .clipShape(RoundedRectangle(cornerRadius: style == .lockScreen ? 10 : 8))
        }
        .buttonStyle(.plain)
    }
}
