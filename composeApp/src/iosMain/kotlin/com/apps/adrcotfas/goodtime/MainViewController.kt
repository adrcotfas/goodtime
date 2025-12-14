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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.apps.adrcotfas.goodtime.billing.BillingAbstract
import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.FinishActionType
import com.apps.adrcotfas.goodtime.bl.IOS_NOTIFICATION_HANDLER
import com.apps.adrcotfas.goodtime.bl.IosNotificationHandler
import com.apps.adrcotfas.goodtime.bl.TimerManager
import com.apps.adrcotfas.goodtime.di.MAIN_SCOPE
import com.apps.adrcotfas.goodtime.di.coreModule
import com.apps.adrcotfas.goodtime.di.coroutineScopeModule
import com.apps.adrcotfas.goodtime.di.localDataModule
import com.apps.adrcotfas.goodtime.di.mainModule
import com.apps.adrcotfas.goodtime.di.platformModule
import com.apps.adrcotfas.goodtime.di.timerManagerModule
import com.apps.adrcotfas.goodtime.di.viewModelModule
import com.apps.adrcotfas.goodtime.main.TimerViewModel
import com.apps.adrcotfas.goodtime.onboarding.MainViewModel
import com.apps.adrcotfas.goodtime.platform.PlatformContext
import com.apps.adrcotfas.goodtime.settings.reminders.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import platform.UIKit.UIViewController

@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        AppWithKoin()
    }

@Composable
private fun AppWithKoin() {
    KoinApplication(
        application = {
            modules(
                iosFlavorModule,
                coroutineScopeModule,
                coreModule,
                localDataModule,
                timerManagerModule,
                mainModule,
                viewModelModule,
                platformModule,
            )
        },
    ) {
        val billing: BillingAbstract = koinInject()
        billing.init()

        val timerViewModel: TimerViewModel = koinInject()
        val mainViewModel: MainViewModel = koinInject()

        initNotificationHandler()
        initReminderManager()

        val platformContext = remember { PlatformContext() }

        GoodtimeApp(
            platformContext = platformContext,
            timerViewModel = timerViewModel,
            mainViewModel = mainViewModel,
            onUpdateClicked = null,
        )
    }
}

@Composable
private fun initNotificationHandler() {
    val notificationHandler = koinInject<EventListener>(named(EventListener.IOS_NOTIFICATION_HANDLER)) as IosNotificationHandler
    val timerManager: TimerManager = koinInject()
    notificationHandler.init {
        timerManager.next(finishActionType = FinishActionType.MANUAL_NEXT)
    }
}

@Composable
private fun initReminderManager() {
    val reminderManager: ReminderManager = koinInject()
    val scope: CoroutineScope = koinInject(named(MAIN_SCOPE))
    scope.launch {
        reminderManager.init()
    }
}
