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

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.apps.adrcotfas.goodtime.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.backup.LocalAutoBackupManager
import com.apps.adrcotfas.goodtime.backup.LocalAutoBackupWorker
import com.apps.adrcotfas.goodtime.billing.PurchaseManager
import com.apps.adrcotfas.goodtime.billing.configurePurchasesFromPlatform
import com.apps.adrcotfas.goodtime.bl.ALARM_MANAGER_HANDLER
import com.apps.adrcotfas.goodtime.bl.AlarmManagerHandler
import com.apps.adrcotfas.goodtime.bl.DND_MODE_MANAGER
import com.apps.adrcotfas.goodtime.bl.DndModeManager
import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.TIMER_SERVICE_HANDLER
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.TimerServiceStarter
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.data.backup.ActivityResultLauncherManager
import com.apps.adrcotfas.goodtime.data.backup.AndroidBackupPrompter
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.di.DB_PATH_KEY
import com.apps.adrcotfas.goodtime.di.IO_SCOPE
import com.apps.adrcotfas.goodtime.di.billingModule
import com.apps.adrcotfas.goodtime.di.coreModule
import com.apps.adrcotfas.goodtime.di.coroutineScopeModule
import com.apps.adrcotfas.goodtime.di.getWith
import com.apps.adrcotfas.goodtime.di.localDataModule
import com.apps.adrcotfas.goodtime.di.mainModule
import com.apps.adrcotfas.goodtime.di.platformModule
import com.apps.adrcotfas.goodtime.di.timerManagerModule
import com.apps.adrcotfas.goodtime.di.viewModelModule
import com.apps.adrcotfas.goodtime.settings.notifications.SoundsViewModel
import com.apps.adrcotfas.goodtime.settings.reminders.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.acra.ACRA
import org.acra.config.mailSender
import org.acra.config.notification
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.koin.android.ext.android.get
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

class GoodtimeApplication :
    Application(),
    KoinComponent,
    Configuration.Provider {
    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        if (ACRA.isACRASenderServiceProcess()) return
        configurePurchasesFromPlatform()
        startKoin {
            modules(
                module {
                    single<Context> { this@GoodtimeApplication }
                    single<ActivityResultLauncherManager> {
                        ActivityResultLauncherManager(
                            get(),
                            coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                        )
                    }

                    single<BackupPrompter> {
                        AndroidBackupPrompter(get())
                    }
                    single<NotificationArchManager> {
                        NotificationArchManager(
                            get<Context>(),
                            MainActivity::class.java,
                            coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                        )
                    }
                    single<EventListener>(named(EventListener.TIMER_SERVICE_HANDLER)) {
                        TimerServiceStarter(get())
                    }
                    single<EventListener>(named(EventListener.ALARM_MANAGER_HANDLER)) {
                        AlarmManagerHandler(
                            get<Context>(),
                            get<TimeProvider>(),
                            getWith("AlarmManagerHandler"),
                        )
                    }
                    viewModel<SoundsViewModel> {
                        SoundsViewModel(
                            settingsRepository = get(),
                        )
                    }

                    single<EventListener>(named(EventListener.DND_MODE_MANAGER)) {
                        DndModeManager(
                            notificationManager = get<NotificationArchManager>(),
                            settingsRepository = get<SettingsRepository>(),
                            coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                        )
                    }
                    single(createdAtStart = true) {
                        LocalAutoBackupManager(
                            context = get(),
                            settingsRepository = get<SettingsRepository>(),
                            logger = getWith("AutoBackupManager"),
                        )
                    }
                    worker {
                        LocalAutoBackupWorker(
                            get(),
                            get(),
                            get(),
                            getWith("AutoBackupWorker"),
                            get<String>(named(DB_PATH_KEY)),
                            get(),
                        )
                    }
                },
                coroutineScopeModule,
                billingModule,
                platformModule,
                coreModule,
                localDataModule,
                timerManagerModule,
                viewModelModule,
                mainModule,
            )
            workManagerFactory()
        }

        initBilling()

        val reminderManager = get<ReminderManager>()
        applicationScope.launch {
            reminderManager.init()
        }
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)

        initAcra {
            alsoReportToAndroidFramework = true
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON

            notification {
                // required
                title = context.getString(R.string.main_crash_notification_title)
                // required
                text = context.getString(R.string.main_crash_notification_desc)
                // required
                channelName = context.getString(R.string.main_crash_channel_name)
                resSendButtonIcon = null
                resDiscardButtonIcon = null
            }
            mailSender {
                mailTo = context.getString(R.string.contact_address)
                subject = context.getString(R.string.crash_report_title)
                reportFileName = "crash.txt"
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            if (BuildConfig.DEBUG) {
                Configuration
                    .Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build()
            } else {
                Configuration
                    .Builder()
                    .setMinimumLoggingLevel(android.util.Log.ERROR)
                    .build()
            }

    private fun initBilling() {
        get<PurchaseManager>().start()
    }
}
