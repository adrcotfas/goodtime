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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import com.apps.adrcotfas.goodtime.BuildConfig
import com.apps.adrcotfas.goodtime.bl.ALARM_MANAGER_HANDLER
import com.apps.adrcotfas.goodtime.bl.DND_MODE_MANAGER
import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.SESSION_RESET_HANDLER
import com.apps.adrcotfas.goodtime.bl.SOUND_AND_VIBRATION_PLAYER
import com.apps.adrcotfas.goodtime.bl.TIMER_SERVICE_HANDLER
import com.apps.adrcotfas.goodtime.bl.notifications.AndroidSoundPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.AndroidTorchManager
import com.apps.adrcotfas.goodtime.bl.notifications.AndroidVibrationPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.SoundPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.SoundVibrationAndTorchPlayer
import com.apps.adrcotfas.goodtime.bl.notifications.TorchManager
import com.apps.adrcotfas.goodtime.bl.notifications.VibrationPlayer
import com.apps.adrcotfas.goodtime.common.AndroidFeedbackHelper
import com.apps.adrcotfas.goodtime.common.AndroidInstallDateProvider
import com.apps.adrcotfas.goodtime.common.AndroidTimeFormatProvider
import com.apps.adrcotfas.goodtime.common.AndroidUrlOpener
import com.apps.adrcotfas.goodtime.common.FeedbackHelper
import com.apps.adrcotfas.goodtime.common.InstallDateProvider
import com.apps.adrcotfas.goodtime.common.TimeFormatProvider
import com.apps.adrcotfas.goodtime.common.UrlOpener
import com.apps.adrcotfas.goodtime.data.local.DATABASE_NAME
import com.apps.adrcotfas.goodtime.data.local.ProductivityDatabase
import com.apps.adrcotfas.goodtime.data.local.getDatabaseBuilder
import com.apps.adrcotfas.goodtime.settings.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<RoomDatabase.Builder<ProductivityDatabase>> { getDatabaseBuilder(get<Context>()) }
        single<FileSystem> { FileSystem.SYSTEM }
        single<String>(named(DB_PATH_KEY)) { getDbPath { get<Context>().getDatabasePath(DATABASE_NAME).absolutePath } }
        single<String>(named(FILES_DIR_PATH_KEY)) { getTmpPath { get<Context>().filesDir.absolutePath + "/tmp" } }

        single<DataStore<Preferences>>(named(SETTINGS_NAME)) {
            getDataStore(
                producePath = { get<Context>().filesDir.resolve(SETTINGS_FILE_NAME).absolutePath },
            )
        }
        single<SoundPlayer> {
            AndroidSoundPlayer(
                context = get(),
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                settingsRepo = get(),
                logger = getWith("SoundPlayer"),
            )
        }
        single<VibrationPlayer> {
            AndroidVibrationPlayer(
                context = get(),
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                settingsRepo = get(),
            )
        }
        single<TorchManager> {
            AndroidTorchManager(
                context = get(),
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                settingsRepo = get(),
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
                get<EventListener>(named(EventListener.TIMER_SERVICE_HANDLER)),
                get<EventListener>(named(EventListener.ALARM_MANAGER_HANDLER)),
                get<EventListener>(named(EventListener.SOUND_AND_VIBRATION_PLAYER)),
                get<EventListener>(named(EventListener.SESSION_RESET_HANDLER)),
                get<EventListener>(named(EventListener.DND_MODE_MANAGER)),
            )
        }
        single<UrlOpener> { AndroidUrlOpener(get()) }
        single<FeedbackHelper> { AndroidFeedbackHelper(get()) }
        single<TimeFormatProvider> { AndroidTimeFormatProvider(get()) }
        single<InstallDateProvider> { AndroidInstallDateProvider(get()) }

        single<ReminderScheduler> {
            ReminderScheduler(
                context = get(),
                timeProvider = get(),
                logger = getWith("ReminderScheduler"),
            )
        }
    }

actual fun isDebug(): Boolean = BuildConfig.DEBUG
