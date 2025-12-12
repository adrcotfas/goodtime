# Live Activity UI Update Plan

**Project:** Goodtime Productivity
**Target:** iOS Live Activity & Dynamic Island
**Date:** 2025-12-10

## Overview

This document outlines the plan to update the Live Activity UI for better visual consistency with the Android app and improved user experience.

**Current Implementation Files:**
- `iosApp/GoodtimeInProgress/GoodtimeInProgressLiveActivity.swift` - Main Live Activity widget
- `iosApp/GoodtimeInProgress/GoodtimeActivityAttributes.swift` - Activity attributes and state
- `iosApp/iosApp/GoodtimeLiveActivityManager.swift` - Activity lifecycle management
- `composeApp/src/androidMain/kotlin/.../NotificationArchManager.kt` - Android notification reference

---

## 1. Custom Graphics for Focus and Break Icons

### Current State
- Using SF Symbols: `brain.head.profile` (focus) and `cup.and.saucer.fill` (break)
- Located in: `GoodtimeInProgressLiveActivity.swift:109-114`

### Custom Assets Available
- `composeApp/src/commonMain/composeResources/drawable/ic_status_goodtime.xml` - Focus icon (hourglass/timer)
- `composeApp/src/commonMain/composeResources/drawable/ic_break.xml` - Break icon (coffee cup)

### Label Icon Required
- **You need to provide a label icon** that will be displayed next to the label name in the expanded and lock screen views
- This icon will be tinted with the label's color
- Can use SF Symbol `tag.fill` temporarily or export a custom icon from your drawable resources
- The icon appears to the left of the label name when a custom (non-default) label is active

### Implementation Steps

#### Option A: Use SF Symbols (Current - Simplest)
**Pros:**
- Already implemented
- Native iOS look and feel
- Automatically adapts to system settings
- No asset conversion needed

**Cons:**
- May not match Android branding exactly

#### Option B: Convert XML Vector Drawables to SwiftUI
**Pros:**
- Exact visual consistency with Android
- Full control over appearance

**Cons:**
- Requires manual conversion of vector paths
- More code to maintain

**Implementation:**
```swift
// Add to GoodtimeInProgressLiveActivity.swift
private func customFocusIcon() -> some View {
    Path { path in
        // Convert XML vector path data to SwiftUI Path commands
        // From ic_status_goodtime.xml line 25-26
    }
    .fill(Color.white)
    .frame(width: 24, height: 24)
}
```

#### Option C: Use PNG/PDF Assets (Recommended)
**Pros:**
- Easy to implement
- Maintains exact branding
- iOS Asset Catalog benefits (automatic scaling)

**Cons:**
- Need to export/convert from XML

**Steps:**
1. Export XML vectors to PDF or PNG @1x, @2x, @3x from Android Studio
2. Add to `iosApp/GoodtimeInProgress/Assets.xcassets/`
3. Update icon functions:
```swift
private func timerTypeIcon(_ type: GoodtimeActivityAttributes.TimerType) -> some View {
    Image(imageName(for: type))
        .renderingMode(.template) // For tinting
}

private func imageName(for type: GoodtimeActivityAttributes.TimerType) -> String {
    switch type {
    case .focus: return "ic_status_goodtime"
    case .shortBreak, .longBreak: return "ic_break"
    }
}
```

### Recommendation
**Use Option C (PNG/PDF Assets)** for brand consistency while keeping implementation simple.

---

## 2. Minimal View with Circular Progress Bar

### Current State
- Minimal view: `GoodtimeInProgressLiveActivity.swift:94-98`
- Shows only icon, no progress indicator

### Proposed Design
Auto-updating circular progress bar with leading icon centered inside.

### Technical Feasibility

#### ⚠️ Important Limitations
**Dynamic Island minimal presentation does NOT support complex views:**
- Limited to simple icons/text
- No custom drawing or progress views
- Maximum size constraint: very small circular area
- Apple's guidelines: Keep it minimal (icon or short text only)

#### Workarounds

**Option 1: Use Background Color Progress (Subtle)**
```swift
minimal: {
    timerTypeIcon(context.attributes.timerType)
        .font(.caption2)
        .padding(4)
        .background(
            Circle()
                .stroke(Color.white.opacity(0.3), lineWidth: 1)
        )
}
```
**Limitation:** Cannot animate/update the stroke, would be static.

**Option 2: Use Timer Percentage as Badge**
```swift
minimal: {
    ZStack(alignment: .topTrailing) {
        timerTypeIcon(context.attributes.timerType)
            .font(.caption2)

        // Small percentage indicator
        if !context.isStale && context.attributes.isCountdown {
            let progress = calculateProgress(context)
            Text("\(Int(progress * 100))")
                .font(.system(size: 8))
                .offset(x: 4, y: -4)
        }
    }
}
```
**Limitation:** Updates infrequently, may not feel "live".

**Option 3: Use Keyline Tint Animation (Native iOS)**
Already implemented at line 99: `.keylineTint(keylineTint(for: context.attributes.timerType))`
- The colored ring around the minimal view
- Can pulse/animate based on state
- Most iOS-native approach

### Recommendation
**The minimal view is too constrained for a circular progress bar.** Instead:
1. Keep the minimal view simple (icon only) - follows Apple HIG
2. Ensure compact leading/trailing views show progress clearly
3. Consider enhancing the keyline tint to indicate timer state (running vs paused)

---

## 3. Expanded View Changes

### Current State
- Expanded view: `GoodtimeInProgressLiveActivity.swift:33-82`
- Hardcoded strings ("Focus", "Short Break", etc.)
- Icon-based buttons
- Timer in trailing region

### 3a) Use Localized Strings from ComposeApp

#### Available Resources
- Strings: `composeApp/src/commonMain/composeResources/values/strings_main.xml`
- Colors: `composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/Colors.kt`
- Colors are already available as hex strings in `palette` list (24 colors + break color)

#### Implementation Strategy ✅ (RECOMMENDED)

**Pass strings and colors directly through LiveActivityBridge!**

This is the cleanest approach since:
1. You already have a working bridge (`LiveActivityBridge` + `IosLiveActivityListener`)
2. Strings are already localized in Kotlin (via `getString(Res.string.x)`)
3. Colors are already defined as hex strings in `Colors.kt`
4. No duplication needed - single source of truth

**Changes Required:**

**Step 1: Update Event.Start to include label color**
```kotlin
// In composeApp/src/commonMain/kotlin/.../Event.kt
sealed class Event {
    data class Start(
        val isFocus: Boolean,
        val autoStarted: Boolean = false,
        val endTime: Long,
        val labelName: String = "",
        val isDefaultLabel: Boolean = true,
        val labelColorHex: String = "",  // ADD THIS
        val isBreakEnabled: Boolean = true,
        val isCountdown: Boolean = false
    ) : Event()
    // ...
}
```

**Step 2: Create a data class for strings**
```kotlin
// In composeApp/src/iosMain/kotlin/.../LiveActivityBridge.ios.kt

data class LiveActivityStrings(
    val pause: String,
    val resume: String,
    val stop: String,
    val startFocus: String,
    val startBreak: String,
    val plusOneMin: String,
    val focusInProgress: String,
    val focusPaused: String,
    val breakInProgress: String,
)

class LiveActivityBridge {
    // ...

    fun start(
        isFocus: Boolean,
        isCountdown: Boolean,
        durationSeconds: Long,
        labelName: String,
        isDefaultLabel: Boolean,
        labelColorHex: String,
        strings: LiveActivityStrings,  // Single parameter for all strings!
    ) {
        val timerType = if (isFocus) 0 else 1

        delegate?.startActivity(
            timerType = timerType,
            isCountdown = isCountdown,
            duration = durationSeconds.toDouble(),
            labelName = labelName,
            isDefaultLabel = isDefaultLabel,
            labelColorHex = labelColorHex,
            strings = strings,
        )
    }
}

interface LiveActivityDelegate {
    fun startActivity(
        timerType: Int,
        isCountdown: Boolean,
        duration: Double,
        labelName: String,
        isDefaultLabel: Bool,
        labelColorHex: String,
        strings: LiveActivityStrings,
    )
    // ...
}
```

**Step 3: Update IosLiveActivityListener to create strings object**
```kotlin
// In composeApp/src/iosMain/kotlin/.../IosLiveActivityListener.kt
class IosLiveActivityListener(
    private val liveActivityBridge: LiveActivityBridge,
    private val timeProvider: TimeProvider,
    private val log: Logger,
) : EventListener {
    override suspend fun onEvent(event: Event) {
        // ...
        when (event) {
            is Event.Start -> {
                // Get label color from palette
                val labelColorHex = if (!event.isDefaultLabel && event.labelColorIndex >= 0) {
                    palette[event.labelColorIndex.toInt()]
                } else {
                    ""
                }

                // Create localized strings object
                val strings = LiveActivityStrings(
                    pause = getString(Res.string.main_pause),
                    resume = getString(Res.string.main_resume),
                    stop = getString(Res.string.main_stop),
                    startFocus = getString(Res.string.main_start_focus),
                    startBreak = getString(Res.string.main_start_break),
                    plusOneMin = getString(Res.string.main_plus_1_min),
                    focusInProgress = getString(Res.string.main_focus_session_in_progress),
                    focusPaused = getString(Res.string.main_focus_session_paused),
                    breakInProgress = getString(Res.string.main_break_in_progress),
                )

                liveActivityBridge.start(
                    isFocus = event.isFocus,
                    isCountdown = isCountdown,
                    durationSeconds = durationSeconds,
                    labelName = event.labelName,
                    isDefaultLabel = event.isDefaultLabel,
                    labelColorHex = labelColorHex,
                    strings = strings,  // Pass the whole object!
                )
            }
        }
    }
}
```

**Step 4: Update GoodtimeActivityAttributes to store strings + color**
```swift
// In iosApp/GoodtimeInProgress/GoodtimeActivityAttributes.swift
struct GoodtimeActivityAttributes: ActivityAttributes {
    let timerType: TimerType
    let isCountdown: Bool
    let labelName: String
    let isDefaultLabel: Bool
    let labelColorHex: String  // ADD THIS

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

    // ...
}
```

**Step 5: Update GoodtimeLiveActivityManager**
```swift
// In iosApp/iosApp/GoodtimeLiveActivityManager.swift
func startActivity(
    timerType: Int32,
    isCountdown: Bool,
    duration: Double,
    labelName: String,
    isDefaultLabel: Bool,
    labelColorHex: String,  // ADD THIS
    // Strings
    strPause: String,
    strResume: String,
    strStop: String,
    strStartFocus: String,
    strStartBreak: String,
    strPlusOneMin: String,
    strFocusInProgress: String,
    strFocusPaused: String,
    strBreakInProgress: String
) {
    // Create attributes with all the data
    let attributes = GoodtimeActivityAttributes(
        timerType: timerType == 0 ? .focus : .shortBreak,
        isCountdown: isCountdown,
        labelName: labelName,
        isDefaultLabel: isDefaultLabel,
        labelColorHex: labelColorHex,
        strPause: strPause,
        strResume: strResume,
        // ... etc
    )
}
```

**Step 6: Use the strings in the Live Activity**
```swift
// In GoodtimeInProgressLiveActivity.swift
private func stateText(for context: ActivityViewContext<GoodtimeActivityAttributes>) -> String {
    let type = context.attributes.timerType
    let isPaused = context.state.isPaused

    if type == .focus {
        return isPaused
            ? context.attributes.strFocusPaused
            : context.attributes.strFocusInProgress
    } else {
        return context.attributes.strBreakInProgress
    }
}
```

**Benefits of This Approach:**
- ✅ Single source of truth for strings (Compose resources)
- ✅ Automatic localization (uses device language)
- ✅ No string duplication
- ✅ Colors come directly from `Colors.kt` palette
- ✅ Easy to maintain and update

### 3b) Label Name and Color Support

#### Current Implementation
- Label name: `GoodtimeActivityAttributes.swift:27` - `labelName: String`
- Is default label: `GoodtimeActivityAttributes.swift:28` - `isDefaultLabel: Bool`
- **Missing:** Label color information

#### Implementation (Already covered in 3a)

Since we're passing the color hex string directly through the bridge (see Step 4 in section 3a), we just need to:

**1. Parse the hex string to Color**
```swift
// Add color extension to parse hex strings
extension Color {
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
```

**2. Update Expanded Center Region**
```swift
// In GoodtimeInProgressLiveActivity.swift:45-69
DynamicIslandExpandedRegion(.center) {
    if context.isStale {
        // ... existing stale state
    } else {
        // If custom label, show colored icon + name
        if !context.attributes.isDefaultLabel && !context.attributes.labelColorHex.isEmpty {
            HStack(spacing: 4) {
                // Label icon tinted with label color
                Image(systemName: "tag.fill")  // Or use custom asset
                    .font(.caption)
                    .foregroundColor(Color(hex: context.attributes.labelColorHex))

                Text(context.attributes.labelName)
                    .font(.headline)
                    .fontWeight(.semibold)
            }
        } else {
            // Default label: show timer type
            Text(context.attributes.strFocusInProgress)  // Use localized string
                .font(.headline)
                .fontWeight(.semibold)
        }
    }
}
```

### 3c) Text Buttons Instead of Icon Buttons

#### Current Implementation
- Icon-based buttons: `GoodtimeActionButtons` (line 323-383)
- Uses SF Symbol icons in rounded rectangles

#### Android Reference
From `NotificationArchManager.kt:128-191`:
- Focus running: "Pause", "+1 min", "Start break"
- Focus paused: "Resume", "Stop", "Start break"
- Break: "Stop", "+1 min", "Start focus"

#### Proposed Change
```swift
// Update GoodtimeActionButtons in GoodtimeInProgressLiveActivity.swift

struct GoodtimeActionButtons: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>
    var style: ButtonStyle = .dynamicIsland

    enum ButtonStyle {
        case dynamicIsland
        case lockScreen
    }

    var body: some View {
        if context.isStale {
            EmptyView()
        } else {
            let timerType = context.attributes.timerType
            let isPaused = context.state.isPaused
            let isCountdown = context.attributes.isCountdown

            HStack(spacing: style == .lockScreen ? 8 : 6) {
                if isCountdown {
                    if timerType == .focus {
                        if isPaused {
                            // Paused Focus: Resume, Stop, Start Break
                            textButton(context.attributes.strResume,
                                      intent: GoodtimeTogglePauseIntent())
                            textButton(context.attributes.strStop,
                                      intent: GoodtimeStopIntent())
                            textButton(context.attributes.strStartBreak,
                                      intent: GoodtimeStartBreakIntent())
                        } else {
                            // Running Focus: Pause, +1 Min, Start Break
                            textButton(context.attributes.strPause,
                                      intent: GoodtimeTogglePauseIntent())
                            textButton(context.attributes.strPlusOneMin,
                                      intent: GoodtimeAddMinuteIntent())
                            textButton(context.attributes.strStartBreak,
                                      intent: GoodtimeStartBreakIntent())
                        }
                    } else {
                        // Break: Stop, +1 Min, Start Focus
                        textButton(context.attributes.strStop,
                                  intent: GoodtimeStopIntent())
                        textButton(context.attributes.strPlusOneMin,
                                  intent: GoodtimeAddMinuteIntent())
                        textButton(context.attributes.strStartFocus,
                                  intent: GoodtimeStartFocusIntent())
                    }
                } else {
                    // Count-up: Just Stop
                    textButton(context.attributes.strStop,
                              intent: GoodtimeStopIntent())
                }
            }
        }
    }

    @ViewBuilder
    private func textButton<Intent: AppIntent>(_ title: String, intent: Intent) -> some View {
        Button(intent: intent) {
            Text(title)
                .font(style == .lockScreen ? .body : .caption)
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
```

### 3d) Larger Trailing Timer with Margin

#### Current Implementation
- Timer display: `GoodtimeTimerDisplay` (line 149-208)
- Expanded font: `.title2` (line 191)
- No specific margin handling

#### Issue
Timer is clipped by Dynamic Island rounding (top-right corner)

#### Solution
```swift
// In GoodtimeInProgressLiveActivity.swift:41-43
DynamicIslandExpandedRegion(.trailing) {
    GoodtimeTimerDisplay(context: context, style: .expanded)
        .font(.title)  // INCREASE from .title2 to .title
        .fontWeight(.semibold)  // ADD weight
        .padding(.trailing, 8)  // ADD trailing margin
        .padding(.top, 4)       // ADD top margin to prevent clipping
}

// Update GoodtimeTimerDisplay fontForStyle:
private var fontForStyle: Font {
    switch style {
    case .compact: return .caption2
    case .expanded: return .title  // CHANGED from .title2
    case .lockScreen: return .title
    }
}
```

---

## 4. Lock Screen View Updates

### Current State
- Lock screen view: `GoodtimeLockScreenView` (line 212-319)
- Already includes: icon, timer, state text, label name (if not default)
- Action buttons at bottom

### Goal
Resemble the expanded Dynamic Island view for consistency

### Proposed Changes

#### Update Layout to Match Expanded View
```swift
struct GoodtimeLockScreenView: View {
    let context: ActivityViewContext<GoodtimeActivityAttributes>

    var body: some View {
        Group {
            if context.isStale {
                // ... existing stale state UI
            } else {
                VStack(spacing: 16) {
                    HStack(spacing: 20) {
                        // Leading: Timer type icon
                        Image(imageName(for: context.attributes.timerType))
                            .resizable()
                            .renderingMode(.template)
                            .frame(width: 40, height: 40)
                            .foregroundColor(.white)

                        // Center: Label or Timer Type
                        VStack(alignment: .leading, spacing: 4) {
                            if !context.attributes.isDefaultLabel && !context.attributes.labelColorHex.isEmpty {
                                // Custom label: show label icon (tinted) + name
                                HStack(spacing: 6) {
                                    Image(systemName: "tag.fill")  // Or use custom label icon asset
                                        .font(.caption)
                                        .foregroundColor(Color(hex: context.attributes.labelColorHex))

                                    Text(context.attributes.labelName)
                                        .font(.headline)
                                        .foregroundColor(.white)
                                }
                            } else {
                                // Default label: show timer type text
                                Text(timerTypeLabel)
                                    .font(.headline)
                                    .foregroundColor(.white)
                            }

                            // State text (paused/in progress)
                            Text(stateText)
                                .font(.caption)
                                .foregroundColor(.white.opacity(0.8))
                        }

                        Spacer()

                        // Trailing: Timer (large)
                        GoodtimeTimerDisplay(context: context, style: .lockScreen)
                            .font(.system(size: 32))
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal)
                    .padding(.top)

                    // Bottom: Text buttons (matching expanded view)
                    GoodtimeActionButtons(context: context, style: .lockScreen)
                        .padding(.horizontal)
                        .padding(.bottom)
                }
            }
        }
        .background(Color.black)  // Always black background
    }

    private func imageName(for type: GoodtimeActivityAttributes.TimerType) -> String {
        switch type {
        case .focus: return "ic_status_goodtime"
        case .shortBreak, .longBreak: return "ic_break"
        }
    }

    private var timerTypeLabel: String {
        let type = context.attributes.timerType
        let isPaused = context.state.isPaused

        if type == .focus {
            return isPaused
                ? context.attributes.strFocusPaused
                : context.attributes.strFocusInProgress
        } else {
            return context.attributes.strBreakInProgress
        }
    }

    private var stateText: String {
        if context.state.isPaused {
            return context.attributes.strFocusPaused
        }
        return context.attributes.strFocusInProgress
    }
}
```

---

## Implementation Checklist

### Phase 1: Bridge Updates (Kotlin/KMP)
- [ ] Add `labelColorHex` to `Event.Start` data class
- [ ] Update `IosLiveActivityListener` to get localized strings via `getString(Res.string.x)`
- [ ] Update `IosLiveActivityListener` to get label color hex from `palette` list
- [ ] Update `LiveActivityBridge.start()` to accept color hex + all string parameters
- [ ] Update `LiveActivityDelegate` interface to include all new parameters
- [ ] Find where `Event.Start` is created and add label color data

### Phase 2: iOS Foundation
- [ ] Export custom icons (ic_status_goodtime, ic_break) as PDF/PNG assets (optional)
- [ ] **Export/add custom label icon** to `iosApp/GoodtimeInProgress/Assets.xcassets/` (for tinting with label color)
- [ ] Add focus/break timer icons to `iosApp/GoodtimeInProgress/Assets.xcassets/` (if using custom icons)
- [ ] Add `Color(hex: String)` extension to parse hex color strings
- [ ] Update `GoodtimeActivityAttributes` to include `labelColorHex` and all string fields
- [ ] Update `GoodtimeLiveActivityManager.startActivity()` to accept all new parameters
- [ ] Pass all new data to `ActivityAttributes` initialization

### Phase 3: Expanded View
- [ ] Update center region to show label name + color or timer type
- [ ] Convert action buttons from icons to text
- [ ] Increase timer font size and add margins
- [ ] Test layout on different iPhone models (with/without Dynamic Island)

### Phase 4: Lock Screen
- [ ] Update lock screen layout to match expanded view structure
- [ ] Set background to always be black (remove color-based backgrounds)
- [ ] Add tinted label icon next to label name (when custom label is active)
- [ ] Remove count-up/countdown display text
- [ ] Replace icon buttons with text buttons
- [ ] Test on lock screen and notification banner

### Phase 5: Testing
- [ ] Test with default label (no label name shown)
- [ ] Test with custom labels (verify name and color appear)
- [ ] Test countdown vs count-up modes
- [ ] Test paused vs running states
- [ ] Test all button actions work correctly
- [ ] Verify timer updates smoothly
- [ ] Check text doesn't clip on any screen size
- [ ] Test all supported locales

---

## Technical Considerations

### Live Activity Update Frequency
- Live Activities update on a budget system
- Timer text updates automatically using `Text(timerInterval:countsDown:)`
- State changes (pause/resume) require explicit updates via `activity.update()`

### Widget Extension Bundle
- Widget extensions have limited memory
- Cannot link to main app's frameworks directly
- Shared code should be in a framework target included in both

### Data Synchronization
- **Critical:** Kotlin/KMP bridge must pass label color index
- Update at: Where `startActivity()` is called from Kotlin side
- Likely in: Timer state observer in iOS bridge code

### Asset Catalog Benefits
- Use PDF vectors for resolution independence
- Xcode auto-generates @1x, @2x, @3x
- Supports dark mode variants if needed

### Localization Strategy
- Keep string keys identical to Android for consistency
- Consider automated extraction from XML in build phase
- For now: Manual sync acceptable (small set of strings)

---

## Questions to Resolve

1. **Custom Icons:** Confirm design preference - native SF Symbols vs custom assets?
2. **Minimal View:** Acceptable to keep simple icon-only (per Apple HIG)?
3. **Label Colors:** Need exact color hex values from Android Material theme
4. **Button Labels:** Confirm exact matching with Android or iOS-style adaptations?
5. **Localization:** Automate string extraction or manual sync for now?

---

## References

**Code Locations:**
- Live Activity widget: `iosApp/GoodtimeInProgress/GoodtimeInProgressLiveActivity.swift`
- Activity attributes: `iosApp/GoodtimeInProgress/GoodtimeActivityAttributes.swift`
- Activity manager: `iosApp/iosApp/GoodtimeLiveActivityManager.swift`
- Android notification: `composeApp/src/androidMain/kotlin/.../NotificationArchManager.kt:72-193`
- String resources: `composeApp/src/commonMain/composeResources/values/strings_main.xml`
- Label colors: `composeApp/src/commonMain/kotlin/.../ApplicationTheme.kt:66`
- Custom icons: `composeApp/src/commonMain/composeResources/drawable/`

**Apple Documentation:**
- [ActivityKit](https://developer.apple.com/documentation/activitykit)
- [Live Activities](https://developer.apple.com/documentation/activitykit/displaying-live-data-with-live-activities)
- [WidgetKit](https://developer.apple.com/documentation/widgetkit)

---

**End of Plan Document**