# iOS Background Execution Design Document
> *Note: This design assumes Live Activity buttons are implemented. If you ever decide to bring buttons to the Live Activity, this document serves as the implementation blueprint.*

## Current State Analysis

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Host App (Kotlin)                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  TimerManager                                                         │   │
│  │  ──────────────────────                                               │   │
│  │  • Core timer logic                                                   │   │
│  │  • Session tracking & statistics (Room)                               │   │
│  │  • State restoration on app launch                                    │   │
│  │  • Device reboot handling                                             │   │
│  │  • Expired timer detection                                            │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  EventListeners                                                       │   │
│  │  ──────────────────────                                               │   │
│  │  • IosTimerStatePersistenceListener (saves to DataStore)             │   │
│  │  • IosLiveActivityListener (updates Live Activity via Bridge)        │   │
│  │  • IosNotificationHandler (schedules UNUserNotification)             │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                          │
└────────────────────────────────────┼──────────────────────────────────────────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │                                 │
         ┌──────────▼──────────┐         ┌───────────▼──────────┐
         │  ActivityKit         │         │  UNUserNotification   │
         │  (Live Activities)   │         │  (Timer finished)     │
         └──────────────────────┘         └──────────────────────┘
                    │
         ┌──────────▼──────────┐
         │  GoodtimeInProgress  │
         │  (Extension)         │
         │  ─────────────────── │
         │  • Displays UI       │
         │  • Button taps →     │
         │    NotificationCenter│
         └──────────────────────┘
                    │
         ┌──────────▼──────────┐
         │  LiveActivityIntent │
         │  Handler            │
         │  (Host app only)    │
         └─────────────────────┘
```

### Communication Mechanism (Current)

| Direction | Mechanism | Data Flow |
|-----------|-----------|-----------|
| Host → Extension | ActivityKit `activity.update()` | Pushes `ContentState` updates |
| Extension → Host | NotificationCenter → LiveActivityIntentHandler → LiveActivityIntentBridge → TimerManager | Requires host app running |

### What Works

1. **Timer State Restoration**: App killed → reopened, timer continues showing correct progress
2. **Live Activity Updates**: Extension UI updates in real-time while host app is alive
3. **Notification Scheduling**: Finish notification scheduled on timer start

---

## Problems

### Problem 1: Live Activity Buttons Don't Work When App Killed

**Root Cause**: NotificationCenter is in-memory only. When host app is killed, `LiveActivityIntentHandler` is destroyed.

**User Impact**:
- User swipes to kill app
- Taps Live Activity button (pause/resume/stop/add minute)
- Nothing happens
- UI shows outdated state

### Problem 2: Notification Actions Don't Work When App Killed

**Root Cause**: `IosNotificationDelegate.handleNotificationAction()` calls `onStartNextSession` callback which requires host app alive.

**Current Code** (`IosNotificationHandler.kt:114-136`):
```swift
private fun handleNotificationAction(actionId: String) {
    when (actionId) {
        ACTION_START_NEXT -> {
            coroutineScope.launch {
                onStartNextSession?.invoke()  // ← Host app must be alive
            }
        }
    }
}
```

**User Impact**:
- Timer expires while app is killed
- Notification arrives with "Start Break" action
- User taps action
- Nothing happens

### Problem 3: Cannot Dismiss Live Activity on App Kill

**Reality**: iOS provides NO callback when user force-kills app:
- ❌ No `applicationWillTerminate`
- ❌ No `sceneDidDisconnect`
- ❌ No way to detect force-kill

**This is by design** (Apple security/privacy). The Live Activity and scheduled notifications will continue until:
- Live Activity reaches its `staleDate`
- Notification fires
- User manually dismisses

**Solution**: The extension must be able to handle timer state independently. This is why we need UserDefaults sharing.

---

## Proposed Solution: UserDefaults as Single Source of Truth

### Architecture Overview (New)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   Shared State (UserDefaults + App Group)                   │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  goodtime_timer_state (DomainTimerData equivalent)                    │   │
│  │  goodtime_completed_sessions (array of finished sessions)             │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                              ▲            ▲
                              │            │
                    ┌─────────┴────────────┴─────────┐
                    │                               │
┌───────────────────▼───────────┐   ┌───────────────▼────────────────────┐
│  Host App (Kotlin)            │   │  Extension (Swift)                 │
│  ─────────────────────────    │   │  ──────────────────────────────    │
│  • Reads on launch            │   │  • Reads on button tap             │
│  • Writes on state change     │   │  • Writes on button tap            │
│  • Migrates sessions → Room   │   │  • Can schedule UN notifications   │
│  • Single source of truth     │   │  • Single source of truth          │
└───────────────────────────────┘   └──────────────────────────────────────┘
```

### Key Design Decision: Single Source of Truth

**Before (Dual Source - Complex)**:
```
Host App          Extension
    │                 │
    ▼                 ▼
DataStore       UserDefaults
    │                 │
    └── sync ─────────┘  ← Potential for drift
```

**After (Single Source - Simple)**:
```
Host App ──────────── Extension
    │                     │
    └── UserDefaults ────┘  ← One truth
```

**Changes**:
- `IosTimerStatePersistenceListener` writes to UserDefaults instead of DataStore
- `TimerStateRestoration` reads from UserDefaults (already does!)
- Extension writes to same UserDefaults
- Both processes use identical data structure

**Benefit**: No sync between DataStore and UserDefaults. Less complexity, fewer bugs.

---

## UserDefaults Schema

### Data Structures

```swift
// Keys
struct UserDefaultsKeys {
    static let timerState = "goodtime_timer_state"
    static let completedSessions = "goodtime_completed_sessions"
}

// Full timer state (minimal - extension doesn't need label info)
struct TimerState: Codable {
    let version: Int

    // MARK: - Current Session State
    let timerType: String          // "focus" | "shortBreak" | "longBreak"
    let state: String              // "running" | "paused" | "stopped" | "reset"
    let startTime: Double          // elapsedRealtime (milliseconds)
    let endTime: Double            // elapsedRealtime (milliseconds)
    let lastStartTime: Double      // elapsedRealtime (milliseconds)
    let timeSpentPaused: Double    // milliseconds
    let timeAtPause: Double        // elapsedRealtime (milliseconds)
    let lastPauseTime: Double      // elapsedRealtime (milliseconds)

    // MARK: - Settings (for extension to start next session)
    let focusDuration: Int         // seconds
    let shortBreakDuration: Int    // seconds
    let longBreakDuration: Int     // seconds
    let sessionsBeforeLongBreak: Int
    let currentStreakCount: Int

    // NOTE: Label info (name, color, duration, countdown) is NOT stored here
    // because the extension cannot change the label. Label is set when
    // starting a session from the host app and remains constant throughout
    // the session chain. The extension only needs to continue with the
    // current session's settings.

    // MARK: - Metadata
    let lastUpdateTime: Double     // wall-clock (for conflict resolution)
    let isCountdown: Bool          // convenience
    let pendingAction: String?     // "start_break" | "start_focus" | nil (from notification)
}

// Completed session for statistics (mirrors Session.kt)
// NOTE: Fields match Session.kt except notes (extension can't add notes)
//       and isArchived (always false for now)
struct CompletedSession: Codable {
    let timestamp: Double          // elapsedRealtime (milliseconds) - when session started
    let duration: Int              // minutes - NOT milliseconds
    let interruptions: Int         // minutes - time spent paused
    let label: String              // label name at time of session
    let isWork: Bool               // true = focus, false = break
}
```

### When Data is Written

| Trigger | Writer | What |
|---------|--------|------|
| Timer starts | Host App | Full TimerState (no label fields needed) |
| Timer pauses | Host App OR Extension | Updated TimerState |
| Timer resumes | Host App OR Extension | Updated TimerState |
| Add minute | Host App OR Extension | Updated TimerState |
| Stop | Host App OR Extension | Clear TimerState, write CompletedSession |
| Skip/Finish | Host App OR Extension | Write CompletedSession, write new TimerState |
| Settings change | Host App | Updated TimerState with new settings |
| Notification action tapped | Notification Delegate | Write pendingAction, optionally write CompletedSession |

---

## Implementation Details

### 1. Host App: Write to UserDefaults

**New: `IosUserDefaultsTimerStorage.kt`**

```kotlin
/**
 * iOS-specific timer storage using UserDefaults (App Group).
 * Single source of truth for timer state between host app and extension.
 */
class IosUserDefaultsTimerStorage(
    private val timeProvider: TimeProvider,
) {
    private val userDefaults = NSUserDefaults(suiteName = "group.com.apps.adrcotfas.goodtime")

    fun writeTimerState(data: DomainTimerData, currentLabel: Label) {
        val state = TimerState(
            version = 1,
            timerType = data.runtime.type.name,
            state = data.runtime.state.name.lowercase(),
            startTime = data.runtime.startTime.toDouble(),
            endTime = data.runtime.endTime.toDouble(),
            lastStartTime = data.runtime.lastStartTime.toDouble(),
            timeSpentPaused = data.runtime.timeSpentPaused.toDouble(),
            timeAtPause = data.runtime.timeAtPause.toDouble(),
            lastPauseTime = data.runtime.lastPauseTime.toDouble(),

            // Settings
            focusDuration = data.timerProfile.focusDuration,
            shortBreakDuration = data.timerProfile.shortBreakDuration,
            longBreakDuration = data.timerProfile.longBreakDuration,
            sessionsBeforeLongBreak = data.longBreakData.sessionsBeforeLongBreak,
            currentStreakCount = data.breakBudgetData.currentStreak,

            // Metadata
            lastUpdateTime = timeProvider.now().toDouble(),
            isCountdown = data.label.isCountdown,
            pendingAction = null,
        )

        val data = JSONEncoder.encode(state)
        userDefaults?.setObject(data, forKey = UserDefaultsKeys.timerState)
        userDefaults?.synchronize()
    }

    fun readTimerState(): TimerState? {
        val data = userDefaults?.data(forKey = UserDefaultsKeys.timerState) ?: return null
        return JSONDecoder.decode(TimerState.self, from: data)
    }

    fun clearTimerState() {
        userDefaults?.removeObjectForKey(UserDefaultsKeys.timerState)
    }

    fun writeCompletedSession(
        runtimeState: TimerRuntimeState,
        label: Label,
        isWork: Boolean
    ) {
        // Convert to Session.kt format
        // duration: minutes (NOT milliseconds)
        // interruptions: minutes (time spent paused)
        val durationMinutes = ((runtimeState.endTime - runtimeState.startTime) / 60000.0).toInt()
        val interruptionsMinutes = (runtimeState.timeSpentPaused / 60000.0).toInt()

        val session = CompletedSession(
            timestamp = runtimeState.startTime,
            duration = durationMinutes,
            interruptions = interruptionsMinutes,
            label = label.name,
            isWork = isWork,
        )

        var sessions = readCompletedSessions().toMutableList()
        sessions.add(session)

        val data = JSONEncoder.encode(sessions)
        userDefaults?.setObject(data, forKey = UserDefaultsKeys.completedSessions)
        userDefaults?.synchronize()
    }

    fun readCompletedSessions(): List<CompletedSession> {
        val data = userDefaults?.data(forKey = UserDefaultsKeys.completedSessions) ?: return emptyList()
        return JSONDecoder.decode([CompletedSession].self, from: data)
    }

    fun clearCompletedSessions() {
        userDefaults?.removeObjectForKey(UserDefaultsKeys.completedSessions)
    }

    // Pending action (from notification)
    fun writePendingAction(action: String) {
        userDefaults?.setObject(action, forKey = "goodtime_pending_action")
        userDefaults?.synchronize()
    }

    fun readPendingAction(): String? {
        return userDefaults?.stringForKey("goodtime_pending_action")
    }

    fun clearPendingAction() {
        userDefaults?.removeObjectForKey("goodtime_pending_action")
    }
}

// UserDefaults keys
object UserDefaultsKeys {
    const val timerState = "goodtime_timer_state"
    const val completedSessions = "goodtime_completed_sessions"
}
```

**Modified: `IosTimerStatePersistenceListener.kt`**

```kotlin
class IosTimerStatePersistenceListener(
    private val userDefaultsStorage: IosUserDefaultsTimerStorage,
    private val localDataRepo: LocalDataRepository,  // For direct Room writes when app is alive
    private val timeProvider: TimeProvider,
    private val coroutineScope: CoroutineScope,
    private val log: Logger,
    private val getCurrentLabel: () -> Label,  // NEW: way to get current label
) : EventListener {
    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start -> {
                persistTimerState(event.domainTimerData, event.label)
            }
            is Event.Pause -> {
                persistTimerState(event.domainTimerData, event.label)
            }
            is Event.Finished -> {
                // Timer completed naturally - store session if >= 1 min
                event.domainTimerData?.let { data ->
                    event.label?.let { label ->
                        maybeStoreAndClearSession(data, label)
                    }
                }
            }
            is Event.Reset -> {
                // User pressed "stop" - read from UserDefaults before it's cleared
                handleResetEvent()
            }
            else -> {
                // Other events don't affect persistence
            }
        }
    }

    private fun persistTimerState(
        domainTimerData: DomainTimerData,
        label: Label,
    ) {
        if (!domainTimerData.runtime.state.isActive) {
            return
        }

        // Write to UserDefaults
        userDefaultsStorage.writeTimerState(domainTimerData, label)
        log.v { "Timer state persisted to UserDefaults" }
    }

    private fun handleResetEvent() {
        // Read current state from UserDefaults before TimerManager clears it
        val currentState = userDefaultsStorage.readTimerState()

        if (currentState != null) {
            // Get current label (from the running session)
            val currentLabel = getCurrentLabel()
            val label = currentLabel

            // Convert TimerState to DomainTimerData-like structure
            val runtime = TimerRuntimeState(
                type = TimerType.valueOf(currentState.timerType.uppercase()),
                state = TimerState.valueOf(currentState.state.uppercase()),
                startTime = currentState.startTime.toLong(),
                endTime = currentState.endTime.toLong(),
                lastStartTime = currentState.lastStartTime.toLong(),
                timeSpentPaused = currentState.timeSpentPaused.toLong(),
                timeAtPause = currentState.timeAtPause.toLong(),
                lastPauseTime = currentState.lastPauseTime.toLong(),
            )

            val domainTimerData = createDomainTimerDataFromState(currentState, runtime, label)
            maybeStoreAndClearSession(domainTimerData, label)
        } else {
            // No state in UserDefaults, just clear
            userDefaultsStorage.clearTimerState()
        }
    }

    private fun maybeStoreAndClearSession(
        domainTimerData: DomainTimerData,
        label: Label,
    ) {
        // Mirror the logic from TimerManager.createFinishedSession()
        val runtime = domainTimerData.runtime
        val timerType = domainTimerData.type
        val isFocus = timerType == TimerType.FOCUS

        // Calculate duration (mirror TimerManager logic)
        val totalDuration = runtime.endTime - runtime.startTime
        val interruptions = runtime.timeSpentPaused

        val durationToSave = totalDuration.let { duration ->
            // For focus: subtract interruptions; for breaks: keep total
            if (isFocus) duration - interruptions else duration
        } + WIGGLE_ROOM_MILLIS

        val durationMinutes = (durationToSave / 60_000).toInt()

        if (durationMinutes < 1) {
            log.i { "Session was shorter than 1 minute, not storing" }
            userDefaultsStorage.clearTimerState()
            return
        }

        // Calculate timestamp (convert elapsedRealtime to wall-clock)
        val now = timeProvider.now()
        val elapsedRealtime = timeProvider.elapsedRealtime()
        val timestampAtEnd = now - elapsedRealtime + runtime.endTime

        // Create session matching Session.kt format
        val session = CompletedSession(
            timestamp = timestampAtEnd,
            duration = durationMinutes,
            interruptions = if (isFocus) (interruptions / 60_000).toInt() else 0,
            label = label.name,
            isWork = isFocus,
        )

        // Store to UserDefaults (will be migrated to Room on app launch)
        userDefaultsStorage.writeCompletedSession(session)
        log.i { "Session stored to UserDefaults: duration=$durationMinutes minutes" }

        // Clear timer state
        userDefaultsStorage.clearTimerState()
    }

    companion object {
        private const val WIGGLE_ROOM_MILLIS = 10000L  // 10 seconds buffer (matches TimerManager)
    }
}
```

**Note**: The `handleResetEvent()` reads from UserDefaults because `Event.Reset` can't carry data (it's an `object`, not a `data class`). This is necessary because the stop action can come from the extension when the app is killed, and we need to capture the session data before it's cleared.

**Modified: `Event.kt`**

```kotlin
sealed class Event {
    data class Start(
        val isFocus: Boolean = true,
        val autoStarted: Boolean = false,
        val endTime: Long = 0,
        val labelName: String = Label.DEFAULT_LABEL_NAME,
        val isDefaultLabel: Boolean = true,
        val labelColorIndex: Int = Label.DEFAULT_LABEL_COLOR_INDEX,
        val isBreakEnabled: Boolean = true,
        val isCountdown: Boolean = true,
        val runtimeState: TimerRuntimeState = TimerRuntimeState(),
        // NEW: Add full data for UserDefaults storage
        val domainTimerData: DomainTimerData? = null,
        val label: Label? = null,
    ) : Event()

    data class Pause(
        val runtimeState: TimerRuntimeState = TimerRuntimeState(),
        // NEW: Add full data for UserDefaults storage
        val domainTimerData: DomainTimerData? = null,
        val label: Label? = null,
    ) : Event()

    data class AddOneMinute(
        val endTime: Long,
    ) : Event()

    data class Finished(
        val type: TimerType,
        val autostartNextSession: Boolean = false,
        // NEW: Add full data for UserDefaults storage
        val domainTimerData: DomainTimerData? = null,
        val label: Label? = null,
    ) : Event()

    data object Reset : Event()  // Note: Reset uses current timerData from TimerManager

    data class SendToBackground(
        val isTimerRunning: Boolean,
        val endTime: Long,
    ) : Event()

    data object BringToForeground : Event()

    data object UpdateActiveLabel : Event()
}
```

**Note**: For `Event.Reset`, we can't include `DomainTimerData` in the event itself since it's an `object`. Instead, the `IosTimerStatePersistenceListener` can access the current state via a different mechanism (e.g., by reading from UserDefaults after the reset, or by having a reference to the current `TimerData`).

**Alternative approach for Event.Reset**: Instead of using the event's data, read from UserDefaults before clearing:

```kotlin
is Event.Reset -> {
    // Read current state from UserDefaults before it's cleared by TimerManager
    val currentState = userDefaultsStorage.readTimerState()
    val currentLabel = getCurrentLabel()  // Need a way to get this

    if (currentState != null && currentLabel != null) {
        maybeStoreAndClearSession(currentState, currentLabel)
    } else {
        userDefaultsStorage.clearTimerState()
    }
}
```

**Modified: `TimerManager.kt`**

Need to pass `DomainTimerData` and `Label` to EventListener events:

```kotlin
// In Event.Start
Event.Start(
    isFocus = ...,
    // ... existing fields ...
    domainTimerData = _timerData.value,
    label = _timerData.value.label.label,
)

// In Event.Pause
Event.Pause(
    runtimeState = updatedRuntimeState,
    domainTimerData = _timerData.value,
    label = _timerData.value.label.label,
)

// In Event.Finished
Event.Finished(
    type = type,
    autostartNextSession = autoStart,
    domainTimerData = _timerData.value,
    label = _timerData.value.label.label,
)

// Event.Reset remains an object (no data)

### 2. Host App: Read from UserDefaults on Launch

**Modified: `TimerStateRestoration.kt`**

```kotlin
class TimerStateRestoration(
    private val userDefaultsStorage: IosUserDefaultsTimerStorage,  // NEW
    private val localDataRepo: LocalDataRepository,
    private val timeProvider: TimeProvider,
    private val log: Logger,
    private val coroutineScope: CoroutineScope,
) {
    fun restoreTimerState(updateTimerData: (TimerRuntimeState) -> Unit) {
        // Only restore state on iOS
        if (getPlatformConfiguration().isAndroid) {
            return
        }

        coroutineScope.launch {
            // NEW: Read from UserDefaults instead of DataStore
            val state = userDefaultsStorage.readTimerState() ?: return@launch

            // Check if timer expired while app was killed
            if (isTimerExpired(state)) {
                log.i { "Timer already expired, not restoring state" }
                // Store completed session if needed
                maybeStoreCompletedSession(state)
                userDefaultsStorage.clearTimerState()
                return@launch
            }

            // Check if device rebooted
            val deviceRebooted = hasDeviceRebooted(state)

            val runtimeState = if (deviceRebooted) {
                recalculateTimesAfterReboot(state)
            } else {
                state.toRuntimeState()
            }

            updateTimerData(runtimeState)

            // Migrate completed sessions
            migrateCompletedSessions()

            log.i { "Timer state restored from UserDefaults" }
        }
    }

    private fun isTimerExpired(state: TimerState): Boolean {
        val now = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.now()

        // Only countdown timers can expire
        if (!state.isCountdown || state.state != "running") {
            return false
        }

        // Convert elapsedRealtime endTime to wall-clock for comparison
        val elapsedNow = timeProvider.elapsedRealtime()
        return elapsedNow >= state.endTime
    }

    private fun hasDeviceRebooted(state: TimerState): Boolean {
        val elapsedRealtime = timeProvider.elapsedRealtime()
        return elapsedRealtime < state.startTime
    }

    private fun recalculateTimesAfterReboot(state: TimerState): TimerRuntimeState {
        // Recalculate based on wall-clock times
        // (implementation similar to existing)
        // ...
    }

    private fun maybeStoreCompletedSession(state: TimerState) {
        // Check if session is long enough (> 1 minute)
        val duration = state.endTime - state.startTime
        if (duration >= 60_000) {  // 1 minute in milliseconds
            val session = CompletedSession(
                startTime = state.startTime,
                endTime = state.endTime,
                duration = duration,
                timerType = state.timerType,
                labelName = state.labelName,
                completedAt = timeProvider.now()
            )
            userDefaultsStorage.writeCompletedSession(session)
        }
    }

    private fun migrateCompletedSessions() {
        val sessions = userDefaultsStorage.readCompletedSessions()
        if (sessions.isEmpty()) return

        log.i { "Migrating ${sessions.size} completed sessions from UserDefaults to Room" }

        sessions.forEach { session ->
            // CompletedSession already matches Session.kt format
            val roomSession = Session(
                id = 0,
                timestamp = session.timestamp.toLong(),
                duration = session.duration.toLong(),      // already in minutes
                interruptions = session.interruptions.toLong(),  // already in minutes
                label = session.label,
                notes = "",  // extension can't add notes
                isWork = session.isWork,
                isArchived = false,
            )
            localDataRepo.insertSession(roomSession)
        }

        userDefaultsStorage.clearCompletedSessions()
    }
}
```

### 3. Extension: Read and Write UserDefaults

**New: `TimerStateManager.swift` (in extension target)**

```swift
import Foundation

@objc public class TimerStateManager: NSObject {
    @objc public static let shared = TimerStateManager()

    private let userDefaults = UserDefaults(suiteName: "group.com.apps.adrcotfas.goodtime")

    // Current label (set when Live Activity starts, remains constant)
    // Extension cannot change label, only host app can
    private var currentLabel: String = Label.DEFAULT_LABEL_NAME
    private var currentLabelColor: String = ""

    // MARK: - Set Label (called when Live Activity starts)

    @objc public func setLabel(name: String, color: String) {
        currentLabel = name
        currentLabelColor = color
    }

    // MARK: - Read State

    @objc public func getCurrentState() -> TimerState? {
        guard let data = userDefaults?.data(forKey: UserDefaultsKeys.timerState),
              let state = try? JSONDecoder().decode(TimerState.self, from: data) else {
            return nil
        }
        return state
    }

    // MARK: - Update State

    @objc public func updateState(_ action: TimerAction) -> TimerState? {
        guard var state = getCurrentState() else { return nil }

        let now = Date().timeIntervalSince1970
        let elapsedRealtime = CACurrentMediaTime() * 1000 // milliseconds

        switch action {
        case .togglePause:
            if state.state == "running" {
                // Pause
                state.state = "paused"
                state.timeAtPause = elapsedRealtime
                state.lastPauseTime = elapsedRealtime
            } else {
                // Resume: recalculate end time
                let pausedDuration = elapsedRealtime - state.timeAtPause
                state.endTime += pausedDuration
                state.timeSpentPaused += pausedDuration
                state.state = "running"
            }

        case .addMinute:
            state.endTime += 60_000  // 1 minute in milliseconds

        case .stop:
            // Store completed session if long enough
            if shouldStoreSession(state: state) {
                storeCompletedSession(from: state)
            }
            clearTimerState()
            return nil

        case .skip:
            // Complete current session and start next
            if shouldStoreSession(state: state) {
                storeCompletedSession(from: state)
            }
            return startNextSession(from: state)
        }

        state.lastUpdateTime = now
        saveState(state)
        return state
    }

    // MARK: - Start Next Session

    private func startNextSession(from currentState: TimerState) -> TimerState {
        // Determine next session type
        let nextType: String
        var nextStreakCount = currentState.currentStreakCount

        if currentState.timerType == "focus" {
            // After focus, start a break
            if (nextStreakCount + 1) >= currentState.sessionsBeforeLongBreak {
                nextType = "longBreak"
            } else {
                nextType = "shortBreak"
            }
            nextStreakCount += 1
        } else {
            // After break, start focus
            nextType = "focus"
            nextStreakCount = 0
        }

        // Calculate duration
        // NOTE: Extension doesn't have label-specific duration since label fields
        // were removed from TimerState. Uses default durations based on session type.
        let duration: Int
        switch nextType {
        case "focus":
            duration = currentState.focusDuration
        case "shortBreak":
            duration = currentState.shortBreakDuration
        case "longBreak":
            duration = currentState.longBreakDuration
        default:
            duration = currentState.focusDuration
        }

        let now = CACurrentMediaTime() * 1000
        let nowWall = Date().timeIntervalSince1970

        let newState = TimerState(
            version: 1,
            timerType: nextType,
            state: "running",
            startTime: now,
            endTime: now + Double(duration * 1000),
            lastStartTime: now,
            timeSpentPaused: 0,
            timeAtPause: 0,
            lastPauseTime: 0,

            // Settings (unchanged)
            focusDuration: currentState.focusDuration,
            shortBreakDuration: currentState.shortBreakDuration,
            longBreakDuration: currentState.longBreakDuration,
            sessionsBeforeLongBreak: currentState.sessionsBeforeLongBreak,
            currentStreakCount: nextStreakCount,

            // Metadata
            lastUpdateTime: nowWall,
            isCountdown: currentState.isCountdown,
            pendingAction: nil
        )

        saveState(newState)
        return newState
    }

    // MARK: - Check Timer Completion

    @objc public func checkTimerCompletion() -> Bool {
        guard let state = getCurrentState(),
              state.state == "running",
              state.isCountdown else { return false }

        let now = CACurrentMediaTime() * 1000
        if now >= state.endTime {
            // Timer completed
            storeCompletedSession(from: state)
            clearTimerState()
            return true
        }
        return false
    }

    // MARK: - Completed Sessions

    private func shouldStoreSession(state: TimerState) -> Bool {
        let duration = state.endTime - state.startTime
        return duration >= 60_000 // 1 minute in milliseconds
    }

    private func storeCompletedSession(from state: TimerState) {
        // Convert to Session.kt format
        // duration: minutes (NOT milliseconds)
        // interruptions: minutes (time spent paused)
        // isWork: true for focus, false for breaks
        let durationMinutes = Int((state.endTime - state.startTime) / 60000)
        let interruptionsMinutes = Int(state.timeSpentPaused / 60000)
        let isWork = state.timerType == "focus"

        let session = CompletedSession(
            timestamp: state.startTime,
            duration: durationMinutes,
            interruptions: interruptionsMinutes,
            label: currentLabel,  // Use stored label from Live Activity start
            isWork: isWork
        )

        var sessions = getCompletedSessions()
        sessions.append(session)

        if let data = try? JSONEncoder().encode(sessions) {
            userDefaults?.set(data, forKey: UserDefaultsKeys.completedSessions)
        }
    }

    @objc public func getCompletedSessions() -> [CompletedSession] {
        guard let data = userDefaults?.data(forKey: UserDefaultsKeys.completedSessions),
              let sessions = try? JSONDecoder().decode([CompletedSession].self, from: data) else {
            return []
        }
        return sessions
    }

    @objc public func clearCompletedSessions() {
        userDefaults?.removeObject(forKey: UserDefaultsKeys.completedSessions)
    }

    // MARK: - Helpers

    private func saveState(_ state: TimerState) {
        if let data = try? JSONEncoder().encode(state) {
            userDefaults?.set(data, forKey: UserDefaultsKeys.timerState)
            userDefaults?.synchronize()
        }
    }

    private func clearTimerState() {
        userDefaults?.removeObject(forKey: UserDefaultsKeys.timerState)
    }
}

enum TimerAction {
    case togglePause
    case addMinute
    case stop
    case skip
}
```

### 4. AppIntents: Set Label When Live Activity Starts

When the host app starts a Live Activity, it must call `setLabel` on the extension.

**Modified: `IosLiveActivityListener.kt`**

```kotlin
is Event.Start -> {
    log.v { "IosLiveActivityListener: Starting Live Activity (isFocus=${event.isFocus}, countdown=${event.endTime != 0L})" }

    // ... existing code for duration, localizedStrings, etc. ...

    // NEW: Set label in extension before starting Live Activity
    // This is needed so the extension can store completed sessions with the correct label
    TimerStateManager.shared.setLabel(
        name: event.labelName,
        color: labelColorHex
    )

    // Then start the Live Activity (existing code)
    liveActivityBridge.start(
        isFocus = event.isFocus,
        isCountdown = isCountdown,
        durationSeconds = durationSeconds,
        labelName = event.labelName,
        isDefaultLabel = event.isDefaultLabel,
        labelColorHex = labelColorHex,
        localizedStrings = localizedStrings,
    )
}
```

**Note**: The label is set ONCE when the Live Activity starts and remains constant for the entire session chain. The extension cannot change the label - only the host app can.

### 5. AppIntents: Use TimerStateManager

**Modified: `GoodtimeAppIntents.swift`**

```swift
struct GoodtimeTogglePauseIntent: LiveActivityIntent {
    func perform() async throws -> some IntentResult {
        print("[Intent] Toggle Pause/Resume")

        // 1. Update shared state
        let newState = TimerStateManager.shared.updateState(.togglePause)

        // 2. Clear delivered notifications
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()

        // 3. Update Live Activity UI
        if let state = newState {
            await updateLiveActivityUI(from: state)
        }

        // 4. Reschedule notification if needed
        await rescheduleNotificationIfNeeded(for: newState)

        return .result()
    }

    private func updateLiveActivityUI(from state: TimerState) async {
        for activity in Activity<GoodtimeActivityAttributes>.activities {
            let updatedState = mapToContentState(state)
            let staleDate: Date? = state.isCountdown && state.state == "running"
                ? Date(timeIntervalSince1970: (state.endTime - CACurrentMediaTime() * 1000) / 1000)
                : nil

            await activity.update(ActivityContent(state: updatedState, staleDate: staleDate))
        }
    }

    private func rescheduleNotificationIfNeeded(for state: TimerState?) async {
        guard let state = state, state.isCountdown else { return }

        let center = UNUserNotificationCenter.current()

        if state.state == "paused" {
            center.removeAllPendingNotificationRequests()
        } else if state.state == "running" {
            let now = CACurrentMediaTime() * 1000
            let remaining = state.endTime - now

            if remaining > 0 {
                let content = UNMutableNotificationContent()
                content.title = state.timerType == "focus" ? "Focus Complete" : "Break Complete"
                content.body = "Tap to continue"
                content.sound = .default

                let trigger = UNTimeIntervalNotificationTrigger(
                    timeInterval: remaining / 1000,
                    repeats: false
                )

                let request = UNNotificationRequest(
                    identifier: "goodtime_timer_finished",
                    content: content,
                    trigger: trigger
                )

                await center.add(request)
            }
        }
    }

    private func mapToContentState(_ state: TimerState) -> GoodtimeActivityAttributes.ContentState {
        let startDate = Date(timeIntervalSince1970: (state.startTime - CACurrentMediaTime() * 1000) / 1000)
        let endDate = Date(timeIntervalSince1970: (state.endTime - CACurrentMediaTime() * 1000) / 1000)

        return GoodtimeActivityAttributes.ContentState(
            timerStartDate: startDate,
            timerEndDate: endDate,
            isPaused: state.state == "paused",
            isRunning: state.state == "running",
            pausedTimeRemaining: state.state == "paused" ? (state.endTime - CACurrentMediaTime() * 1000) / 1000 : nil,
            pausedElapsedTime: nil
        )
    }
}

// Similar changes for:
// - GoodtimeStopIntent
// - GoodtimeAddMinuteIntent
// - GoodtimeStartBreakIntent (uses .skip action)
// - GoodtimeStartFocusIntent (uses .skip action)
```

---

## Risks and Mitigations

### Risk 1: Race Conditions

**Scenario**: Host app writes state → Extension writes → Host app reads stale data

**Mitigation**:
- Use `lastUpdateTime` timestamp
- Last write wins (simplest approach)
- Acceptable: timer state is eventually consistent

### Risk 2: Settings Drift

**Scenario**: User changes settings in host app while extension has stale copy in UserDefaults

**Mitigation**:
- Host app writes to UserDefaults on every state change AND settings change
- Extension always reads latest settings from UserDefaults before acting
- When app opens, it rewrites UserDefaults with current settings

### Risk 3: Data Loss on App Kill During Write

**Scenario**: App killed while writing to UserDefaults

**Mitigation**:
- UserDefaults writes are atomic for small values
- Use `synchronize()` explicitly for critical writes
- Validate state on read (check for null/invalid values)

### Risk 4: Time Synchronization

**Scenario**: Device time changes (manual adjustment, timezone change, NTP sync)

**Mitigation**:
- Use `CACurrentMediaTime()` for timer calculations (monotonic)
- Store wall-clock time only for "lastUpdateTime"
- Detect large time jumps and invalidate state

### Risk 5: Extension Cannot Start New Sessions (Initially)

**Scenario**: Timer finishes, user taps "Start Break" but extension doesn't have settings

**Mitigation**:
- ✅ SOLVED: Full DomainTimerData in UserDefaults includes all settings
- Extension can start next session independently

---

## Implementation Plan

### Phase 1: Foundation (UserDefaults Storage)
1. Create `IosUserDefaultsTimerStorage.kt`
2. Define `TimerState` and `CompletedSession` Codable structs in Swift
3. Modify `IosTimerStatePersistenceListener` to write to UserDefaults
4. Modify `TimerStateRestoration` to read from UserDefaults
5. Update `TimerManager` to pass `DomainTimerData` to events

### Phase 2: Extension Integration
1. Implement `TimerStateManager.swift` in extension target
2. Modify `GoodtimeAppIntents.swift` to use `TimerStateManager`
3. Implement Live Activity UI updates
4. Implement notification rescheduling
5. Test: kill app, tap all buttons, verify state

### Phase 3: Session Completion & Statistics
1. Implement completed session storage in extension
2. Implement session migration on app launch
3. Test: complete session while killed, verify stats on launch

### Phase 4: Edge Cases
1. Handle timer completion detection in extension
2. Handle app launch from extension-modified state
3. Add time jump detection and handling
4. Test: multiple actions without opening app

### Phase 5: Validation
1. Stress test: rapid button presses
2. Long-running test: timer runs for hours
3. Kill-restart test at various states
4. Statistics accuracy test
5. Settings change test

---

## Open Questions

### ✅ SOLVED: Notification Action (.foreground) Issue

**Problem**: Using `.foreground` opens the app but `onStartNextSession` callback doesn't execute.

**Root Cause**: When app is killed and relaunched from notification, it's a fresh launch. The callback hasn't been set up yet by `IosNotificationHandler.init()`.

**Solution**: Store pending action in UserDefaults during notification callback, then handle it in `TimerStateRestoration`.

### How It Works

**iOS App Lifecycle with .foreground Notification Action:**

```
User taps "Start Break" action (app killed, timer finished)
        │
        ▼
System launches app (Not Running → Inactive)
        │
        ▼
userNotificationCenter(_:didReceive:) called
        │  ← App is still "inactive" here
        │
        ├─ Write "pendingAction: start_break" to UserDefaults
        ├─ Store completed session if needed
        │
        ▼
call completionHandler()
        │
        ▼
App becomes Active
        │
        ▼
TimerStateRestoration.restoreTimerState()
        │
        ├─ Reads UserDefaults state
        ├─ Checks for pendingAction
        ├─ If "start_break": timerManager.finish() + timerManager.next()
        ├─ If "start_focus": timerManager.finish() + timerManager.next()
        ├─ Clears pendingAction
        └─ Continues normal restoration
```

**Key Timing**: The UserDefaults write happens while app is still "inactive", BEFORE the app is fully foreground. This ensures the data is available when `TimerStateRestoration` runs.

### Implementation

**Modified: `IosNotificationDelegate.kt`**

```kotlin
class IosNotificationDelegate(
    private val userDefaultsStorage: IosUserDefaultsTimerStorage,
    private val localDataRepo: LocalDataRepository,
    private val timeProvider: TimeProvider,
    private val log: Logger,
) : UNUserNotificationCenterDelegate {

    private var onStartNextSession: (() -> Unit)? = null

    fun init(onStartNextSession: () -> Unit) {
        this.onStartNextSession = onStartNextSession
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val actionIdentifier = didReceiveNotificationResponse.actionIdentifier

        log.i { "Received notification action: $actionIdentifier" }

        when (actionIdentifier) {
            ACTION_START_NEXT -> {
                handleStartNextAction()
            }
            "com.apple.UNNotificationDefaultActionIdentifier" -> {
                log.i { "User tapped notification body (default action)" }
            }
            else -> {
                log.w { "Unknown action identifier: $actionIdentifier" }
            }
        }

        withCompletionHandler()
    }

    private fun handleStartNextAction() {
        // 1. Read current timer state from UserDefaults
        val currentState = userDefaultsStorage.readTimerState()

        // 2. If timer exists and is completed, store the session
        currentState?.let { state ->
            if (isTimerCompleted(state)) {
                maybeStoreCompletedSession(state)
            }
        }

        // 3. Write pending action to UserDefaults
        val pendingAction = if (currentState?.timerType == "focus") {
            "start_break"
        } else {
            "start_focus"
        }

        userDefaultsStorage.writePendingAction(pendingAction)
        log.i { "Wrote pending action to UserDefaults: $pendingAction" }

        // 4. Set callback for when app becomes active
        // (This will be called by TimerStateRestoration instead)
    }

    private fun isTimerCompleted(state: TimerState): Boolean {
        if (!state.isCountdown || state.state != "running") {
            return false
        }
        val now = CACurrentMediaTime() * 1000
        return now >= state.endTime
    }

    private fun maybeStoreCompletedSession(state: TimerState) {
        val duration = state.endTime - state.startTime
        if (duration >= 60_000) {  // 1 minute
            // Convert to Session.kt format
            val durationMinutes = (duration / 60000.0).toInt()
            val interruptionsMinutes = (state.timeSpentPaused / 60000.0).toInt()
            val isWork = state.timerType == "focus"

            // NOTE: We don't have the label name here since we removed it from TimerState.
            // The notification action is triggered AFTER the timer completes, so we should
            // use a default label or retrieve it from somewhere else.
            // For now, use default label name.
            val session = CompletedSession(
                timestamp = state.startTime,
                duration = durationMinutes,
                interruptions = interruptionsMinutes,
                label = Label.DEFAULT_LABEL_NAME,
                isWork = isWork
            )
            userDefaultsStorage.writeCompletedSession(session)
        }
    }

    companion object {
        private const val ACTION_START_NEXT = "START_NEXT"
    }
}
```

**Note**: The notification delegate doesn't have access to the label name since it was removed from `TimerState`. This is acceptable because:
1. The notification action is a rare case
2. We use the default label name
3. The user can edit the session later in the statistics screen

---

## Important Constants and Limits

### From TimerManager (commonMain)

```kotlin
// Time buffer added when calculating session duration to avoid rounding issues
const val WIGGLE_ROOM_MILLIS = 10000L  // 10 seconds

// Maximum duration for count-up timers (15 hours)
val COUNT_UP_HARD_LIMIT = 900.minutes.inWholeMilliseconds  // 54000 seconds

// Skip autostart if user returns more than 30 minutes after timer was supposed to end
// Also used to dismiss the finished session sheet when idle time exceeds this window
val AUTOSTART_TIMEOUT = 30.minutes.inWholeMilliseconds
```

### From GoodtimeLiveActivityManager (iOS)

```swift
// Apple's recommended Live Activity duration limit
let LIVE_ACTIVITY_COUNT_UP_LIMIT: TimeInterval = 28800  // 8 hours
```

**Why these limits matter**:

1. **WIGGLE_ROOM_MILLIS (10 seconds)**: Added to duration before converting to minutes. Without this, a session of 59.9 seconds would be stored as 0 minutes and discarded. With the buffer, sessions between 50-59 seconds are stored as 1 minute.

2. **COUNT_UP_HARD_LIMIT (15 hours)**: For count-up timers (break budget mode), the timer caps at 15 hours. After this, `TimerForegroundMonitor` automatically resets the timer.

3. **AUTOSTART_TIMEOUT (30 minutes)**: Two purposes:
   - **Auto-start on return**: If the app was in background when timer ended, and user returns within 30 minutes, automatically start the next session. iOS can't auto-start sessions in background, so this is a workaround.
   - **Dismiss finished sheet**: If idle time exceeds 30 minutes, dismiss the finished session sheet automatically.

4. **LIVE_ACTIVITY_COUNT_UP_LIMIT (8 hours)**: Apple recommends Live Activities not exceed 4 hours, but allows up to 8 hours. The iOS app uses 8 hours for count-up timers. After this, the Live Activity becomes stale.

**Interaction with our design**:
- The extension should respect these limits
- When starting a "next session" from extension, use 8 hours as the maximum staleDate for count-up
- Session storage logic must include the 10-second wiggle room
- The 30-minute timeout is handled by the host app (not relevant to extension)

---

## Time Source Compatibility

### Kotlin (iOS) vs Swift

| Kotlin (TimeProvider) | Swift | Equivalence |
|----------------------|-------|-------------|
| `timeProvider.elapsedRealtime()` | `CACurrentMediaTime() * 1000` | ✅ Both use monotonic clock ( CLOCK_MONOTONIC) |
| `timeProvider.now()` | `Date().timeIntervalSince1970 * 1000` | ✅ Both use Unix epoch time (wall-clock) |

**Implementation**:

```kotlin
// IosTimeProvider.kt - Kotlin
override fun elapsedRealtime(): Long = clock_gettime(CLOCK_MONOTONIC)
```

```swift
// Swift - Extension
let elapsedRealtime = CACurrentMediaTime() * 1000  // milliseconds
let wallClock = Date().timeIntervalSince1970 * 1000  // milliseconds
```

**Why this matters**:
- The extension and host app use equivalent time sources
- `elapsedRealtime()` is monotonic (includes sleep time, doesn't change with time adjustments)
- This ensures consistent timestamp calculations between Kotlin and Swift code

---

## Resolved Questions

### 1. **COUNT_UP_HARD_LIMIT on iOS**

**Decision**: Use 8 hours (28800 seconds) instead of 15 to match Live Activity limit.

**Edge case**: If user counts up to 8 hours without any action:
- Store a session of 8 hours
- Set notification for 8 hours
- Store session in UserDefaults
- When app opens, migrate to Room

**Implementation**: The extension's `checkTimerCompletion()` should check if a count-up timer has reached 8 hours and handle it appropriately.

### 2. **Settings Drift When App Is Killed**

**Not a concern** - Settings cannot change while the app is killed. Settings are only changed when the user has the host app open and navigates to the settings screen. The UserDefaults will always have the current settings.

### 3. **Timestamp Calculation**

Use the same formula as `TimerManager.createFinishedSession()`:

```kotlin
val now = timeProvider.now()              // wall-clock (Unix epoch ms)
val elapsedRealtime = timeProvider.elapsedRealtime()  // monotonic (ms since boot)
val timestampAtEnd = now - elapsedRealtime + runtime.endTime
```

This works because:
- `now - elapsedRealtime` = Unix epoch at boot
- Adding `endTime` (elapsedRealtime when session ended) = wall-clock timestamp of session end

**Time source equivalence**:
- Kotlin: `clock_gettime(CLOCK_MONOTONIC)`
- Swift: `CACurrentMediaTime() * 1000`
- Both are monotonic and include sleep time

---

## Appendix: File Changes Summary

### New Files
| File | Target | Purpose |
|------|--------|---------|
| `IosUserDefaultsTimerStorage.kt` | Host App (iosMain) | UserDefaults read/write (Kotlin) |
| `TimerStateManager.swift` | Extension | Read/write shared state (Swift) |
| `TimerState.swift` (Codable) | Extension/Shared | Timer state struct |
| `CompletedSession.swift` (Codable) | Extension/Shared | Completed session struct (matches Session.kt) |

### Modified Files
| File | Changes |
|------|---------|
| `Event.kt` | Add `domainTimerData: DomainTimerData?` and `label: Label?` to Start/Pause/Finished events |
| `IosTimerStatePersistenceListener.kt` | Write to UserDefaults instead of DataStore; handle Event.Reset by reading from UserDefaults; mirror `createFinishedSession` logic (1-min rule, wiggle room, interruptions) |
| `TimerStateRestoration.kt` | Read from UserDefaults instead of DataStore; migrate completed sessions to Room; handle pending actions |
| `TimerManager.kt` | Pass `DomainTimerData` and `Label` to EventListener events (Start/Pause/Finished) |
| `GoodtimeAppIntents.swift` | Use `TimerStateManager` instead of NotificationCenter; set label when Live Activity starts |
| `IosNotificationDelegate.kt` | Write pendingAction to UserDefaults for notification actions; use default label for completed sessions |
| `GoodtimeInProgressLiveActivity.swift` | Update UI after state change from AppIntents |
| `GoodtimeLiveActivityManager.kt` (or bridge) | Call `TimerManager.shared.setLabel()` when starting Live Activity |
| `DataStore` (SettingsRepository) | Remove `PersistedTimerState` (no longer needed on iOS) |

### Deprecated Files (Can Remove)
| File | Reason |
|------|--------|
| `LiveActivityIntentHandler.swift` | No longer needed (replaced by UserDefaults) |
| `LiveActivityIntentBridge.kt` | No longer needed (replaced by UserDefaults) |

---

## Summary

**Key Changes**:
1. UserDefaults becomes single source of truth for iOS timer state
2. `IosTimerStatePersistenceListener` writes to UserDefaults instead of DataStore
3. `TimerStateRestoration` reads from UserDefaults (already compatible)
4. Extension can read/write same UserDefaults
5. `CompletedSession` matches `Session.kt` structure (duration/interruptions in minutes)
6. `IosTimerStatePersistenceListener` mirrors `TimerManager.createFinishedSession` logic:
   - **Focus**: duration = (totalDuration - interruptions) + wiggle room
   - **Break**: duration = totalDuration + wiggle room
   - **Focus**: includes interruptions; **Break**: interruptions = 0
   - **1-minute rule**: sessions < 1 minute are discarded
   - **Timestamp**: calculated from elapsedRealtime → wall-clock
7. `Event.Reset` handled by reading from UserDefaults (event can't carry data)

**Result**:
- Live Activity buttons work when app is killed
- Extension can start next sessions independently
- No sync between DataStore and UserDefaults
- Simpler architecture, fewer bugs
- Sessions stored from extension are accurate (same logic as host app)
