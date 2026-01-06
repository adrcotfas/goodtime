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

import com.apps.adrcotfas.goodtime.backup.BackupFileManager
import com.apps.adrcotfas.goodtime.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.backup.CloudBackupManager
import com.apps.adrcotfas.goodtime.backup.ICloudBackupService
import com.apps.adrcotfas.goodtime.data.backup.IosBackupPrompter
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformBackupModule: Module =
    module {
        single<BackupPrompter> {
            IosBackupPrompter(
                logger = getWith("IosBackupPrompter"),
                mainScope = get<CoroutineScope>(named(MAIN_SCOPE)),
            )
        }

        single<CloudBackupManager>(createdAtStart = true) {
            CloudBackupManager(
                backupManager = get<BackupFileManager>(),
                settingsRepository = get<SettingsRepository>(),
                fileSystem = get<FileSystem>(),
                dbPath = get<String>(named(DB_PATH_KEY)),
                logger = getWith("CloudBackupManager"),
            )
        }

        single<CloudBackupService> {
            ICloudBackupService(
                cloudBackupManager = get<CloudBackupManager>(),
                backupManager = get<BackupFileManager>(),
                logger = getWith("ICloudBackupService"),
            )
        }
    }
