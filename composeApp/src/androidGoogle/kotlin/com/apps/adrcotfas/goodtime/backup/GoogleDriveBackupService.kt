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
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class GoogleDriveBackupService(
    private val googleDriveManager: GoogleDriveManager,
    private val backupManager: BackupFileManager,
    private val context: Context,
    private val logger: Logger,
)  {
    private val authorizationClient = Identity.getAuthorizationClient(context)
    private val workManager = WorkManager.getInstance(context)

    suspend fun auth(): GoogleDriveAuthResult {
        val request =
            AuthorizationRequest
                .builder()
                .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA)))
                .build()

        val result = authorizationClient.authorize(request).await()

        if (result.hasResolution()) {
            val pendingIntent =
                result.pendingIntent
                    ?: return GoogleDriveAuthResult.Error(Exception("No pending intent"))
            return GoogleDriveAuthResult.NeedsUserConsent(pendingIntent)
        } else {
            return GoogleDriveAuthResult.Success(result)
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        if (enabled) {
            schedulePeriodicBackup()
        } else {
            cancelAllBackupWork()
        }
    }

    fun backup() {
        scheduleOneTimeBackup()
    }

    suspend fun listAvailableBackups(): List<String> {
        val token = getAuthTokenOrNull()
        return if (token != null) {
            googleDriveManager.listBackups(token)
        } else {
            logger.e { " Cannot list backups without being connected" }
            emptyList()
        }
    }

    suspend fun restoreFromBackup(fileName: String): BackupPromptResult {
        logger.i { "restoreFromBackup() - $fileName" }

        val token = getAuthTokenOrNull()
        if (token == null) {
            logger.e { "Cannot restore without being connected" }
            return BackupPromptResult.FAILED
        }

        return try {
            val tempFilePath = googleDriveManager.downloadBackup(token, fileName)
            if (tempFilePath != null) {
                backupManager.restoreFromFile(tempFilePath)
            } else {
                logger.e { "restoreFromBackup() - download returned null" }
                BackupPromptResult.FAILED
            }
        } catch (e: TokenRevokedException) {
            logger.e(e) { "restoreFromBackup() - token revoked" }
            BackupPromptResult.FAILED
        } catch (e: Exception) {
            logger.e(e) { "restoreFromBackup() - failed" }
            BackupPromptResult.FAILED
        }
    }

    suspend fun disconnect() {
        val token = getAuthTokenOrNull()
        if (token == null) {
            logger.e { "Cannot disconnect without being connected" }
            return
        }
        authorizationClient
            .clearToken(ClearTokenRequest.builder().setToken(token).build())
            .await()
    }

    private fun buildNetworkConstraints(): Constraints =
        Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    private fun schedulePeriodicBackup() {
        val constraints = buildNetworkConstraints()

        val workRequest =
            PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(
                repeatInterval = 1L,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            ).setInitialDelay(1L, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

        workManager.enqueueUniquePeriodicWork(
            GoogleDriveBackupWorker.AUTO_BACKUP,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    private fun scheduleOneTimeBackup() {
        val constraints = buildNetworkConstraints()

        val workRequest =
            OneTimeWorkRequestBuilder<GoogleDriveBackupWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

        workManager.enqueueUniqueWork(
            GoogleDriveBackupWorker.MANUAL_BACKUP,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )
    }

    fun cancelAllBackupWork() {
        workManager.cancelUniqueWork(GoogleDriveBackupWorker.AUTO_BACKUP)
        workManager.cancelUniqueWork(GoogleDriveBackupWorker.MANUAL_BACKUP)
    }

    suspend fun getAuthTokenOrNull(): String? =
        when (val authResult = auth()) {
            is GoogleDriveAuthResult.Success -> {
                authResult.authResult.accessToken
            }

            else -> {
                null
            }
        }
}
