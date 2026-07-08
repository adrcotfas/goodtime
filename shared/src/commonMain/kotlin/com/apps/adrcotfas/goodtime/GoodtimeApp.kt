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
package com.apps.adrcotfas.goodtime

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.apps.adrcotfas.goodtime.bl.TimerForegroundMonitor
import com.apps.adrcotfas.goodtime.data.settings.ThemePreference
import com.apps.adrcotfas.goodtime.main.MainDest
import com.apps.adrcotfas.goodtime.main.MainViewModel
import com.apps.adrcotfas.goodtime.main.OnboardingDest
import com.apps.adrcotfas.goodtime.main.route
import com.apps.adrcotfas.goodtime.platform.PlatformContext
import com.apps.adrcotfas.goodtime.platform.configureSystemBars
import com.apps.adrcotfas.goodtime.platform.setFullscreen
import com.apps.adrcotfas.goodtime.platform.setShowWhenLocked
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Main composable for the Goodtime app.
 * Contains all navigation, theming, and core UI logic.
 * Platform-agnostic and shared between Android and iOS.
 *
 * @param platformContext Platform-specific context for accessing platform APIs
 * @param mainViewModel ViewModel for main app state
 * @param themeSettings Current theme settings (resolved from system + user preferences)
 * @param onUpdateClicked Callback for when user clicks update button (null on iOS, Google Play only)
 */
@Composable
fun GoodtimeApp(
    platformContext: PlatformContext,
    mainViewModel: MainViewModel,
    onUpdateClicked: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val timerForegroundMonitor: TimerForegroundMonitor = koinInject()

    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val isDarkTheme =
        if (uiState.darkThemePreference == ThemePreference.SYSTEM) {
            isSystemInDarkTheme()
        } else {
            uiState.darkThemePreference == ThemePreference.DARK
        }
    LaunchedEffect(isDarkTheme) {
        platformContext.configureSystemBars(
            isDarkTheme = isDarkTheme,
        )
    }

    val showWhenLocked = uiState.showWhenLocked
    val isFinished = uiState.isFinished
    var isMainScreen by rememberSaveable { mutableStateOf(true) }

    // Handle show when locked
    LaunchedEffect(showWhenLocked) {
        platformContext.setShowWhenLocked(showWhenLocked)
    }

    // Handle fullscreen mode
    val fullscreenMode = isMainScreen && uiState.fullscreenMode
    var fullScreenJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(fullscreenMode) {
        fullscreenMode.let {
            platformContext.setFullscreen(it)
            if (!it) fullScreenJob?.cancel()
        }
    }

    LifecycleResumeEffect(Unit) {
        timerForegroundMonitor.onBringToForeground(coroutineScope)
        onPauseOrDispose {
            timerForegroundMonitor.onSendToBackground()
        }
    }

    var hideBottomBar by remember(fullscreenMode) {
        mutableStateOf(fullscreenMode)
    }

    val onSurfaceClick = {
        if (fullscreenMode) {
            fullScreenJob?.cancel()
            fullScreenJob =
                coroutineScope.launch {
                    platformContext.setFullscreen(false)
                    hideBottomBar = false
                    delay(3000.milliseconds)
                    platformContext.setFullscreen(true)
                    hideBottomBar = true
                }
        }
    }

    // Calculate start destination
    val startDestination =
        remember(uiState.showOnboarding) {
            if (uiState.showOnboarding) {
                OnboardingDest
            } else {
                MainDest
            }
        }

    ApplicationTheme(darkTheme = isDarkTheme, dynamicColor = uiState.isDynamicColor) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        DisposableEffect(navController) {
            val listener =
                NavController.OnDestinationChangedListener { _, destination, _ ->
                    isMainScreen = destination.route == MainDest.route
                }
            navController.addOnDestinationChangedListener(listener)
            onDispose { navController.removeOnDestinationChangedListener(listener) }
        }

        // Handle finished session navigation
        LaunchedEffect(isFinished) {
            if (isFinished) {
                navController.currentDestination?.route?.let {
                    val shouldNavigate = it != MainDest.route
                    if (shouldNavigate) {
                        navController.navigate(MainDest) {
                            popUpTo(MainDest) {
                                inclusive = true
                            }
                        }
                    }
                }
            }
        }

        ObserveSnackbarEvents(snackbarHostState, coroutineScope)

        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                )
            },
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                goodtimeNavGraph(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    onSurfaceClick = onSurfaceClick,
                    hideBottomBar = hideBottomBar,
                    onUpdateClicked = onUpdateClicked,
                )
            }
        }
    }
}
