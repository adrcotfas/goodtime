# Live Activities Implementation Plan for Goodtime (KMP/CMP iOS)

## Overview

This document provides a detailed, step-by-step action plan for implementing iOS Live Activities in **Goodtime**, your Kotlin Multiplatform (KMP) / Compose Multiplatform (CMP) productivity timer app. The goal is to display both **countdown** and **count-up** timers in the Dynamic Island and Lock Screen, along with interactive controls: **Stop**, **+1 Minute**, and **Start Focus/Break** (conditional).

---

## Table of Contents

1. [Prerequisites & Requirements](#1-prerequisites--requirements)
2. [Architecture Overview](#2-architecture-overview)
3. [Understanding Countdown vs Count-Up Timers](#3-understanding-countdown-vs-count-up-timers)
4. [Phase 1: Project Setup](#phase-1-project-setup)
5. [Phase 2: Define ActivityAttributes](#phase-2-define-activityattributes)
6. [Phase 3: Create Live Activity UI](#phase-3-create-live-activity-ui)
7. [Phase 4: Implement Live Activity Manager](#phase-4-implement-live-activity-manager)
8. [Phase 5: Bridge KMP to Swift (Data Communication)](#phase-5-bridge-kmp-to-swift-data-communication)
9. [Phase 6: Add Interactive Buttons with App Intents](#phase-6-add-interactive-buttons-with-app-intents)
10. [Phase 7: Integration & Testing](#phase-7-integration--testing)
11. [Phase 8: Polish & Edge Cases](#phase-8-polish--edge-cases)
12. [Common Pitfalls & Solutions](#common-pitfalls--solutions)
13. [File Structure Reference](#file-structure-reference)

---

## 1. Prerequisites & Requirements

### System Requirements
- iOS 16.1+ (Live Activities), iOS 17+ (Interactive buttons)
- Xcode 15+ 
- Device with Dynamic Island (iPhone 14 Pro+) for full testing, though Lock Screen works on all iOS 16.1+ devices

### What You'll Need
- Your existing Goodtime KMP/CMP iOS project
- Apple Developer account (for provisioning profiles with App Groups)
- Understanding of SwiftUI basics

### Key Constraints
- Live Activities can run for up to **8 hours** (Dynamic Island) / **12 hours** (Lock Screen)
- Updates are limited—use `Text(timerInterval:)` for automatic timer display
- Widget extensions have a **4KB size limit** for the bundle
- Cannot run arbitrary code in background—timer must use `timerInterval` approach

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     KMP Shared Module                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Goodtime Timer Logic / State Management (Kotlin)       │   │
│  │  - Timer state (running, paused, focus/break mode)      │   │
│  │  - Duration, elapsed time, end time                     │   │
│  │  - Countdown OR Count-up mode                           │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   iOS Native Layer (Swift)                      │
│  ┌──────────────────────┐    ┌──────────────────────────────┐  │
│  │  LiveActivityManager │◄──►│  App Groups (UserDefaults)   │  │
│  │  - Start/Update/End  │    │  - Shared timer state        │  │
│  └──────────────────────┘    └──────────────────────────────┘  │
│              │                              ▲                   │
│              ▼                              │                   │
│  ┌──────────────────────────────────────────┴───────────────┐  │
│  │              Widget Extension (GoodtimeWidget)            │  │
│  │  ┌─────────────────────────────────────────────────────┐ │  │
│  │  │  ActivityConfiguration (SwiftUI)                    │ │  │
│  │  │  - Lock Screen View                                 │ │  │
│  │  │  - Dynamic Island (Compact/Expanded/Minimal)        │ │  │
│  │  │  - Interactive Buttons (via App Intents)            │ │  │
│  │  └─────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Understanding Countdown vs Count-Up Timers

### The `timerInterval` Mechanism

Live Activities use `Text(timerInterval:countsDown:)` for automatic timer updates without draining battery or hitting API limits. This is **essential**—you cannot use regular timers in widgets.

```swift
Text(timerInterval: startDate...endDate, countsDown: Bool)
```

### Countdown Timer (e.g., "25 minutes focus session")

```swift
// Countdown: Shows 25:00 → 24:59 → ... → 00:00
let startDate = Date()
let endDate = startDate.addingTimeInterval(25 * 60)  // 25 minutes from now

Text(timerInterval: startDate...endDate, countsDown: true)
// Display: 24:59, 24:58, 24:57... → 00:00
```

**Behavior:** Timer counts DOWN from the duration to zero. When it reaches 00:00, it stays there (doesn't go negative).

### Count-Up Timer (e.g., "How long have I been working?")

```swift
// Count-up with no limit (stopwatch style)
let startDate = Date()

Text(timerInterval: startDate...Date.distantFuture, countsDown: false)
// Display: 00:00, 00:01, 00:02... → ∞
```

```swift
// Count-up with a target (e.g., "work until you hit 2 hours")
let startDate = Date()
let targetDate = startDate.addingTimeInterval(2 * 60 * 60)  // 2 hours

Text(timerInterval: startDate...targetDate, countsDown: false)
// Display: 00:00 → 00:01 → ... → 2:00:00 (then stops)
```

### Key Differences Summary

| Aspect | Countdown | Count-Up (Unbounded) | Count-Up (Bounded) |
|--------|-----------|---------------------|-------------------|
| `countsDown` | `true` | `false` | `false` |
| Start Date | Timer start time | Timer start time | Timer start time |
| End Date | When timer should end | `Date.distantFuture` | Target time |
| Display | 25:00 → 00:00 | 00:00 → ∞ | 00:00 → target |
| At completion | Shows 00:00 | Never completes | Shows target time |
| Pause stores | Remaining time | Elapsed time | Elapsed time |
| Resume adjusts | End date | Start date | Start date |

### Pause/Resume Logic Differences

**Countdown Pause/Resume:**
```swift
// PAUSE: Store remaining time
let remaining = endDate.timeIntervalSince(Date())  // e.g., 15 minutes left
// Store: pausedTimeRemaining = remaining

// RESUME: Calculate new end date
let newEndDate = Date().addingTimeInterval(pausedTimeRemaining)
// Timer continues: 15:00 → 14:59 → ...
```

**Count-Up Pause/Resume:**
```swift
// PAUSE: Store elapsed time
let elapsed = Date().timeIntervalSince(startDate)  // e.g., 10 minutes elapsed
// Store: pausedElapsedTime = elapsed

// RESUME: Adjust start date backwards
let newStartDate = Date().addingTimeInterval(-pausedElapsedTime)
// Timer continues: 10:00 → 10:01 → ...
```

---

## Phase 1: Project Setup

### Step 1.1: Add Widget Extension Target

```
INSTRUCTION FOR CLAUDE CODE:
Open the Goodtime iOS project in Xcode. Add a new Widget Extension target.
```

**Actions:**
1. In Xcode: File → New → Target → Widget Extension
2. Name it `GoodtimeWidget`
3. **CHECK** "Include Live Activity" option
4. **UNCHECK** "Include Configuration App Intent" (we'll add intents manually)
5. When prompted, activate the scheme

### Step 1.2: Enable Live Activities in Info.plist

Add to your **main Goodtime app's** `Info.plist`:

```xml
<key>NSSupportsLiveActivities</key>
<true/>
```

If you expect frequent updates (which timers do), also add:
```xml
<key>NSSupportsLiveActivitiesFrequentUpdates</key>
<true/>
```

### Step 1.3: Configure App Groups

**This is CRITICAL for communication between main app and widget extension.**

1. Go to Apple Developer Portal → Certificates, Identifiers & Profiles → App Groups
2. Create a new App Group: `group.com.yourcompany.goodtime` (use your actual bundle ID pattern)
3. In Xcode, for BOTH targets (Goodtime main app AND GoodtimeWidget):
   - Select target → Signing & Capabilities → + Capability → App Groups
   - Enable your app group

### Step 1.4: Add Widget Extension to CocoaPods/SPM (if using KMP shared code)

If you're consuming KMP shared code in the widget, update your `Podfile`:

```ruby
target 'GoodtimeWidget' do
  pod 'shared', :path => '../shared'  # Adjust path to your KMP module
end
```

Then run `pod install`.

**Note:** Widget extensions have strict size limits. Consider whether you need full KMP access in the widget or just shared data via App Groups.

---

## Phase 2: Define ActivityAttributes

### Step 2.1: Create the Attributes Structure

Create a new Swift file `GoodtimeActivityAttributes.swift` that will be shared between the main app and widget extension.

**Important:** Add this file to BOTH targets (Goodtime app and GoodtimeWidget) in the File Inspector.

```swift
// GoodtimeActivityAttributes.swift
// Target Membership: Goodtime ✓, GoodtimeWidget ✓

import Foundation
import ActivityKit

struct GoodtimeActivityAttributes: ActivityAttributes {
    
    // STATIC properties (don't change during the activity)
    let timerType: TimerType
    let timerMode: TimerMode
    let targetDuration: TimeInterval?  // nil for unbounded count-up
    
    // ContentState contains DYNAMIC properties (change during activity)
    struct ContentState: Codable, Hashable {
        // Timer interval boundaries
        let timerStartDate: Date
        let timerEndDate: Date
        
        // State flags
        let isPaused: Bool
        
        // Paused state storage (different for countdown vs count-up)
        let pausedTimeRemaining: TimeInterval?  // For countdown pause
        let pausedElapsedTime: TimeInterval?    // For count-up pause
        
        // For conditional button display
        let canStartFocus: Bool
        let canStartBreak: Bool
    }
    
    enum TimerType: String, Codable {
        case focus
        case shortBreak
        case longBreak
        case custom
    }
    
    enum TimerMode: String, Codable {
        case countdown      // 25:00 → 00:00
        case countUp        // 00:00 → ∞ (unbounded)
        case countUpTarget  // 00:00 → target (bounded)
    }
}

// MARK: - Convenience Extensions

extension GoodtimeActivityAttributes {
    var isCountingDown: Bool {
        timerMode == .countdown
    }
}

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
```

### Step 2.2: Create a Shared Data Model for App Groups

Create a file for data that needs to be shared via UserDefaults:

```swift
// GoodtimeSharedState.swift
// Target Membership: Goodtime ✓, GoodtimeWidget ✓

import Foundation

struct GoodtimeSharedState: Codable {
    let isRunning: Bool
    let isPaused: Bool
    let timerType: String
    let timerMode: String
    let startDate: Date?
    let endDate: Date?
    let pausedTimeRemaining: TimeInterval?
    let pausedElapsedTime: TimeInterval?
    let targetDuration: TimeInterval?
    
    static let userDefaultsKey = "goodtimeSharedTimerState"
    static let appGroupID = "group.com.yourcompany.goodtime"  // UPDATE THIS
    
    static var shared: UserDefaults? {
        UserDefaults(suiteName: appGroupID)
    }
    
    func save() {
        guard let data = try? JSONEncoder().encode(self),
              let defaults = Self.shared else { return }
        defaults.set(data, forKey: Self.userDefaultsKey)
        defaults.synchronize()  // Ensure immediate write
    }
    
    static func load() -> GoodtimeSharedState? {
        guard let defaults = Self.shared,
              let data = defaults.data(forKey: Self.userDefaultsKey),
              let state = try? JSONDecoder().decode(GoodtimeSharedState.self, from: data)
        else { return nil }
        return state
    }
    
    static func clear() {
        shared?.removeObject(forKey: userDefaultsKey)
        shared?.synchronize()
    }
}
```

---

## Phase 3: Create Live Activity UI

### Step 3.1: Understand the UI Components

Live Activities have multiple presentation modes:

| Mode | When Shown | Space Available |
|------|-----------|-----------------|
| **Lock Screen** | Always on Lock Screen | Full width banner |
| **Compact Leading** | Dynamic Island, single activity | Left side of island |
| **Compact Trailing** | Dynamic Island, single activity | Right side of island |
| **Minimal** | Dynamic Island, multiple activities | Small circular area |
| **Expanded** | User long-presses Dynamic Island | Large expanded view |

### Step 3.2: Create the Live Activity Widget

Replace or update `GoodtimeWidgetLiveActivity.swift`:

```swift
// GoodtimeWidgetLiveActivity.swift

import ActivityKit
import WidgetKit
import SwiftUI

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
                }
                
                DynamicIslandExpandedRegion(.trailing) {
                    GoodtimeTimerDisplay(context: context, style: .expanded)
                }
                
                DynamicIslandExpandedRegion(.center) {
                    VStack(spacing: 2) {
                        Text(timerTypeLabel(context.attributes.timerType))
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        if !context.attributes.isCountingDown {
                            Text("Count Up")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                DynamicIslandExpandedRegion(.bottom) {
                    GoodtimeExpandedControlsView(context: context)
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
        case .custom: return "timer"
        }
    }
    
    private func timerTypeLabel(_ type: GoodtimeActivityAttributes.TimerType) -> String {
        switch type {
        case .focus: return "Focus"
        case .shortBreak: return "Short Break"
        case .longBreak: return "Long Break"
        case .custom: return "Timer"
        }
    }
    
    private func backgroundTint(for type: GoodtimeActivityAttributes.TimerType) -> Color {
        switch type {
        case .focus: return .indigo
        case .shortBreak, .longBreak: return .green
        case .custom: return .blue
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
            if context.state.isPaused {
                // PAUSED: Show static time
                pausedTimeText
                    .foregroundColor(.yellow)
            } else {
                // RUNNING: Auto-updating timer
                Text(
                    timerInterval: context.state.timerStartDate...context.state.timerEndDate,
                    countsDown: context.attributes.isCountingDown
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
                
                HStack(spacing: 8) {
                    if !context.attributes.isCountingDown {
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
        .padding()
    }
    
    private var iconName: String {
        switch context.attributes.timerType {
        case .focus: return "brain.head.profile"
        case .shortBreak, .longBreak: return "cup.and.saucer.fill"
        case .custom: return "timer"
        }
    }
    
    private var timerLabel: String {
        switch context.attributes.timerType {
        case .focus: return "Focus"
        case .shortBreak: return "Short Break"
        case .longBreak: return "Long Break"
        case .custom: return "Timer"
        }
    }
}

// MARK: - Expanded Controls (Placeholder - buttons added in Phase 6)

struct GoodtimeExpandedControlsView: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>
    
    var body: some View {
        HStack(spacing: 20) {
            // Placeholder - buttons will be added in Phase 6
            Text("Controls loading...")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}
```

### Step 3.3: Register the Widget

Update `GoodtimeWidgetBundle.swift`:

```swift
import WidgetKit
import SwiftUI

@main
struct GoodtimeWidgetBundle: WidgetBundle {
    var body: some Widget {
        // Add your home screen widgets here if any
        // GoodtimeHomeWidget()
        
        // Live Activity
        GoodtimeLiveActivity()
    }
}
```

---

## Phase 4: Implement Live Activity Manager

### Step 4.1: Create the Manager Class

Create `GoodtimeLiveActivityManager.swift` in your main Goodtime app target:

```swift
// GoodtimeLiveActivityManager.swift
// Target Membership: Goodtime ✓ (NOT widget extension)

import Foundation
import ActivityKit
import UIKit

@available(iOS 16.1, *)
class GoodtimeLiveActivityManager: ObservableObject {
    
    static let shared = GoodtimeLiveActivityManager()
    
    @Published private(set) var currentActivity: Activity<GoodtimeActivityAttributes>?
    
    private init() {}
    
    // MARK: - Check Availability
    
    var areActivitiesEnabled: Bool {
        ActivityAuthorizationInfo().areActivitiesEnabled
    }
    
    // MARK: - Start Countdown Activity
    
    func startCountdownActivity(
        timerType: GoodtimeActivityAttributes.TimerType,
        duration: TimeInterval
    ) async throws {
        
        await endAllActivities()
        
        let now = Date()
        let endDate = now.addingTimeInterval(duration)
        
        let attributes = GoodtimeActivityAttributes(
            timerType: timerType,
            timerMode: .countdown,
            targetDuration: duration
        )
        
        let initialState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: now,
            timerEndDate: endDate,
            isPaused: false,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil,
            canStartFocus: timerType != .focus,
            canStartBreak: timerType == .focus
        )
        
        let content = ActivityContent(
            state: initialState,
            staleDate: endDate.addingTimeInterval(60)
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
            
            saveSharedState(
                isRunning: true,
                isPaused: false,
                timerType: timerType,
                timerMode: .countdown,
                startDate: now,
                endDate: endDate,
                targetDuration: duration
            )
            
            print("Goodtime: Countdown Live Activity started - ID: \(activity.id)")
            
        } catch {
            print("Goodtime: Failed to start Live Activity - \(error)")
            throw error
        }
    }
    
    // MARK: - Start Count-Up Activity
    
    func startCountUpActivity(
        timerType: GoodtimeActivityAttributes.TimerType,
        targetDuration: TimeInterval? = nil  // nil = unbounded
    ) async throws {
        
        await endAllActivities()
        
        let now = Date()
        let endDate: Date
        let mode: GoodtimeActivityAttributes.TimerMode
        
        if let target = targetDuration {
            endDate = now.addingTimeInterval(target)
            mode = .countUpTarget
        } else {
            endDate = Date.distantFuture
            mode = .countUp
        }
        
        let attributes = GoodtimeActivityAttributes(
            timerType: timerType,
            timerMode: mode,
            targetDuration: targetDuration
        )
        
        let initialState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: now,
            timerEndDate: endDate,
            isPaused: false,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil,
            canStartFocus: timerType != .focus,
            canStartBreak: timerType == .focus
        )
        
        let content = ActivityContent(
            state: initialState,
            staleDate: targetDuration != nil ? endDate.addingTimeInterval(60) : nil
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
            
            saveSharedState(
                isRunning: true,
                isPaused: false,
                timerType: timerType,
                timerMode: mode,
                startDate: now,
                endDate: endDate,
                targetDuration: targetDuration
            )
            
            print("Goodtime: Count-up Live Activity started - ID: \(activity.id)")
            
        } catch {
            print("Goodtime: Failed to start Live Activity - \(error)")
            throw error
        }
    }
    
    // MARK: - Pause Activity (handles both countdown and count-up)
    
    func pauseActivity() async {
        guard let activity = currentActivity else {
            print("Goodtime: No active Live Activity to pause")
            return
        }
        
        let currentState = activity.content.state
        let now = Date()
        
        var pausedRemaining: TimeInterval? = nil
        var pausedElapsed: TimeInterval? = nil
        
        if activity.attributes.isCountingDown {
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
            pausedTimeRemaining: pausedRemaining,
            pausedElapsedTime: pausedElapsed,
            canStartFocus: currentState.canStartFocus,
            canStartBreak: currentState.canStartBreak
        )
        
        let content = ActivityContent(
            state: updatedState,
            staleDate: nil  // No stale date when paused
        )
        
        await activity.update(content)
        
        saveSharedState(
            isRunning: true,
            isPaused: true,
            timerType: activity.attributes.timerType,
            timerMode: activity.attributes.timerMode,
            startDate: currentState.timerStartDate,
            endDate: currentState.timerEndDate,
            pausedRemaining: pausedRemaining,
            pausedElapsed: pausedElapsed,
            targetDuration: activity.attributes.targetDuration
        )
        
        print("Goodtime: Activity paused")
    }
    
    // MARK: - Resume Activity (handles both countdown and count-up)
    
    func resumeActivity() async {
        guard let activity = currentActivity else {
            print("Goodtime: No active Live Activity to resume")
            return
        }
        
        let currentState = activity.content.state
        let now = Date()
        
        var newStartDate: Date
        var newEndDate: Date
        
        if activity.attributes.isCountingDown {
            // COUNTDOWN: Adjust end date based on remaining time
            let remaining = currentState.pausedTimeRemaining ?? 0
            newStartDate = now
            newEndDate = now.addingTimeInterval(remaining)
        } else {
            // COUNT-UP: Adjust start date backwards based on elapsed time
            let elapsed = currentState.pausedElapsedTime ?? 0
            newStartDate = now.addingTimeInterval(-elapsed)
            
            if activity.attributes.timerMode == .countUpTarget,
               let target = activity.attributes.targetDuration {
                // Bounded count-up: also adjust end date
                newEndDate = newStartDate.addingTimeInterval(target)
            } else {
                // Unbounded count-up
                newEndDate = Date.distantFuture
            }
        }
        
        let updatedState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: newStartDate,
            timerEndDate: newEndDate,
            isPaused: false,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil,
            canStartFocus: currentState.canStartFocus,
            canStartBreak: currentState.canStartBreak
        )
        
        let staleDate: Date? = activity.attributes.isCountingDown || activity.attributes.timerMode == .countUpTarget
            ? newEndDate.addingTimeInterval(60)
            : nil
        
        let content = ActivityContent(
            state: updatedState,
            staleDate: staleDate
        )
        
        await activity.update(content)
        
        saveSharedState(
            isRunning: true,
            isPaused: false,
            timerType: activity.attributes.timerType,
            timerMode: activity.attributes.timerMode,
            startDate: newStartDate,
            endDate: newEndDate,
            targetDuration: activity.attributes.targetDuration
        )
        
        print("Goodtime: Activity resumed")
    }
    
    // MARK: - Add One Minute (only for countdown)
    
    func addOneMinute() async {
        guard let activity = currentActivity else { return }
        guard activity.attributes.isCountingDown else {
            print("Goodtime: +1 minute only available for countdown timers")
            return
        }
        
        let currentState = activity.content.state
        let newEndDate: Date
        
        if currentState.isPaused {
            // If paused, add to the remaining time
            let newRemaining = (currentState.pausedTimeRemaining ?? 0) + 60
            
            let updatedState = GoodtimeActivityAttributes.ContentState(
                timerStartDate: currentState.timerStartDate,
                timerEndDate: currentState.timerEndDate,  // Doesn't matter when paused
                isPaused: true,
                pausedTimeRemaining: newRemaining,
                pausedElapsedTime: nil,
                canStartFocus: currentState.canStartFocus,
                canStartBreak: currentState.canStartBreak
            )
            
            await activity.update(ActivityContent(state: updatedState, staleDate: nil))
            return
        }
        
        newEndDate = currentState.timerEndDate.addingTimeInterval(60)
        
        let updatedState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: currentState.timerStartDate,
            timerEndDate: newEndDate,
            isPaused: false,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil,
            canStartFocus: currentState.canStartFocus,
            canStartBreak: currentState.canStartBreak
        )
        
        let content = ActivityContent(
            state: updatedState,
            staleDate: newEndDate.addingTimeInterval(60)
        )
        
        await activity.update(content)
        
        saveSharedState(
            isRunning: true,
            isPaused: false,
            timerType: activity.attributes.timerType,
            timerMode: activity.attributes.timerMode,
            startDate: currentState.timerStartDate,
            endDate: newEndDate,
            targetDuration: activity.attributes.targetDuration
        )
        
        print("Goodtime: Added 1 minute")
    }
    
    // MARK: - End Activity
    
    func endActivity(showFinalState: Bool = true) async {
        guard let activity = currentActivity else { return }
        
        let finalState = GoodtimeActivityAttributes.ContentState(
            timerStartDate: activity.content.state.timerStartDate,
            timerEndDate: Date(),
            isPaused: false,
            pausedTimeRemaining: nil,
            pausedElapsedTime: nil,
            canStartFocus: true,
            canStartBreak: true
        )
        
        let finalContent = ActivityContent(
            state: finalState,
            staleDate: nil
        )
        
        await activity.end(
            finalContent,
            dismissalPolicy: showFinalState ? .after(Date().addingTimeInterval(5)) : .immediate
        )
        
        await MainActor.run {
            self.currentActivity = nil
        }
        
        GoodtimeSharedState.clear()
        
        print("Goodtime: Activity ended")
    }
    
    func endAllActivities() async {
        for activity in Activity<GoodtimeActivityAttributes>.activities {
            await activity.end(nil, dismissalPolicy: .immediate)
        }
        await MainActor.run {
            self.currentActivity = nil
        }
        GoodtimeSharedState.clear()
    }
    
    // MARK: - App Groups Storage
    
    private func saveSharedState(
        isRunning: Bool,
        isPaused: Bool,
        timerType: GoodtimeActivityAttributes.TimerType,
        timerMode: GoodtimeActivityAttributes.TimerMode,
        startDate: Date?,
        endDate: Date?,
        pausedRemaining: TimeInterval? = nil,
        pausedElapsed: TimeInterval? = nil,
        targetDuration: TimeInterval?
    ) {
        let state = GoodtimeSharedState(
            isRunning: isRunning,
            isPaused: isPaused,
            timerType: timerType.rawValue,
            timerMode: timerMode.rawValue,
            startDate: startDate,
            endDate: endDate,
            pausedTimeRemaining: pausedRemaining,
            pausedElapsedTime: pausedElapsed,
            targetDuration: targetDuration
        )
        state.save()
    }
}
```

---

## Phase 5: Bridge KMP to Swift (Data Communication)

### Step 5.1: Create KMP Interface

In your shared KMP module, define the interface:

```kotlin
// commonMain/kotlin/com/goodtime/timer/LiveActivityBridge.kt

expect class LiveActivityBridge {
    
    // Start countdown timer (25:00 → 00:00)
    fun startCountdownActivity(
        timerType: String,  // "focus", "shortBreak", "longBreak", "custom"
        durationSeconds: Long
    )
    
    // Start count-up timer (00:00 → ∞ or target)
    fun startCountUpActivity(
        timerType: String,
        targetDurationSeconds: Long?  // null = unbounded
    )
    
    fun pauseActivity()
    
    fun resumeActivity()
    
    fun addOneMinute()
    
    fun endActivity()
    
    fun isLiveActivitySupported(): Boolean
}
```

### Step 5.2: iOS Implementation (Kotlin)

```kotlin
// iosMain/kotlin/com/goodtime/timer/LiveActivityBridge.ios.kt

import platform.Foundation.*

actual class LiveActivityBridge {
    
    actual fun startCountdownActivity(
        timerType: String,
        durationSeconds: Long
    ) {
        GoodtimeLiveActivityBridgeImpl.shared.startCountdown(
            timerType = timerType,
            duration = durationSeconds.toDouble()
        )
    }
    
    actual fun startCountUpActivity(
        timerType: String,
        targetDurationSeconds: Long?
    ) {
        GoodtimeLiveActivityBridgeImpl.shared.startCountUp(
            timerType = timerType,
            targetDuration = targetDurationSeconds?.toDouble()
        )
    }
    
    actual fun pauseActivity() {
        GoodtimeLiveActivityBridgeImpl.shared.pause()
    }
    
    actual fun resumeActivity() {
        GoodtimeLiveActivityBridgeImpl.shared.resume()
    }
    
    actual fun addOneMinute() {
        GoodtimeLiveActivityBridgeImpl.shared.addMinute()
    }
    
    actual fun endActivity() {
        GoodtimeLiveActivityBridgeImpl.shared.end()
    }
    
    actual fun isLiveActivitySupported(): Boolean {
        return GoodtimeLiveActivityBridgeImpl.shared.isSupported()
    }
}
```

### Step 5.3: Android Stub (Kotlin)

```kotlin
// androidMain/kotlin/com/goodtime/timer/LiveActivityBridge.android.kt

actual class LiveActivityBridge {
    
    actual fun startCountdownActivity(timerType: String, durationSeconds: Long) {
        // No-op on Android (or implement Android widgets/notifications)
    }
    
    actual fun startCountUpActivity(timerType: String, targetDurationSeconds: Long?) {
        // No-op on Android
    }
    
    actual fun pauseActivity() { }
    actual fun resumeActivity() { }
    actual fun addOneMinute() { }
    actual fun endActivity() { }
    
    actual fun isLiveActivitySupported(): Boolean = false
}
```

### Step 5.4: Swift Bridge Implementation

Create in your Goodtime iOS app:

```swift
// GoodtimeLiveActivityBridgeImpl.swift
// Target Membership: Goodtime ✓

import Foundation
import ActivityKit

@objc public class GoodtimeLiveActivityBridgeImpl: NSObject {
    
    @objc public static let shared = GoodtimeLiveActivityBridgeImpl()
    
    private override init() {
        super.init()
    }
    
    @objc public func startCountdown(timerType: String, duration: Double) {
        guard #available(iOS 16.1, *) else { return }
        
        let type = mapTimerType(timerType)
        
        Task {
            try? await GoodtimeLiveActivityManager.shared.startCountdownActivity(
                timerType: type,
                duration: duration
            )
        }
    }
    
    @objc public func startCountUp(timerType: String, targetDuration: NSNumber?) {
        guard #available(iOS 16.1, *) else { return }
        
        let type = mapTimerType(timerType)
        
        Task {
            try? await GoodtimeLiveActivityManager.shared.startCountUpActivity(
                timerType: type,
                targetDuration: targetDuration?.doubleValue
            )
        }
    }
    
    @objc public func pause() {
        guard #available(iOS 16.1, *) else { return }
        Task {
            await GoodtimeLiveActivityManager.shared.pauseActivity()
        }
    }
    
    @objc public func resume() {
        guard #available(iOS 16.1, *) else { return }
        Task {
            await GoodtimeLiveActivityManager.shared.resumeActivity()
        }
    }
    
    @objc public func addMinute() {
        guard #available(iOS 16.1, *) else { return }
        Task {
            await GoodtimeLiveActivityManager.shared.addOneMinute()
        }
    }
    
    @objc public func end() {
        guard #available(iOS 16.1, *) else { return }
        Task {
            await GoodtimeLiveActivityManager.shared.endActivity()
        }
    }
    
    @objc public func isSupported() -> Bool {
        guard #available(iOS 16.1, *) else { return false }
        return GoodtimeLiveActivityManager.shared.areActivitiesEnabled
    }
    
    private func mapTimerType(_ type: String) -> GoodtimeActivityAttributes.TimerType {
        switch type.lowercased() {
        case "focus": return .focus
        case "shortbreak": return .shortBreak
        case "longbreak": return .longBreak
        default: return .custom
        }
    }
}
```

---

## Phase 6: Add Interactive Buttons with App Intents

### Step 6.1: Create App Intents (Main App Target ONLY)

Create `GoodtimeIntents.swift` in your **main Goodtime app target only**:

```swift
// GoodtimeIntents.swift
// Target Membership: Goodtime ✓ (NOT GoodtimeWidget)

import AppIntents
import ActivityKit

// MARK: - Stop Timer Intent

struct GoodtimeStopIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Stop Timer"
    static var description = IntentDescription("Stops the current Goodtime timer")
    
    func perform() async throws -> some IntentResult {
        guard #available(iOS 16.1, *) else {
            return .result()
        }
        
        await GoodtimeLiveActivityManager.shared.endActivity()
        
        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeTimerStoppedFromLiveActivity"),
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
        guard #available(iOS 16.1, *) else {
            return .result()
        }
        
        await GoodtimeLiveActivityManager.shared.addOneMinute()
        
        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeTimerExtendedFromLiveActivity"),
            object: nil,
            userInfo: ["addedSeconds": 60]
        )
        
        return .result()
    }
}

// MARK: - Start Focus Intent

struct GoodtimeStartFocusIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Start Focus"
    static var description = IntentDescription("Starts a new focus session")
    
    func perform() async throws -> some IntentResult {
        guard #available(iOS 16.1, *) else {
            return .result()
        }
        
        // Get default focus duration from user settings/App Groups
        let focusDuration: TimeInterval = 25 * 60  // Default 25 minutes
        
        await GoodtimeLiveActivityManager.shared.endAllActivities()
        try? await GoodtimeLiveActivityManager.shared.startCountdownActivity(
            timerType: .focus,
            duration: focusDuration
        )
        
        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeFocusStartedFromLiveActivity"),
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
        guard #available(iOS 16.1, *) else {
            return .result()
        }
        
        let breakDuration: TimeInterval = 5 * 60  // Default 5 minutes
        
        await GoodtimeLiveActivityManager.shared.endAllActivities()
        try? await GoodtimeLiveActivityManager.shared.startCountdownActivity(
            timerType: .shortBreak,
            duration: breakDuration
        )
        
        NotificationCenter.default.post(
            name: NSNotification.Name("GoodtimeBreakStartedFromLiveActivity"),
            object: nil
        )
        
        return .result()
    }
}
```

### Step 6.2: Create Intent Placeholders (Widget Extension ONLY)

**Critical:** The widget extension needs to know about the intents to compile but can't execute them.

```swift
// GoodtimeIntentPlaceholders.swift
// Target Membership: GoodtimeWidget ✓ (NOT Goodtime main app)

import AppIntents

// These are placeholders that allow the widget to compile
// The actual implementations are in the main app target

struct GoodtimeStopIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Stop Timer"
    
    func perform() async throws -> some IntentResult {
        return .result()
    }
}

struct GoodtimeAddMinuteIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Add 1 Minute"
    
    func perform() async throws -> some IntentResult {
        return .result()
    }
}

struct GoodtimeStartFocusIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Start Focus"
    
    func perform() async throws -> some IntentResult {
        return .result()
    }
}

struct GoodtimeStartBreakIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Start Break"
    
    func perform() async throws -> some IntentResult {
        return .result()
    }
}
```

### Step 6.3: Update the Expanded Controls View

Now update `GoodtimeExpandedControlsView` with actual buttons:

```swift
// Update in GoodtimeWidgetLiveActivity.swift

struct GoodtimeExpandedControlsView: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>
    
    var body: some View {
        HStack(spacing: 16) {
            // Stop Button - always visible
            Button(intent: GoodtimeStopIntent()) {
                Label("Stop", systemImage: "stop.fill")
                    .font(.caption)
            }
            .buttonStyle(.borderedProminent)
            .tint(.red)
            
            // +1 Minute Button - only for countdown
            if context.attributes.isCountingDown {
                Button(intent: GoodtimeAddMinuteIntent()) {
                    Label("+1", systemImage: "plus.circle.fill")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
            }
            
            // Conditional: Start Focus (when on break)
            if context.state.canStartFocus {
                Button(intent: GoodtimeStartFocusIntent()) {
                    Label("Focus", systemImage: "brain.head.profile")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .tint(.indigo)
            }
            
            // Conditional: Start Break (when on focus)
            if context.state.canStartBreak {
                Button(intent: GoodtimeStartBreakIntent()) {
                    Label("Break", systemImage: "cup.and.saucer.fill")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .tint(.green)
            }
        }
        .padding(.horizontal)
    }
}
```

---

## Phase 7: Integration & Testing

### Step 7.1: Call from Your Goodtime KMP Timer Logic

```kotlin
// Example in your Goodtime shared module

class GoodtimeTimerViewModel {
    private val liveActivityBridge = LiveActivityBridge()
    
    fun startFocusSession(durationMinutes: Int, isCountUp: Boolean = false) {
        // Your existing timer logic...
        
        if (liveActivityBridge.isLiveActivitySupported()) {
            if (isCountUp) {
                liveActivityBridge.startCountUpActivity(
                    timerType = "focus",
                    targetDurationSeconds = (durationMinutes * 60).toLong()
                )
            } else {
                liveActivityBridge.startCountdownActivity(
                    timerType = "focus",
                    durationSeconds = (durationMinutes * 60).toLong()
                )
            }
        }
    }
    
    fun startOpenFocusSession() {
        // Unbounded count-up (stopwatch style)
        if (liveActivityBridge.isLiveActivitySupported()) {
            liveActivityBridge.startCountUpActivity(
                timerType = "focus",
                targetDurationSeconds = null  // No limit
            )
        }
    }
    
    fun pauseTimer() {
        // Your existing pause logic...
        liveActivityBridge.pauseActivity()
    }
    
    fun resumeTimer() {
        // Your existing resume logic...
        liveActivityBridge.resumeActivity()
    }
    
    fun stopTimer() {
        // Your existing stop logic...
        liveActivityBridge.endActivity()
    }
    
    fun addOneMinute() {
        // Your existing logic...
        liveActivityBridge.addOneMinute()
    }
}
```

### Step 7.2: Handle Intent Notifications in Main App

```swift
// In your Goodtime App or AppDelegate

.onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("GoodtimeTimerStoppedFromLiveActivity"))) { _ in
    // Sync your KMP state
    // goodtimeViewModel.handleTimerStoppedExternally()
}

.onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("GoodtimeTimerExtendedFromLiveActivity"))) { notification in
    if let seconds = notification.userInfo?["addedSeconds"] as? Int {
        // Sync your KMP state
        // goodtimeViewModel.handleTimeAdded(seconds)
    }
}

.onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("GoodtimeFocusStartedFromLiveActivity"))) { _ in
    // Sync your KMP state to new focus session
}

.onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("GoodtimeBreakStartedFromLiveActivity"))) { _ in
    // Sync your KMP state to new break session
}
```

### Step 7.3: Testing Checklist

**Simulator Testing:**
- [ ] Countdown Live Activity starts correctly
- [ ] Count-up Live Activity starts correctly (both bounded and unbounded)
- [ ] Timer displays correctly in Lock Screen
- [ ] Countdown shows decreasing time (25:00 → 00:00)
- [ ] Count-up shows increasing time (00:00 → target or infinity)
- [ ] Pause shows static time (correct value for both modes)
- [ ] Resume continues correctly (countdown from remaining, count-up from elapsed)
- [ ] Activity ends when timer completes or stopped

**Device Testing (requires iPhone 14 Pro+ for Dynamic Island):**
- [ ] Compact leading shows correct icon
- [ ] Compact trailing shows timer (both countdown and count-up)
- [ ] Long-press shows expanded view
- [ ] All buttons respond to taps
- [ ] Stop button ends timer
- [ ] +1 minute adds time (countdown only)
- [ ] Start Focus/Break buttons work conditionally
- [ ] Multiple activities show minimal view

### Step 7.4: Debug Tips

```swift
// Add to GoodtimeLiveActivityManager for debugging

func debugPrintAllActivities() {
    print("=== Goodtime Live Activities Debug ===")
    for activity in Activity<GoodtimeActivityAttributes>.activities {
        print("ID: \(activity.id)")
        print("Type: \(activity.attributes.timerType)")
        print("Mode: \(activity.attributes.timerMode)")
        print("isPaused: \(activity.content.state.isPaused)")
        print("Start: \(activity.content.state.timerStartDate)")
        print("End: \(activity.content.state.timerEndDate)")
        print("---")
    }
}
```

---

## Phase 8: Polish & Edge Cases

### Step 8.1: Handle App Lifecycle

```swift
// In your Goodtime App or SceneDelegate

func sceneDidBecomeActive(_ scene: UIScene) {
    Task {
        await syncLiveActivityWithGoodtimeState()
    }
}

private func syncLiveActivityWithGoodtimeState() async {
    // Read actual timer state from your KMP layer
    // Update Live Activity if out of sync
}
```

### Step 8.2: Handle Timer Completion

```swift
// For countdown timers that reach 00:00

func handleCountdownCompletion() async {
    guard let activity = currentActivity,
          activity.attributes.isCountingDown else { return }
    
    // The timerInterval Text will show 00:00 but won't update further
    // Options:
    // 1. End activity after a delay
    // 2. Update to show "Complete!" message
    // 3. Auto-start break (if focus just completed)
    
    DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
        Task {
            await self.endActivity(showFinalState: false)
        }
    }
}
```

### Step 8.3: Handle Edge Cases

```swift
// Check if activities are enabled before starting
func startActivitySafely() async throws {
    guard ActivityAuthorizationInfo().areActivitiesEnabled else {
        throw GoodtimeError.liveActivitiesDisabled
    }
    // ... start activity
}

// Handle permission changes
func observeActivityPermissions() {
    Task {
        for await enabled in ActivityAuthorizationInfo().activityEnablementUpdates {
            if !enabled {
                // User disabled Live Activities in Settings
                // Update Goodtime UI to reflect this
            }
        }
    }
}
```

---

## Common Pitfalls & Solutions

### Problem: Timer Doesn't Update Automatically
**Solution:** You MUST use `Text(timerInterval:countsDown:)`. Regular Text with a timer value only updates when you call `activity.update()`.

### Problem: Count-Up Timer Shows Wrong Time After Resume
**Solution:** For count-up, adjust the **start date** backwards when resuming:
```swift
let newStartDate = Date().addingTimeInterval(-pausedElapsedTime)
```

### Problem: Countdown Timer Shows Wrong Time After Resume
**Solution:** For countdown, adjust the **end date** forward when resuming:
```swift
let newEndDate = Date().addingTimeInterval(pausedTimeRemaining)
```

### Problem: Widget Extension Won't Compile with Intents
**Solution:** Create separate placeholder intent files for the widget target. The actual implementations should only be in the main app target.

### Problem: App Groups Data Not Syncing
**Solution:** 
1. Verify App Group is enabled on BOTH targets
2. Use exact same App Group identifier
3. Call `synchronize()` after writes

### Problem: Live Activity Not Showing
**Solution:**
1. Check `NSSupportsLiveActivities` is in Info.plist
2. Verify `ActivityAuthorizationInfo().areActivitiesEnabled` returns true
3. Check device is iOS 16.1+
4. User may have disabled in Settings → Face ID & Passcode → Live Activities

### Problem: Buttons Don't Work
**Solution:**
1. Verify using `Button(intent:)` not regular `Button(action:)`
2. Intent must conform to `LiveActivityIntent` protocol
3. Ensure intent implementation is in main app target

### Problem: +1 Minute Doesn't Make Sense for Count-Up
**Solution:** Hide the +1 minute button when in count-up mode:
```swift
if context.attributes.isCountingDown {
    Button(intent: GoodtimeAddMinuteIntent()) { ... }
}
```

---

## File Structure Reference

```
Goodtime/
├── shared/                              # KMP Shared Module
│   ├── commonMain/
│   │   └── kotlin/
│   │       └── com/goodtime/timer/
│   │           └── LiveActivityBridge.kt
│   ├── iosMain/
│   │   └── kotlin/
│   │       └── com/goodtime/timer/
│   │           └── LiveActivityBridge.ios.kt
│   └── androidMain/
│       └── kotlin/
│           └── com/goodtime/timer/
│               └── LiveActivityBridge.android.kt
│
├── iosApp/                              # iOS App (Goodtime)
│   ├── Info.plist                       # Add NSSupportsLiveActivities
│   ├── GoodtimeActivityAttributes.swift # Shared with widget
│   ├── GoodtimeSharedState.swift        # Shared with widget
│   ├── GoodtimeLiveActivityManager.swift# Main App only
│   ├── GoodtimeLiveActivityBridgeImpl.swift # Main App only
│   └── GoodtimeIntents.swift            # Main App only
│
└── GoodtimeWidget/                      # Widget Extension
    ├── GoodtimeWidgetBundle.swift
    ├── GoodtimeWidgetLiveActivity.swift
    ├── GoodtimeIntentPlaceholders.swift # Widget only
    ├── GoodtimeActivityAttributes.swift # Shared (same file, both targets)
    └── GoodtimeSharedState.swift        # Shared (same file, both targets)
```

---

## Summary of Key Steps

1. **Add Widget Extension** (`GoodtimeWidget`) with Live Activity support
2. **Configure App Groups** for data sharing between app and widget
3. **Define ActivityAttributes** with support for both countdown and count-up modes
4. **Create SwiftUI views** that handle both timer directions
5. **Implement LiveActivityManager** with separate countdown/count-up logic
6. **Bridge KMP to Swift** for cross-platform integration
7. **Add App Intents** for interactive buttons (with placeholder pattern)
8. **Handle pause/resume correctly** (different logic for countdown vs count-up)
9. **Test thoroughly** on device with Dynamic Island
10. **Handle edge cases** and app lifecycle

Good luck implementing Live Activities in Goodtime! This feature will make your productivity timer even more engaging and useful for users who want to track their focus sessions without constantly checking their phone.
