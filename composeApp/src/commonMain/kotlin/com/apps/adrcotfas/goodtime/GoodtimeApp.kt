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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.apps.adrcotfas.goodtime.billing.ProScreen
import com.apps.adrcotfas.goodtime.data.settings.ThemePreference
import com.apps.adrcotfas.goodtime.labels.addedit.AddEditLabelScreen
import com.apps.adrcotfas.goodtime.labels.archived.ArchivedLabelsScreen
import com.apps.adrcotfas.goodtime.labels.main.LabelsScreen
import com.apps.adrcotfas.goodtime.main.AboutDest
import com.apps.adrcotfas.goodtime.main.AcknowledgementsDest
import com.apps.adrcotfas.goodtime.main.AddEditLabelDest
import com.apps.adrcotfas.goodtime.main.ArchivedLabelsDest
import com.apps.adrcotfas.goodtime.main.BackupDest
import com.apps.adrcotfas.goodtime.main.LabelsDest
import com.apps.adrcotfas.goodtime.main.LicensesDest
import com.apps.adrcotfas.goodtime.main.MainDest
import com.apps.adrcotfas.goodtime.main.MainScreen
import com.apps.adrcotfas.goodtime.main.NotificationSettingsDest
import com.apps.adrcotfas.goodtime.main.OnboardingDest
import com.apps.adrcotfas.goodtime.main.ProDest
import com.apps.adrcotfas.goodtime.main.SettingsDest
import com.apps.adrcotfas.goodtime.main.StatsDest
import com.apps.adrcotfas.goodtime.main.TimerDurationsDest
import com.apps.adrcotfas.goodtime.main.TimerViewModel
import com.apps.adrcotfas.goodtime.main.UserInterfaceDest
import com.apps.adrcotfas.goodtime.main.route
import com.apps.adrcotfas.goodtime.onboarding.MainViewModel
import com.apps.adrcotfas.goodtime.onboarding.OnboardingScreen
import com.apps.adrcotfas.goodtime.platform.PlatformContext
import com.apps.adrcotfas.goodtime.platform.configureSystemBars
import com.apps.adrcotfas.goodtime.platform.setFullscreen
import com.apps.adrcotfas.goodtime.platform.setShowWhenLocked
import com.apps.adrcotfas.goodtime.settings.SettingsScreen
import com.apps.adrcotfas.goodtime.settings.about.AboutScreen
import com.apps.adrcotfas.goodtime.settings.about.AcknowledgementsScreen
import com.apps.adrcotfas.goodtime.settings.about.LicensesScreen
import com.apps.adrcotfas.goodtime.settings.backup.BackupScreen
import com.apps.adrcotfas.goodtime.settings.notifications.NotificationsScreen
import com.apps.adrcotfas.goodtime.settings.timerdurations.TimerProfileScreen
import com.apps.adrcotfas.goodtime.settings.timerstyle.UserInterfaceScreen
import com.apps.adrcotfas.goodtime.stats.StatisticsScreen
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import com.apps.adrcotfas.goodtime.ui.ObserveAsEvents
import com.apps.adrcotfas.goodtime.ui.SnackbarController
import com.apps.adrcotfas.goodtime.ui.popBackStack2
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main composable for the Goodtime app.
 * Contains all navigation, theming, and core UI logic.
 * Platform-agnostic and shared between Android and iOS.
 *
 * @param platformContext Platform-specific context for accessing platform APIs
 * @param timerViewModel ViewModel for timer functionality
 * @param mainViewModel ViewModel for main app state
 * @param themeSettings Current theme settings (resolved from system + user preferences)
 * @param onUpdateClicked Callback for when user clicks update button (null on iOS, Google Play only)
 */
@Composable
fun GoodtimeApp(
    platformContext: PlatformContext,
    timerViewModel: TimerViewModel = koinViewModel(),
    mainViewModel: MainViewModel,
    onUpdateClicked: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
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
        timerViewModel.onEnterForeground(coroutineScope)
        onPauseOrDispose {
            timerViewModel.onExitForeground()
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
                    executeDelayed(3000) {
                        platformContext.setFullscreen(true)
                        hideBottomBar = true
                    }
                }
        }
    }

    // Calculate start destination
    val startDestination =
        remember(mainUiState.showOnboarding) {
            if (mainUiState.showOnboarding) {
                OnboardingDest
            } else {
                MainDest
            }
        }

    ApplicationTheme(darkTheme = isDarkTheme, dynamicColor = uiState.isDynamicColor) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            isMainScreen = destination.route == MainDest.route
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

        // Handle snackbar events
        ObserveAsEvents(
            flow = SnackbarController.events,
            snackbarHostState,
        ) { event ->
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()

                val result =
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.action?.name,
                        duration = event.duration,
                    )

                if (result == SnackbarResult.ActionPerformed) {
                    event.action?.action?.invoke()
                }
            }
        }

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
                composable<OnboardingDest> { OnboardingScreen() }
                composable<MainDest> {
                    MainScreen(
                        onSurfaceClick = onSurfaceClick,
                        hideBottomBar = hideBottomBar,
                        navController = navController,
                        viewModel = timerViewModel,
                        mainViewModel = mainViewModel,
                        onUpdateClicked = onUpdateClicked ?: {},
                    )
                }
                composable<LabelsDest> {
                    LabelsScreen(
                        onNavigateToLabel = navController::navigate,
                        onNavigateToArchivedLabels = {
                            navController.navigate(ArchivedLabelsDest)
                        },
                        onNavigateToPro = { navController.navigate(ProDest) },
                        onNavigateBack = navController::popBackStack2,
                    )
                }
                composable<AddEditLabelDest> {
                    val addEditLabelDest = it.toRoute<AddEditLabelDest>()
                    AddEditLabelScreen(
                        labelName = addEditLabelDest.name,
                        onNavigateToDefault = { navController.navigate(TimerDurationsDest) },
                        onNavigateBack = navController::popBackStack2,
                    )
                }
                composable<ArchivedLabelsDest> {
                    ArchivedLabelsScreen(
                        onNavigateBack = navController::popBackStack2,
                    )
                }
                composable<StatsDest> {
                    StatisticsScreen(
                        onNavigateBack = navController::popBackStack2,
                    )
                }
                composable<SettingsDest> {
                    SettingsScreen(
                        onNavigateToUserInterface = {
                            navController.navigate(
                                UserInterfaceDest,
                            )
                        },
                        onNavigateToNotifications = {
                            navController.navigate(
                                NotificationSettingsDest,
                            )
                        },
                        onNavigateToDefaultLabel = {
                            navController.navigate(TimerDurationsDest)
                        },
                        onNavigateBack = navController::popBackStack2,
                    )
                }
                composable<TimerDurationsDest> {
                    TimerProfileScreen(
                        onNavigateBack = navController::popBackStack2,
                    )
                }
                composable<UserInterfaceDest> {
                    UserInterfaceScreen(
                        onNavigateToPro = { navController.navigate(ProDest) },
                        onNavigateBack = navController::popBackStack2,
                    )
                }
                composable<NotificationSettingsDest> {
                    NotificationsScreen(
                        onNavigateBack = navController::popBackStack2,
                    )
                }

                composable<BackupDest> {
                    BackupScreen(
                        onNavigateToPro = { navController.navigate(ProDest) },
                        onNavigateBack = navController::popBackStack2,
                    )
                }
                composable<AboutDest> {
                    AboutScreen(
                        onNavigateToLicenses = {
                            navController.navigate(
                                LicensesDest,
                            )
                        },
                        onNavigateToAcknowledgements = {
                            navController.navigate(
                                AcknowledgementsDest,
                            )
                        },
                        onNavigateBack = navController::popBackStack2,
                        onNavigateToMain = {
                            navController.navigate(MainDest) {
                                popUpTo(MainDest) {
                                    inclusive = true
                                }
                            }
                        },
                    )
                }
                composable<LicensesDest> {
                    LicensesScreen(onNavigateBack = navController::popBackStack2)
                }
                composable<AcknowledgementsDest> {
                    AcknowledgementsScreen(navController::popBackStack2)
                }
                composable<ProDest> {
                    ProScreen(onNavigateBack = { navController.popBackStack2() })
                }
            }
        }
    }
}

/**
 * Helper function to execute a block after a delay.
 */
private suspend fun executeDelayed(
    delay: Long,
    block: () -> Unit,
) {
    coroutineScope {
        delay(delay)
        block()
    }
}
