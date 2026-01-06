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
import com.apps.adrcotfas.goodtime.backup.BackupFileManager
import com.apps.adrcotfas.goodtime.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.backup.LocalAutoBackupManager
import com.apps.adrcotfas.goodtime.backup.LocalAutoBackupWorker
import com.apps.adrcotfas.goodtime.data.backup.ActivityResultLauncherManager
import com.apps.adrcotfas.goodtime.data.backup.AndroidBackupPrompter
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val androidCommonBackupModule: Module =
    module {
        single<ActivityResultLauncherManager> {
            ActivityResultLauncherManager(
                context = get<Context>(),
                coroutineScope = get<CoroutineScope>(named(MAIN_SCOPE)),
            )
        }

        single<BackupPrompter> {
            AndroidBackupPrompter(
                activityResultLauncherManager = get<ActivityResultLauncherManager>(),
            )
        }

        single<LocalAutoBackupManager>(createdAtStart = true) {
            LocalAutoBackupManager(
                context = get<Context>(),
                settingsRepository = get<SettingsRepository>(),
                logger = getWith("LocalAutoBackupManager"),
            )
        }

        worker {
            LocalAutoBackupWorker(
                context = get<Context>(),
                backupManager = get<BackupFileManager>(),
                settingsRepository = get<SettingsRepository>(),
                logger = getWith("LocalAutoBackupWorker"),
                dbPath = get<String>(named(DB_PATH_KEY)),
                params = get(),
            )
        }
    }
