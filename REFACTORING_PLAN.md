# MainActivity.kt to Compose Multiplatform Refactoring Plan

**Project:** Goodtime Productivity
**Goal:** Extract Compose UI from Android-only MainActivity.kt to commonMain for iOS/Android sharing
**Platform Focus:** Android-first (Linux dev machine), iOS testing later on Mac
**Date Started:** 2025-11-29

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Target Architecture](#target-architecture)
3. [Phase-by-Phase Plan](#phase-by-phase-plan)
4. [Progress Tracking](#progress-tracking)
5. [Technical Decisions](#technical-decisions)
6. [Code Patterns & Examples](#code-patterns--examples)

---

## Current State Analysis

### What's Already in commonMain ✅

- **All screen composables:**
  - `OnboardingScreen.kt`
  - `MainScreen.kt`
  - `SettingsScreen.kt`
  - `BackupScreen.kt` (common version exists)
  - `NotificationsScreen.kt`
  - `StatisticsScreen.kt`
  - `LabelsScreen.kt`, `AddEditLabelScreen.kt`, `ArchivedLabelsScreen.kt`
  - `UserInterfaceScreen.kt`, `TimerProfileScreen.kt`
  - `AboutScreen.kt`, `LicensesScreen.kt`, `AcknowledgementsScreen.kt`
  - `ProScreen.kt` (billing)

- **Navigation:**
  - `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/main/Destination.kt`
  - All destination objects defined (OnboardingDest, MainDest, LabelsDest, etc.)

- **ViewModels:**
  - `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/onboarding/MainViewModel.kt`
  - `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/main/TimerViewModel.kt`

- **UI Components:**
  - `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/ApplicationTheme.kt`
  - `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/ObserveAsEvents.kt`
  - `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/SnackbarEvents.kt` (SnackbarController)

- **Other:**
  - `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/App.kt` (placeholder)

### What's Currently Android-Only ❌

**File:** `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/MainActivity.kt` (442 lines)

**Android-Specific Components:**
1. **Lifecycle Management:**
   - `onResume()` - calls `timerViewModel.onBringToForeground()`
   - `onPause()` - calls `timerViewModel.onSendToBackground()`
   - `onCreate()` - main setup
   - `onDestroy()` - notification cleanup

2. **Platform UI Setup:**
   - Splash screen: `installSplashScreen()`
   - Edge-to-edge: `enableEdgeToEdge()`, `WindowCompat.setDecorFitsSystemWindows()`
   - System bar styling: `SystemBarStyle.auto()`
   - Dark theme detection: `isSystemInDarkTheme()` via `resources.configuration`

3. **Window Management:**
   - `toggleKeepScreenOn()` - uses `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`
   - `toggleFullscreen()` - uses `WindowInsetsController` to hide/show system bars
   - `setShowWhenLocked()` - Android O_MR1+ API
   - Fullscreen auto-hide with delayed re-enable (coroutine job management)

4. **Navigation Setup:**
   - NavHost with all composable destinations (lines 272-383)
   - NavController listener for tracking `isMainScreen`
   - `popBackStack2()` extension function

5. **Theme Management:**
   - ThemeSettings data class (lines 438-441)
   - Theme flow combining system dark mode + user preference
   - Dynamic color support check

6. **Flavor-Specific Base Class:**
   - `GoodtimeMainActivity` has TWO implementations:
     - `/composeApp/src/androidGoogle/kotlin/.../GoodtimeMainActivity.kt` (with app updates)
     - `/composeApp/src/androidFdroid/kotlin/.../GoodtimeMainActivity.kt` (minimal)

### iOS Current State

**File:** `/composeApp/src/iosMain/kotlin/com/apps/adrcotfas/goodtime/MainViewController.kt`

Currently just calls `App()` placeholder. Needs to be updated to call the new shared GoodtimeApp.

---

## Target Architecture

### File Structure After Refactoring

```
composeApp/src/
├── commonMain/kotlin/com/apps/adrcotfas/goodtime/
│   ├── GoodtimeApp.kt                    [NEW] Main app composable with navigation
│   ├── App.kt                            [REPLACE] Currently placeholder
│   ├── platform/
│   │   ├── PlatformContext.kt           [NEW] expect class for platform APIs
│   │   └── PlatformConfiguration.kt     [NEW] Platform capability detection
│   ├── ui/
│   │   ├── ThemeSettings.kt             [NEW] Moved from MainActivity
│   │   └── (existing files)
│   └── main/
│       ├── Destination.kt               [EXISTING] ✅
│       ├── MainScreen.kt                [EXISTING] ✅
│       └── (other screens)
│
├── androidMain/kotlin/com/apps/adrcotfas/goodtime/
│   ├── MainActivity.kt                   [REFACTOR] Thin lifecycle wrapper
│   ├── platform/
│   │   ├── PlatformContext.android.kt   [NEW] actual implementations
│   │   └── PlatformConfiguration.android.kt [NEW]
│   └── ui/
│       ├── UiExtensions.kt              [EXISTING] isSystemInDarkTheme flow
│       └── ThemeState.android.kt        [NEW] Theme state collection
│
├── androidGoogle/kotlin/.../main/
│   └── GoodtimeMainActivity.kt          [EXISTING] With app updates
│
├── androidFdroid/kotlin/.../main/
│   └── GoodtimeMainActivity.kt          [EXISTING] Minimal version
│
└── iosMain/kotlin/com/apps/adrcotfas/goodtime/
    ├── MainViewController.kt             [UPDATE] Call GoodtimeApp
    ├── platform/
    │   ├── PlatformContext.ios.kt       [NEW] actual implementations
    │   └── PlatformConfiguration.ios.kt [NEW]
    └── ui/
        └── ThemeState.ios.kt            [NEW] Theme state collection
```

---

## Phase-by-Phase Plan

### Phase 1: Create Platform Abstraction Layer ⏳

**Goal:** Set up expect/actual pattern for platform-specific APIs

#### Step 1.1: Create PlatformContext (expect)
- [ ] **File:** `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformContext.kt`
- [ ] Define `expect class PlatformContext`
- [ ] Define extension functions:
  ```kotlin
  expect fun PlatformContext.setKeepScreenOn(enabled: Boolean)
  expect fun PlatformContext.setFullscreen(enabled: Boolean)
  expect fun PlatformContext.setShowWhenLocked(enabled: Boolean)
  expect fun PlatformContext.configureSystemBars(isDarkTheme: Boolean, isFullscreen: Boolean)
  ```

#### Step 1.2: Create PlatformConfiguration (expect)
- [ ] **File:** `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformConfiguration.kt`
- [ ] Define interface with platform capabilities:
  ```kotlin
  interface PlatformConfiguration {
      val supportsInAppUpdates: Boolean
      val supportsDynamicColor: Boolean
      val supportsShowWhenLocked: Boolean
  }
  expect fun getPlatformConfiguration(): PlatformConfiguration
  ```

#### Step 1.3: Implement Android actual - PlatformContext
- [ ] **File:** `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformContext.android.kt`
- [ ] Implement `actual class PlatformContext(private val activity: ComponentActivity)`
- [ ] Implement `setKeepScreenOn()` using `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`
- [ ] Implement `setFullscreen()` using `WindowInsetsController`
- [ ] Implement `setShowWhenLocked()` using `Activity.setShowWhenLocked()` (O_MR1+)
- [ ] Implement `configureSystemBars()` wrapping `enableEdgeToEdge()` logic

#### Step 1.4: Implement Android actual - PlatformConfiguration
- [ ] **File:** `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformConfiguration.android.kt`
- [ ] Return `supportsInAppUpdates = true`
- [ ] Return `supportsDynamicColor = Build.VERSION.SDK_INT >= 31`
- [ ] Return `supportsShowWhenLocked = true`

#### Step 1.5: Create iOS stubs (implement later on Mac)
- [ ] **File:** `/composeApp/src/iosMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformContext.ios.kt`
- [ ] Create stub implementations (empty or basic)
- [ ] **File:** `/composeApp/src/iosMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformConfiguration.ios.kt`
- [ ] Return iOS capabilities (mostly false for now)

---

### Phase 2: Extract Theme Management ⏳

**Goal:** Move theme state and ThemeSettings to common code

#### Step 2.1: Create ThemeSettings data class in common
- [ ] **File:** `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/ThemeSettings.kt`
- [ ] Move `ThemeSettings` data class from MainActivity.kt:
  ```kotlin
  data class ThemeSettings(
      val darkTheme: Boolean,
      val isDynamicTheme: Boolean,
  )
  ```

#### Step 2.2: Create expect/actual for theme state collection
- [ ] **File (expect):** `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/ThemeState.kt`
- [ ] Define:
  ```kotlin
  @Composable
  expect fun collectThemeSettings(
      mainUiState: MainUiState,
      showOnboarding: Boolean
  ): State<ThemeSettings>
  ```

#### Step 2.3: Implement Android theme state collection
- [ ] **File:** `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/ui/ThemeState.android.kt`
- [ ] Implement actual function using `isSystemInDarkTheme()` flow from UiExtensions.kt
- [ ] Combine with `mainUiState.darkThemePreference` and `mainUiState.isDynamicColor`
- [ ] Use existing pattern from MainActivity.kt lines 141-149

#### Step 2.4: Create iOS theme state stub
- [ ] **File:** `/composeApp/src/iosMain/kotlin/com/apps/adrcotfas/goodtime/ui/ThemeState.ios.kt`
- [ ] Create stub that returns basic ThemeSettings (implement properly on Mac later)

---

### Phase 3: Create GoodtimeApp Composable ⏳

**Goal:** Extract the entire Compose UI tree into a shared composable

#### Step 3.1: Create GoodtimeApp.kt
- [ ] **File:** `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/GoodtimeApp.kt`
- [ ] Create main composable:
  ```kotlin
  @Composable
  fun GoodtimeApp(
      platformContext: PlatformContext,
      timerViewModel: TimerViewModel,
      mainViewModel: MainViewModel,
      themeSettings: ThemeSettings,
      onUpdateClicked: (() -> Unit)? = null, // null on iOS
  )
  ```

#### Step 3.2: Move Scaffold and Snackbar setup
- [ ] Move `SnackbarHostState` creation
- [ ] Move `ObserveAsEvents` for SnackbarController
- [ ] Move `Scaffold` with `SnackbarHost`
- [ ] Source: MainActivity.kt lines 225-270

#### Step 3.3: Move ApplicationTheme wrapper
- [ ] Wrap content in `ApplicationTheme(darkTheme, dynamicColor)`
- [ ] Source: MainActivity.kt line 223

#### Step 3.4: Move NavController setup
- [ ] Create `navController = rememberNavController()`
- [ ] Add destination change listener for `isMainScreen` tracking
- [ ] Source: MainActivity.kt lines 224-228

#### Step 3.5: Move start destination logic
- [ ] Calculate `startDestination` based on `mainUiState.showOnboarding`
- [ ] Source: MainActivity.kt lines 214-221

#### Step 3.6: Move NavHost with all destinations
- [ ] Move entire `NavHost` block (lines 272-383)
- [ ] Keep all composable destinations
- [ ] Handle `onUpdateClicked` being optional (show update UI only if not null)
- [ ] Pass `platformContext` where needed

#### Step 3.7: Handle fullscreen logic
- [ ] Move fullscreen state management
- [ ] Move `onSurfaceClick` callback logic
- [ ] Move `hideBottomBar` state
- [ ] Handle `fullScreenJob` for delayed auto-hide
- [ ] Source: MainActivity.kt lines 188-211
- [ ] **Note:** This is complex - may need refinement

#### Step 3.8: Handle other UI state effects
- [ ] Move `isFinished` navigation logic (lines 231-244)
- [ ] Call `platformContext.setKeepScreenOn()` when needed (line 213)
- [ ] Call `platformContext.setShowWhenLocked()` when needed (lines 184-186)

---

### Phase 4: Refactor MainActivity to Use GoodtimeApp ⏳

**Goal:** Make MainActivity a thin lifecycle wrapper

#### Step 4.1: Simplify MainActivity onCreate
- [ ] **File:** `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/MainActivity.kt`
- [ ] Keep splash screen setup (line 126)
- [ ] Keep `WindowCompat.setDecorFitsSystemWindows()` (line 130)
- [ ] Create `PlatformContext(this)`
- [ ] Move theme state collection to Android-specific actual
- [ ] Replace entire `setContent {}` block with call to `GoodtimeApp()`

#### Step 4.2: Update setContent block
- [ ] Collect theme settings using Android actual function
- [ ] Call `platformContext.configureSystemBars()` in LaunchedEffect
- [ ] Call `GoodtimeApp()` with all parameters:
  ```kotlin
  setContent {
      val themeSettings by collectThemeSettings(
          mainUiState = viewModel.uiState.collectAsStateWithLifecycle().value,
          showOnboarding = viewModel.uiState.value.showOnboarding
      )

      val platformContext = remember { PlatformContext(this) }

      LaunchedEffect(themeSettings.darkTheme) {
          platformContext.configureSystemBars(
              isDarkTheme = themeSettings.darkTheme,
              isFullscreen = false // updated dynamically in GoodtimeApp
          )
      }

      GoodtimeApp(
          platformContext = platformContext,
          timerViewModel = timerViewModel,
          mainViewModel = viewModel,
          themeSettings = themeSettings,
          onUpdateClicked = { triggerAppUpdate() }
      )
  }
  ```

#### Step 4.3: Keep Android lifecycle methods
- [ ] Keep `onResume()` - calls `timerViewModel.onBringToForeground()`
- [ ] Keep `onPause()` - calls `timerViewModel.onSendToBackground()`
- [ ] Keep `onDestroy()` - calls `notificationManager.clearFinishedNotification()`
- [ ] **Note:** Consider moving foreground/background to common code later

#### Step 4.4: Remove moved code
- [ ] Delete `ThemeSettings` data class (moved to common)
- [ ] Delete `toggleKeepScreenOn()` (moved to PlatformContext)
- [ ] Delete `toggleFullscreen()` (moved to PlatformContext)
- [ ] Delete `executeDelayed()` helper (moved to GoodtimeApp if still needed)
- [ ] Delete `lightScrim` and `darkScrim` constants (moved to PlatformContext)

#### Step 4.5: Keep utility functions
- [ ] Keep `popBackStack2()` extension - but consider moving to common

---

### Phase 5: Handle Edge Cases and Polish ⏳

**Goal:** Address remaining platform-specific concerns

#### Step 5.1: Review GoodtimeMainActivity integration
- [ ] Verify `GoodtimeMainActivity` (androidGoogle flavor) still works
- [ ] Verify `GoodtimeMainActivity` (androidFdroid flavor) still works
- [ ] Ensure `viewModel` injection from base class works
- [ ] Ensure `triggerAppUpdate()` from base class (Google flavor) works

#### Step 5.2: Handle navigation extensions
- [ ] Consider moving `popBackStack2()` to commonMain
- [ ] **File:** `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/NavigationExtensions.kt`
- [ ] Or keep in MainActivity if Android-specific lifecycle check is needed

#### Step 5.3: Review fullscreen implementation
- [ ] Test fullscreen mode on Android
- [ ] Verify auto-hide delay works correctly
- [ ] Verify `onSurfaceClick` brings back system bars temporarily
- [ ] Ensure `hideBottomBar` state syncs with fullscreen state

#### Step 5.4: Verify Koin dependency injection
- [ ] Ensure `TimerViewModel` injection works in GoodtimeApp
- [ ] Ensure `MainViewModel` injection works
- [ ] Check Koin configuration for commonMain ViewModels

---

### Phase 6: Testing on Android ⏳

**Goal:** Verify all functionality works after refactoring

#### Step 6.1: Build and run
- [ ] `./gradlew :composeApp:assembleDebug`
- [ ] Install and run on Android device/emulator
- [ ] Verify app launches without crashes

#### Step 6.2: Test navigation
- [ ] Verify onboarding shows on first launch
- [ ] Navigate through all screens:
  - [ ] MainScreen
  - [ ] LabelsScreen → AddEditLabelScreen
  - [ ] LabelsScreen → ArchivedLabelsScreen
  - [ ] StatisticsScreen
  - [ ] SettingsScreen
  - [ ] SettingsScreen → UserInterfaceScreen
  - [ ] SettingsScreen → NotificationsScreen
  - [ ] SettingsScreen → TimerDurationsScreen
  - [ ] SettingsScreen → BackupScreen (if applicable)
  - [ ] AboutScreen → LicensesScreen
  - [ ] AboutScreen → AcknowledgementsScreen
  - [ ] ProScreen (if not pro user)

#### Step 6.3: Test platform features
- [ ] Test fullscreen mode (toggle and auto-hide)
- [ ] Test keep screen on during active timer
- [ ] Test show when locked (start timer, lock device)
- [ ] Test theme switching (light/dark/system)
- [ ] Test dynamic color (Android 12+)
- [ ] Test splash screen

#### Step 6.4: Test timer functionality
- [ ] Start timer, verify foreground tracking
- [ ] Send app to background, verify `onPause` handling
- [ ] Bring app to foreground, verify `onResume` handling
- [ ] Complete timer session, verify navigation to main
- [ ] Test finished session notification

#### Step 6.5: Test flavor differences
- [ ] Test androidGoogle build:
  - [ ] Verify app update prompt shows (if update available)
  - [ ] Verify clicking update triggers Play Store flow
- [ ] Test androidFdroid build:
  - [ ] Verify no update prompt (feature disabled)

#### Step 6.6: Test edge cases
- [ ] Snackbar events display correctly
- [ ] Back navigation works properly
- [ ] Configuration changes (rotate device)
- [ ] Process death and restoration

---

### Phase 7: iOS Implementation (Later on Mac) 🔜

**Goal:** Complete iOS implementation and test on Mac

#### Step 7.1: Implement iOS PlatformContext
- [ ] **File:** `/composeApp/src/iosMain/kotlin/.../platform/PlatformContext.ios.kt`
- [ ] Implement `setKeepScreenOn()` using `UIApplication.sharedApplication.idleTimerDisabled`
- [ ] Implement or stub `setFullscreen()` (iOS handles differently)
- [ ] Stub `setShowWhenLocked()` (not applicable to iOS)
- [ ] Implement `configureSystemBars()` for iOS status bar

#### Step 7.2: Implement iOS theme state collection
- [ ] **File:** `/composeApp/src/iosMain/kotlin/.../ui/ThemeState.ios.kt`
- [ ] Implement system dark mode detection for iOS
- [ ] Combine with user preferences

#### Step 7.3: Update MainViewController
- [ ] Call `GoodtimeApp()` instead of `App()`
- [ ] Inject ViewModels using Koin
- [ ] Pass `onUpdateClicked = null`

#### Step 7.4: iOS-specific considerations
- [ ] Handle timer background behavior (local notifications)
- [ ] Handle safe area insets
- [ ] Test on iOS simulator
- [ ] Test on physical iOS device

---

## Progress Tracking

### Overall Progress

- [ ] Phase 1: Create Platform Abstraction Layer (0/5 steps)
- [ ] Phase 2: Extract Theme Management (0/4 steps)
- [ ] Phase 3: Create GoodtimeApp Composable (0/8 steps)
- [ ] Phase 4: Refactor MainActivity (0/5 steps)
- [ ] Phase 5: Handle Edge Cases (0/4 steps)
- [ ] Phase 6: Testing on Android (0/6 steps)
- [ ] Phase 7: iOS Implementation (0/4 steps - later on Mac)

### Current Phase
**Not started** - Ready to begin Phase 1

---

## Technical Decisions

### 1. Platform Abstraction Pattern
**Decision:** Use `expect class PlatformContext` with extension functions
**Rationale:** Cleaner than interfaces, allows direct platform API access in actual implementations
**Source:** Opus 4.5 suggestion, better than my original PlatformUiEffects

### 2. Theme Management
**Decision:** Use expect/actual for `collectThemeSettings()` composable function
**Rationale:** Android needs Configuration listener flow, iOS has different API
**Alternative considered:** Shared Flow with platform-specific collectors

### 3. App Updates
**Decision:** Make `onUpdateClicked` nullable `(() -> Unit)?`
**Rationale:** Feature is Play Store specific, doesn't exist on iOS App Store
**Implementation:** Conditionally show update UI in GoodtimeApp when not null

### 4. Navigation
**Decision:** Keep entire NavHost in commonMain
**Rationale:** All destinations are already common, Jetpack Navigation supports multiplatform (1.7.0+)
**Already done:** All screens and destinations are in commonMain

### 5. Fullscreen Logic
**Decision:** Keep complex auto-hide logic in common GoodtimeApp
**Rationale:** UX behavior should be consistent across platforms
**Platform-specific:** Only the actual system bar hiding implementation
**Challenge:** May need refinement during implementation

### 6. Lifecycle Handling
**Decision:** Keep Android lifecycle methods in MainActivity for now
**Rationale:** `onResume`/`onPause` calls to ViewModel are currently Android-specific
**Future:** Consider moving to common lifecycle hooks

### 7. ViewModels
**Decision:** Keep ViewModels in commonMain, inject via Koin
**Rationale:** Already done, Koin supports multiplatform ViewModels
**Note:** Verify Koin 4.0 configuration works for both platforms

### 8. Splash Screen
**Decision:** Keep platform-specific
**Rationale:** Android uses `installSplashScreen()`, iOS uses Storyboard or Compose wrapper
**Implementation:** Android in MainActivity, iOS in MainViewController

---

## Code Patterns & Examples

### PlatformContext Usage Pattern

```kotlin
// In GoodtimeApp.kt (commonMain)
@Composable
fun GoodtimeApp(platformContext: PlatformContext, ...) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Platform abstraction in action
    LaunchedEffect(uiState.keepScreenOn, uiState.isActive) {
        platformContext.setKeepScreenOn(uiState.isActive && uiState.keepScreenOn)
    }

    LaunchedEffect(uiState.fullscreenMode) {
        platformContext.setFullscreen(uiState.fullscreenMode)
    }
}

// Android implementation
actual fun PlatformContext.setKeepScreenOn(enabled: Boolean) {
    if (enabled) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

// iOS implementation
actual fun PlatformContext.setKeepScreenOn(enabled: Boolean) {
    UIApplication.sharedApplication.idleTimerDisabled = enabled
}
```

### Navigation Extension Pattern

```kotlin
// commonMain or keep in androidMain depending on Lifecycle dependency
fun NavController.popBackStack2(): Boolean {
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        return this.popBackStack()
    }
    return false
}
```

### Conditional Update UI Pattern

```kotlin
// In GoodtimeApp.kt
@Composable
fun GoodtimeApp(
    onUpdateClicked: (() -> Unit)? = null,
    ...
) {
    // Only show update UI if callback provided (Android Google flavor)
    if (onUpdateClicked != null && mainUiState.updateAvailable) {
        UpdateAvailableBanner(onClick = onUpdateClicked)
    }
}
```

### Theme Collection Pattern

```kotlin
// commonMain - expect
@Composable
expect fun collectThemeSettings(
    mainUiState: MainUiState,
    showOnboarding: Boolean
): State<ThemeSettings>

// androidMain - actual
@Composable
actual fun collectThemeSettings(
    mainUiState: MainUiState,
    showOnboarding: Boolean
): State<ThemeSettings> {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    return produceState(
        initialValue = ThemeSettings(
            darkTheme = false,
            isDynamicTheme = false
        )
    ) {
        activity?.let {
            combine(
                it.isSystemInDarkTheme(),
                flowOf(mainUiState)
            ) { systemDark, uiState ->
                ThemeSettings(
                    darkTheme = uiState.darkThemePreference.isDarkTheme(systemDark)
                        && !showOnboarding,
                    isDynamicTheme = uiState.isDynamicColor
                )
            }.collect { value = it }
        }
    }
}
```

---

## File Reference Quick Links

### Files to Create
1. `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformContext.kt`
2. `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformConfiguration.kt`
3. `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/ThemeSettings.kt`
4. `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/ui/ThemeState.kt`
5. `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/GoodtimeApp.kt`
6. `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformContext.android.kt`
7. `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformConfiguration.android.kt`
8. `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/ui/ThemeState.android.kt`
9. `/composeApp/src/iosMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformContext.ios.kt`
10. `/composeApp/src/iosMain/kotlin/com/apps/adrcotfas/goodtime/platform/PlatformConfiguration.ios.kt`
11. `/composeApp/src/iosMain/kotlin/com/apps/adrcotfas/goodtime/ui/ThemeState.ios.kt`

### Files to Modify
1. `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/MainActivity.kt` - Major refactoring
2. `/composeApp/src/iosMain/kotlin/com/apps/adrcotfas/goodtime/MainViewController.kt` - Update to call GoodtimeApp

### Files to Reference (Don't Modify)
1. `/composeApp/src/commonMain/kotlin/com/apps/adrcotfas/goodtime/main/Destination.kt` - Navigation destinations
2. `/composeApp/src/androidMain/kotlin/com/apps/adrcotfas/goodtime/ui/UiExtensions.kt` - isSystemInDarkTheme()
3. `/composeApp/src/androidGoogle/kotlin/.../main/GoodtimeMainActivity.kt` - Base class with updates
4. `/composeApp/src/androidFdroid/kotlin/.../main/GoodtimeMainActivity.kt` - Base class minimal

---

## Notes & Observations

### Complexity Hotspots
1. **Fullscreen auto-hide logic** - Lines 188-211 of MainActivity.kt
   - Uses coroutine job management
   - Has delayed re-enable after surface click
   - State coordination between `fullscreenMode`, `hideBottomBar`, `onSurfaceClick`
   - May need careful abstraction

2. **Theme state collection** - Lines 132-167 of MainActivity.kt
   - Combines system dark mode (from Configuration listener)
   - User preference from MainViewModel
   - Triggers EdgeToEdge system bar updates
   - Platform-specific (uses Android Configuration)

3. **Finished session navigation** - Lines 231-244 of MainActivity.kt
   - Navigates to MainDest when timer finishes
   - Only if not already on MainDest
   - Should work in common code

### Questions to Resolve During Implementation
1. Should `popBackStack2()` move to common? (depends on Lifecycle import)
2. How to handle `lifecycleScope` vs `rememberCoroutineScope()` for fullscreen job?
3. Best way to pass activity reference to PlatformContext? (constructor vs LocalContext)
4. Should ViewModel foreground/background calls move to common lifecycle?

### Testing Priorities
1. **Critical:** Navigation between all screens
2. **Critical:** Timer start/stop/background/foreground
3. **High:** Fullscreen mode and auto-hide
4. **High:** Theme switching
5. **Medium:** App update flow (Google flavor)
6. **Medium:** Configuration changes
7. **Low:** Edge cases (process death, etc.)

---

## Timeline Estimates

| Phase | Estimated Time | Complexity |
|-------|---------------|------------|
| Phase 1 | 1-2 hours | Medium |
| Phase 2 | 1 hour | Low-Medium |
| Phase 3 | 2-3 hours | High (most code moved here) |
| Phase 4 | 1 hour | Medium |
| Phase 5 | 1 hour | Low-Medium |
| Phase 6 | 2-3 hours | Medium (thorough testing) |
| **Total (Android)** | **8-11 hours** | - |
| Phase 7 (iOS) | TBD on Mac | TBD |

---

## Success Criteria

### Phase 1-5 Complete When:
- [ ] All new files created and compiling
- [ ] MainActivity.kt reduced to < 150 lines
- [ ] No code duplication between MainActivity and GoodtimeApp
- [ ] GoodtimeApp.kt contains all UI/navigation logic
- [ ] Build succeeds: `./gradlew :composeApp:assembleDebug`

### Phase 6 Complete When:
- [ ] App launches without crashes
- [ ] All screens accessible via navigation
- [ ] Timer functionality works (start/stop/background/foreground)
- [ ] Platform features work (fullscreen, keep screen on, theme)
- [ ] Both flavors build and run correctly

### Phase 7 Complete When:
- [ ] iOS build succeeds
- [ ] iOS app launches and navigates
- [ ] Basic timer functionality works on iOS
- [ ] Cross-platform code sharing confirmed

---

## Rollback Plan

If major issues encountered:
1. Commit early and often during each phase
2. Git branch: `feature/cmp-refactoring` (create before starting)
3. Can revert to `master` if needed
4. Keep original MainActivity.kt backed up temporarily

---

**Last Updated:** 2025-11-29
**Next Step:** Begin Phase 1, Step 1.1 - Create PlatformContext expect class
