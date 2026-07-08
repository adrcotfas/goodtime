/**
 *     Goodtime Productivity
 *     Copyright (C) 2026 Adrian Cotfas
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

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.apps.adrcotfas.goodtime.backup.BackupScreen
import com.apps.adrcotfas.goodtime.billing.ProScreen
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
import com.apps.adrcotfas.goodtime.main.UserInterfaceDest
import com.apps.adrcotfas.goodtime.onboarding.MainViewModel
import com.apps.adrcotfas.goodtime.onboarding.OnboardingScreen
import com.apps.adrcotfas.goodtime.settings.SettingsScreen
import com.apps.adrcotfas.goodtime.settings.about.AboutScreen
import com.apps.adrcotfas.goodtime.settings.about.AcknowledgementsScreen
import com.apps.adrcotfas.goodtime.settings.about.LicensesScreen
import com.apps.adrcotfas.goodtime.settings.notifications.NotificationsScreen
import com.apps.adrcotfas.goodtime.settings.timerdurations.TimerProfileScreen
import com.apps.adrcotfas.goodtime.settings.timerstyle.UserInterfaceScreen
import com.apps.adrcotfas.goodtime.stats.StatisticsScreen
import com.apps.adrcotfas.goodtime.ui.popBackStack2

/**
 * Registers every app destination on the [NavHost] graph.
 * Keeps the screen-wiring out of [GoodtimeApp], which owns the app shell.
 */
fun NavGraphBuilder.goodtimeNavGraph(
    navController: NavController,
    mainViewModel: MainViewModel,
    onSurfaceClick: () -> Unit,
    hideBottomBar: Boolean,
    onUpdateClicked: (() -> Unit)?,
) {
    composable<OnboardingDest> { OnboardingScreen() }
    composable<MainDest> {
        MainScreen(
            onSurfaceClick = onSurfaceClick,
            hideBottomBar = hideBottomBar,
            navController = navController,
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
            onNavigateToMainAndReset = {
                navController.navigate(MainDest) {
                    popUpTo(MainDest) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }
    composable<AboutDest> {
        AboutScreen(
            mainViewModel = mainViewModel,
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
