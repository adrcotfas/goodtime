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
package com.apps.adrcotfas.goodtime.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.IOS_LIVE_ACTIVITY_LISTENER
import com.apps.adrcotfas.goodtime.bl.IOS_NOTIFICATION_HANDLER
import com.apps.adrcotfas.goodtime.bl.IosLiveActivityListener
import com.apps.adrcotfas.goodtime.bl.IosNotificationHandler
import com.apps.adrcotfas.goodtime.bl.LiveActivityBridge
import com.apps.adrcotfas.goodtime.bl.SOUND_AND_VIBRATION_PLAYER
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.notifications.IosSoundPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.IosTorchManager
import com.apps.adrcotfas.goodtime.bl.notifications.IosVibrationPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.SoundPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.SoundVibrationAndTorchPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.TorchManager
import com.apps.adrcotfas.goodtime.bl.notifications.VibrationPlayer
import com.apps.adrcotfas.goodtime.common.FeedbackHelper
import com.apps.adrcotfas.goodtime.common.InstallDateProvider
import com.apps.adrcotfas.goodtime.common.IosFeedbackHelper
import com.apps.adrcotfas.goodtime.common.IosInstallDateProvider
import com.apps.adrcotfas.goodtime.common.IosTimeFormatProvider
import com.apps.adrcotfas.goodtime.common.IosUrlOpener
import com.apps.adrcotfas.goodtime.common.TimeFormatProvider
import com.apps.adrcotfas.goodtime.common.UrlOpener
import com.apps.adrcotfas.goodtime.data.backup.IosBackupPrompter
import com.apps.adrcotfas.goodtime.data.local.DATABASE_NAME
import com.apps.adrcotfas.goodtime.data.local.ProductivityDatabase
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.data.local.getDatabaseBuilder
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.settings.reminders.ReminderScheduler
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
actual val platformModule: Module =
    module {
        single<RoomDatabase.Builder<ProductivityDatabase>> { getDatabaseBuilder() }

        single<FileSystem> { FileSystem.SYSTEM }

        single<String>(named(DB_PATH_KEY)) {
            val documentDirectory: NSURL? =
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
            requireNotNull(documentDirectory).path + "/$DATABASE_NAME"
        }

        single<String>(named(FILES_DIR_PATH_KEY)) {
            val documentDirectory: NSURL? =
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
            requireNotNull(documentDirectory).path + "/tmp"
        }

        single<BackupPrompter> {
            IosBackupPrompter(
                logger = getWith("IosBackupPrompter"),
                mainScope = get<CoroutineScope>(named(MAIN_SCOPE)),
            )
        }

        single<DataStore<Preferences>>(named(SETTINGS_NAME)) {
            getDataStore(
                producePath = {
                    val documentDirectory: NSURL? =
                        NSFileManager.defaultManager.URLForDirectory(
                            directory = NSDocumentDirectory,
                            inDomain = NSUserDomainMask,
                            appropriateForURL = null,
                            create = false,
                            error = null,
                        )
                    requireNotNull(documentDirectory).path + "/$SETTINGS_FILE_NAME"
                },
            )
        }
        single<UrlOpener> { IosUrlOpener() }
        single<FeedbackHelper> { IosFeedbackHelper() }
        single<TimeFormatProvider> { IosTimeFormatProvider() }
        single<InstallDateProvider> { IosInstallDateProvider() }

        single<EventListener>(named(EventListener.IOS_NOTIFICATION_HANDLER)) {
            IosNotificationHandler(
                timeProvider = get<TimeProvider>(),
                settingsRepo = get<SettingsRepository>(),
                coroutineScope = get<CoroutineScope>(named(MAIN_SCOPE)),
                log = getWith("IosNotificationHandler"),
            )
        }

        single<LiveActivityBridge> { LiveActivityBridge.shared }

        single<EventListener>(named(EventListener.IOS_LIVE_ACTIVITY_LISTENER)) {
            IosLiveActivityListener(
                liveActivityBridge = get<LiveActivityBridge>(),
                timeProvider = get<TimeProvider>(),
                log = getWith("IosLiveActivityListener"),
            )
        }

        single<SoundPlayer> {
            IosSoundPlayer(
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                settingsRepo = get<SettingsRepository>(),
                logger = getWith("SoundPlayer"),
            )
        }

        single<VibrationPlayer> {
            IosVibrationPlayer(
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                settingsRepo = get<SettingsRepository>(),
                logger = getWith("VibrationPlayer"),
            )
        }

        single<TorchManager> {
            IosTorchManager(
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                settingsRepo = get<SettingsRepository>(),
                logger = getWith("TorchManager"),
            )
        }

        single<EventListener>(named(EventListener.SOUND_AND_VIBRATION_PLAYER)) {
            SoundVibrationAndTorchPlayer(
                soundPlayer = get(),
                vibrationPlayer = get(),
                torchManager = get(),
            )
        }

        single<List<EventListener>> {
            listOf(
                get<EventListener>(named(EventListener.IOS_NOTIFICATION_HANDLER)),
                get<EventListener>(named(EventListener.IOS_LIVE_ACTIVITY_LISTENER)),
                get<EventListener>(named(EventListener.SOUND_AND_VIBRATION_PLAYER)),
            )
        }

        single<ReminderScheduler> {
            ReminderScheduler(
                logger = getWith("ReminderScheduler"),
            )
        }
    }

@OptIn(ExperimentalNativeApi::class)
actual fun isDebug(): Boolean = Platform.isDebugBinary
