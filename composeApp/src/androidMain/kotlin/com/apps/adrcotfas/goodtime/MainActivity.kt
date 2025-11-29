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

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.apps.adrcotfas.goodtime.billing.ProScreen
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.data.settings.isDarkTheme
import com.apps.adrcotfas.goodtime.labels.addedit.AddEditLabelScreen
import com.apps.adrcotfas.goodtime.labels.archived.ArchivedLabelsScreen
import com.apps.adrcotfas.goodtime.labels.main.LabelsScreen
import com.apps.adrcotfas.goodtime.main.AboutDest
import com.apps.adrcotfas.goodtime.main.AcknowledgementsDest
import com.apps.adrcotfas.goodtime.main.AddEditLabelDest
import com.apps.adrcotfas.goodtime.main.ArchivedLabelsDest
import com.apps.adrcotfas.goodtime.main.BackupDest
import com.apps.adrcotfas.goodtime.main.GoodtimeMainActivity
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
import com.apps.adrcotfas.goodtime.onboarding.OnboardingScreen
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
import com.apps.adrcotfas.goodtime.ui.isSystemInDarkTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject

class MainActivity : GoodtimeMainActivity() {
    private val notificationManager: NotificationArchManager by inject()
    private val timerViewModel: TimerViewModel by viewModel<TimerViewModel>()
    private var fullScreenJob: Job? = null
    private var timerStateJob: Job? = null

    override fun onResume() {
        super.onResume()
        timerViewModel.onBringToForeground()
        timerStateJob =
            lifecycleScope.launch {
                timerViewModel.listenForeground()
            }
    }

    override fun onPause() {
        timerViewModel.onSendToBackground()
        timerStateJob?.cancel()
        timerStateJob = null
        super.onPause()
    }

    @SuppressLint("UnrememberedGetBackStackEntry", "UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        log.d { "onCreate" }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        var themeSettings by mutableStateOf(
            ThemeSettings(
                darkTheme = resources.configuration.isSystemInDarkTheme,
                isDynamicTheme = false,
            ),
        )

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    isSystemInDarkTheme(),
                    viewModel.uiState,
                ) { systemDark, uiState ->
                    ThemeSettings(
                        darkTheme = uiState.darkThemePreference.isDarkTheme(systemDark),
                        isDynamicTheme = uiState.isDynamicColor,
                    )
                }.onEach { themeSettings = it }
                    .map { it.darkTheme }
                    .distinctUntilChanged()
                    .collect { darkTheme ->
                        enableEdgeToEdge(
                            statusBarStyle =
                                SystemBarStyle.auto(
                                    lightScrim = android.graphics.Color.TRANSPARENT,
                                    darkScrim = android.graphics.Color.TRANSPARENT,
                                ) { darkTheme },
                            navigationBarStyle =
                                SystemBarStyle.auto(
                                    lightScrim = lightScrim,
                                    darkScrim = darkScrim,
                                    detectDarkMode = { darkTheme },
                                ),
                        )
                    }
            }
        }

        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.loading }

        setContent {
            val coroutineScope = rememberCoroutineScope()

            val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDarkTheme = themeSettings.darkTheme
            val showWhenLocked = uiState.showWhenLocked
            val isActive = uiState.isActive
            val keepScreenOn = uiState.keepScreenOn
            val isFinished = uiState.isFinished
            var isMainScreen by rememberSaveable { mutableStateOf(true) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(showWhenLocked)
            }

            val fullscreenMode = isMainScreen && uiState.fullscreenMode
            LaunchedEffect(fullscreenMode) {
                fullscreenMode.let {
                    toggleFullscreen(it)
                    if (!it) fullScreenJob?.cancel()
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
                            toggleFullscreen(false)
                            hideBottomBar = false
                            executeDelayed(3000) {
                                toggleFullscreen(true)
                                hideBottomBar = true
                            }
                        }
                }
            }

            toggleKeepScreenOn(isActive && keepScreenOn)
            val startDestination =
                remember(mainUiState.showOnboarding) {
                    if (mainUiState.showOnboarding) {
                        OnboardingDest
                    } else {
                        MainDest
                    }
                }

            ApplicationTheme(darkTheme = isDarkTheme, dynamicColor = themeSettings.isDynamicTheme) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                navController.addOnDestinationChangedListener { _, destination, _ ->
                    isMainScreen = destination.route == MainDest.route
                }

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
                                duration = SnackbarDuration.Long,
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
                                mainViewModel = viewModel,
                                onUpdateClicked = { this@MainActivity.triggerAppUpdate() },
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
                            ProScreen(onNavigateBack = navController::popBackStack2)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        log.d { "onDestroy" }
        notificationManager.clearFinishedNotification()
        super.onDestroy()
    }

    private fun toggleKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun toggleFullscreen(enabled: Boolean) {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)

        if (enabled) {
            windowInsetsController.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private suspend fun executeDelayed(
        delay: Long,
        block: () -> Unit,
    ) {
        coroutineScope {
            delay(delay)
            block()
        }
    }
}

fun NavController.popBackStack2(): Boolean {
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        return this.popBackStack()
    }
    return false
}

private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

data class ThemeSettings(
    val darkTheme: Boolean,
    val isDynamicTheme: Boolean,
)
