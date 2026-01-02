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

import com.apps.adrcotfas.goodtime.backup.BackupManager
import com.apps.adrcotfas.goodtime.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.backup.BackupViewModel
import com.apps.adrcotfas.goodtime.backup.CloudBackupService
import com.apps.adrcotfas.goodtime.backup.LocalBackupService
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.local.ProductivityDatabase
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

expect val platformBackupModule: Module

val coreBackupModule: Module =
    module {
        single<BackupManager> {
            BackupManager(
                get<FileSystem>(),
                get<String>(named(DB_PATH_KEY)),
                get<String>(named(CACHE_DIR_PATH_KEY)),
                get<ProductivityDatabase>(),
                get<TimeProvider>(),
                get<BackupPrompter>(),
                get<LocalDataRepository>(),
                getWith("BackupManager"),
            )
        }

        single<LocalBackupService> { LocalBackupService(backupManager = get<BackupManager>()) }

        viewModel {
            BackupViewModel(
                localBackupService = get<LocalBackupService>(),
                cloudBackupService = getOrNull<CloudBackupService>(),
                settingsRepository = get<SettingsRepository>(),
            )
        }
    }
