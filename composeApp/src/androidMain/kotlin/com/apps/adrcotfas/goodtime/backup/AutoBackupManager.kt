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
package com.apps.adrcotfas.goodtime.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.settings.BackupSettings
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Manager for scheduling and canceling auto backup operations.
 * It observes the BackupSettings from SettingsRepository and schedules or cancels
 * the backup work accordingly.
 */
class AutoBackupManager(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
) {
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        logger.i { "AutoBackupManager initialized" }
        observeBackupSettings()
    }

    private fun observeBackupSettings() {
        scope.launch {
            settingsRepository.settings
                .map { it.backupSettings }
                .distinctUntilChanged()
                .collect { backupSettings ->
                    handleBackupSettingsChange(backupSettings)
                }
        }
    }

    private fun handleBackupSettingsChange(backupSettings: BackupSettings) {
        logger.i {
            "Backup settings changed: autoBackupEnabled=${backupSettings.autoBackupEnabled}, " +
                "path=${backupSettings.path}"
        }

        if (backupSettings.autoBackupEnabled && backupSettings.path.isNotBlank()) {
            scheduleBackup()
            logger.i { "Auto backup scheduled with path: ${backupSettings.path}" }
        } else {
            cancelBackup()
            logger.i { "Auto backup canceled" }
        }
    }

    private fun scheduleBackup() {
        val constraints =
            Constraints
                .Builder()
                .setRequiresCharging(true)
                .build()

        val backupWorkRequest =
            PeriodicWorkRequestBuilder<AutoBackupWorker>(
                repeatInterval = 1L,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            ).setInitialDelay(10L, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

        // Use UPDATE policy to reschedule when settings change
        workManager.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupWorkRequest,
        )
    }

    private fun cancelBackup() {
        logger.i { "Auto backup canceled" }
        workManager.cancelUniqueWork(AutoBackupWorker.WORK_NAME)
    }
}
