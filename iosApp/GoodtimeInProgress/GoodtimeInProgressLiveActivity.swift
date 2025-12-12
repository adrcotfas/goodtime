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
                // EXPANDED VIEW (long-press) - Similar to lock screen layout
                DynamicIslandExpandedRegion(.leading) {
                    Image("product_icon")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 24, height: 24)
                        .cornerRadius(4).padding(4)
                }

                DynamicIslandExpandedRegion(.trailing) {
                    GoodtimeTimerDisplay(context: context, style: .expanded)
                        .padding(4)
                }

                DynamicIslandExpandedRegion(.center) {
                    VStack(alignment: .leading) {
                        if context.isStale {
                            // STALE STATE UI
                            VStack(alignment: .leading, spacing: 4) {
                                GoodtimeStatusText(
                                    context: context,
                                    font: .headline,
                                    foregroundColor: .white
                                )
                            }
                            .padding()
                        } else {
                            // ACTIVE STATE UI
                            VStack(alignment: .leading) {
                                GoodtimeStatusText(
                                    context: context,
                                    font: .body,
                                    foregroundColor: .white
                                )
                                if !context.attributes.isDefaultLabel && !context.attributes.labelColorHex.isEmpty {
                                    GoodtimeLabelBadge(
                                        labelName: context.attributes.labelName,
                                        labelColorHex: context.attributes.labelColorHex,
                                        fontSize: .caption,
                                        foregroundColor: .white
                                    )
                                }
                            }
                        }
                    }
                }

                DynamicIslandExpandedRegion(.bottom) {
                    if !context.isStale {
                        // Action buttons
                        GoodtimeActionButtons(context: context)
                            .padding(.top, 4)
                    }
                }

            } compactLeading: {
                // COMPACT LEFT
                timerTypeIcon(context.attributes.timerType)
                    

            } compactTrailing: {
                // COMPACT RIGHT - Timer display
                GoodtimeTimerDisplay(context: context, style: .compact)
                    .frame(width: 50)

            } minimal: {
                timerTypeIcon(context.attributes.timerType)
                    
            }
            .keylineTint(keylineTint(for: context.attributes.timerType))
        }
    }

    // MARK: - Helper Functions

    private func timerTypeIcon(_ type: GoodtimeActivityAttributes.TimerType) -> some View {
        Image(iconName(for: type))
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 20, height: 20)
            .foregroundColor(.white)
    }

    private func iconName(for type: GoodtimeActivityAttributes.TimerType) -> String {
        switch type {
        case .focus: return "ic_focus"
        case .shortBreak, .longBreak: return "ic_break"
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
            return isPaused ? context.attributes.strFocusPaused : context.attributes.strFocusInProgress
        } else {
            return context.attributes.strBreakInProgress
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
    }

    var body: some View {
        Group {
            if context.isStale {
                
            } else if context.state.isPaused {
                pausedTimeText
                    .opacity(0.35)
            } else {
                // RUNNING: Auto-updating timer
                Text(
                    timerInterval: context.state.timerStartDate...context.state.timerEndDate,
                    countsDown: context.attributes.isCountdown
                )
            }
        }
        .monospacedDigit()
        .multilineTextAlignment(textAlignment)
        .font(fontForStyle)
        .foregroundColor(.white)
    }

    @ViewBuilder
    private var pausedTimeText: some View {
        let time = context.state.displayTime
        Text(formatTime(time))
    }

    private var fontForStyle: Font {
        switch style {
        case .compact:
            return .caption2
        case .expanded:
            // Use caption2 if duration is longer than 60 minutes (shows hours)
            let duration = context.state.timerEndDate.timeIntervalSince(context.state.timerStartDate)
            return abs(duration) > 3600 ? .caption2 : .title3
        }
    }

    private var textAlignment: TextAlignment {
        switch style {
        case .compact: return .trailing
        case .expanded: return .trailing
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
            return String(format: "%d:%02d", minutes, secs)
        }
    }
}

// MARK: - Status Text Component (Reusable)

struct GoodtimeStatusText: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>
    var font: Font = .body
    var foregroundColor: Color = .white

    var body: some View {
        Text(statusText)
            .font(font)
            .foregroundColor(foregroundColor)
    }

    private var statusText: String {
        if context.isStale {
            return context.attributes.timerType == .focus
                ? context.attributes.strFocusComplete
                : context.attributes.strBreakComplete
        }

        let type = context.attributes.timerType
        let isPaused = context.state.isPaused

        if type == .focus {
            return isPaused ? context.attributes.strFocusPaused : context.attributes.strFocusInProgress
        } else {
            return context.attributes.strBreakInProgress
        }
    }
}

// MARK: - Label Badge Component (Reusable)

struct GoodtimeLabelBadge: View {
    let labelName: String
    let labelColorHex: String
    var fontSize: Font = .caption
    var foregroundColor: Color = .white

    var body: some View {
        HStack(spacing: 6) {
            Image("ic_label")
                .resizable()
                .renderingMode(.template)
                .aspectRatio(contentMode: .fit)
                .frame(width: 12, height: 12)
                .foregroundColor(Color(hex: labelColorHex))
            Text(labelName)
                .font(fontSize)
                .foregroundColor(foregroundColor)
        }
    }
}

// MARK: - Lock Screen View

struct GoodtimeLockScreenView: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>

    var body: some View {
        Group {
            VStack(spacing: 12) {
                HStack() {
                    HStack(spacing: 6) {
                        Image("product_icon")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 24, height: 24)
                            .cornerRadius(4)
                        Text("Goodtime")
                            .font(.caption)
                            .foregroundColor(.white)
                    }
                    Spacer()
                    GoodtimeTimerDisplay(context: context, style: .expanded)
                }
                if context.isStale {
                    // STALE STATE UI
                    VStack(spacing: 12) {
                        HStack(spacing: 16) {
                            VStack(alignment: .leading, spacing: 4) {
                                GoodtimeStatusText(
                                    context: context,
                                    font: .headline,
                                    foregroundColor: .white
                                )
                            }
                        }
                    }
                    .padding()
                } else {
                    // ACTIVE STATE UI
                    VStack(alignment: .leading) {
                        GoodtimeStatusText(
                            context: context,
                            font: .body,
                            foregroundColor: .white
                        )
                        .lineLimit(1)
                        if !context.attributes.isDefaultLabel && !context.attributes.labelColorHex.isEmpty {
                            GoodtimeLabelBadge(
                                labelName: context.attributes.labelName,
                                labelColorHex: context.attributes.labelColorHex,
                                fontSize: .caption,
                                foregroundColor: .white
                            )
                        }
                        GoodtimeActionButtons(context: context, style: .dynamicIsland).padding(.top, 16)
                    }
                }
            }
            .padding()
        }
        .background(Color.black)  // Always black background for lock screen
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
                //TODO: add isStale branch with options equivalent to NotificationArchManager.buildFinishedNotification actions
                if isCountdown {
                    // COUNTDOWN MODE
                    if timerType == .focus {
                        // FOCUS SESSION
                        if isPaused {
                            // Paused Focus: Resume, Stop, Start Break
                            textButton(context.attributes.strResume, intent: GoodtimeTogglePauseIntent())
                            textButton(context.attributes.strStop, intent: GoodtimeStopIntent())
                            textButton(context.attributes.strStartBreak, intent: GoodtimeStartBreakIntent())
                        } else {
                            // Running Focus: Pause, +1 Min, Start Break
                            textButton(context.attributes.strPause, intent: GoodtimeTogglePauseIntent())
                            textButton(context.attributes.strPlusOneMin, intent: GoodtimeAddMinuteIntent())
                            textButton(context.attributes.strStartBreak, intent: GoodtimeStartBreakIntent())
                        }
                    } else {
                        // BREAK SESSION: Stop, +1 Min, Start Focus
                        textButton(context.attributes.strStop, intent: GoodtimeStopIntent())
                        textButton(context.attributes.strPlusOneMin, intent: GoodtimeAddMinuteIntent())
                        textButton(context.attributes.strStartFocus, intent: GoodtimeStartFocusIntent())
                    }
                } else {
                    // COUNT-UP MODE: Just Stop
                    textButton(context.attributes.strStop, intent: GoodtimeStopIntent())
                }
            }
        }
    }

    @ViewBuilder
    private func textButton<Intent: AppIntent>(_ title: String, intent: Intent) -> some View {
        Button(intent: intent) {
            Text(title)
                .font(style == .lockScreen ? .caption : .caption)
                .fontWeight(.medium)
                .foregroundColor(.white)
                .padding(.horizontal, style == .lockScreen ? 16 : 12)
                .padding(.vertical, style == .lockScreen ? 10 : 6)
                .background(Color.white.opacity(0.2))
                .clipShape(RoundedRectangle(cornerRadius: style == .lockScreen ? 10 : 8))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Previews

struct GoodtimeLiveActivity_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            // Live Activity - Focus Countdown
            GoodtimeActivityAttributes.previewFocusCountdown
                .previewContext(
                    GoodtimeActivityAttributes.ContentState.previewRunning,
                    viewKind: .content
                )
                .previewDisplayName("Live Activity - Focus Running")

            GoodtimeActivityAttributes.previewFocusCountdown
                .previewContext(
                    GoodtimeActivityAttributes.ContentState.previewPaused,
                    viewKind: .content
                )
                .previewDisplayName("Live Activity - Focus Paused")

            // Dynamic Island - Compact
            GoodtimeActivityAttributes.previewFocusCountdown
                .previewContext(
                    GoodtimeActivityAttributes.ContentState.previewRunning,
                    viewKind: .dynamicIsland(.compact)
                )
                .previewDisplayName("Dynamic Island - Compact")

            // Dynamic Island - Expanded
            GoodtimeActivityAttributes.previewFocusCountdown
                .previewContext(
                    GoodtimeActivityAttributes.ContentState.previewRunning,
                    viewKind: .dynamicIsland(.expanded)
                )
                .previewDisplayName("Dynamic Island - Expanded")

            GoodtimeActivityAttributes.previewFocusCountdown
                .previewContext(
                    GoodtimeActivityAttributes.ContentState.previewPaused,
                    viewKind: .dynamicIsland(.expanded)
                )
                .previewDisplayName("Dynamic Island - Expanded Paused")

            // Live Activity - Break
            GoodtimeActivityAttributes.previewShortBreak
                .previewContext(
                    GoodtimeActivityAttributes.ContentState.previewRunning,
                    viewKind: .content
                )
                .previewDisplayName("Live Activity - Break")

            // Live Activity - Count Up
            GoodtimeActivityAttributes.previewFocusCountUp
                .previewContext(
                    GoodtimeActivityAttributes.ContentState.previewCountUpRunning,
                    viewKind: .content
                )
                .previewDisplayName("Live Activity - Count Up")
        }
    }
}
