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

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.main.GoodtimeMainActivity
import com.apps.adrcotfas.goodtime.main.TimerViewModel
import com.apps.adrcotfas.goodtime.platform.PlatformContext
import com.apps.adrcotfas.goodtime.platform.configureSystemBars
import com.apps.adrcotfas.goodtime.ui.collectThemeSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject

/**
 * MainActivity - Thin lifecycle wrapper for GoodtimeApp.
 * Handles Android-specific initialization and delegates UI to shared GoodtimeApp composable.
 */
class MainActivity : GoodtimeMainActivity() {
    private val notificationManager: NotificationArchManager by inject()
    private val timerViewModel: TimerViewModel by viewModel<TimerViewModel>()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        log.d { "onCreate" }

        // Configure window for edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Force screen to always stay on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Keep splash screen visible while loading
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.loading }

        setContent {
            val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Collect theme settings using Android-specific implementation
            val themeSettings by collectThemeSettings(
                mainUiState = mainUiState,
                showOnboarding = mainUiState.showOnboarding,
            )

            // Create platform context for accessing Android APIs
            val platformContext = remember { PlatformContext(this) }

            // Configure system bars when theme changes
            LaunchedEffect(themeSettings.darkTheme) {
                platformContext.configureSystemBars(
                    isDarkTheme = themeSettings.darkTheme,
                    isFullscreen = false, // Updated dynamically in GoodtimeApp
                )
            }

            // Call shared GoodtimeApp composable
            GoodtimeApp(
                platformContext = platformContext,
                timerViewModel = timerViewModel,
                mainViewModel = viewModel,
                themeSettings = themeSettings,
                onUpdateClicked = { triggerAppUpdate() },
            )
        }
    }

    override fun onDestroy() {
        log.d { "onDestroy" }
        notificationManager.clearFinishedNotification()
        super.onDestroy()
    }
}
