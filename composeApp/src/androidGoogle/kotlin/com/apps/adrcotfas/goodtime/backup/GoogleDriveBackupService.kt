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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Sealed class representing authorization state for Google Drive operations.
 */
sealed class GoogleDriveAuthState {
    data object Idle : GoogleDriveAuthState()

    data class NeedsConsent(
        val pendingIntent: PendingIntent,
    ) : GoogleDriveAuthState()

    data object Authorized : GoogleDriveAuthState()

    data class Failed(
        val message: String,
    ) : GoogleDriveAuthState()
}

/**
 * Google Drive implementation of CloudBackupService.
 *
 * Handles backup and restore operations to Google Drive's appDataFolder,
 * which is a hidden app-specific folder that users cannot access directly.
 */
class GoogleDriveBackupService(
    private val context: Context,
    private val googleDriveAuthManager: GoogleDriveAuthManager,
    private val googleDriveManager: GoogleDriveManager,
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
) : CloudBackupService {
    private val workManager = WorkManager.getInstance(context)

    // Temporarily holds access token for current operation
    private var currentAccessToken: String? = null

    // When true, operations require re-authorization through the consent UI
    private var isDisconnected: Boolean = false

    // Exposes auth state to UI for handling consent flow
    private val _authState = MutableStateFlow<GoogleDriveAuthState>(GoogleDriveAuthState.Idle)
    val authState: StateFlow<GoogleDriveAuthState> = _authState.asStateFlow()

    /**
     * Check if Google Drive is available and authorized.
     * Returns null if operations should succeed, or an issue if not.
     */
    override suspend fun preflightBackup(): CloudAutoBackupIssue? {
        logger.d { "preflightBackup() - checking authorization" }
        return tryAuthorize()
    }

    private suspend fun tryAuthorize(): CloudAutoBackupIssue? {
        // If disconnected, block all operations until user explicitly reconnects
        // by enabling auto-backup again
        if (isDisconnected) {
            logger.d { "tryAuthorize() - disconnected, blocking operation" }
            return CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_FAILED
        }

        return when (val authResult = googleDriveAuthManager.authorize()) {
            is GoogleDriveAuthResult.Success -> {
                currentAccessToken = authResult.accessToken
                logger.d { "tryAuthorize() - authorized" }
                null
            }
            is GoogleDriveAuthResult.NeedsUserConsent -> {
                logger.d { "tryAuthorize() - needs user consent" }
                _authState.value = GoogleDriveAuthState.NeedsConsent(authResult.pendingIntent)
                CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_REQUIRED
            }
            is GoogleDriveAuthResult.Error -> {
                logger.e(authResult.exception) { "tryAuthorize() - authorization failed: ${authResult.exception.message}" }
                _authState.value = GoogleDriveAuthState.Failed(authResult.exception.message ?: "Unknown error")
                CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_FAILED
            }
        }
    }

    /**
     * Enable or disable the periodic backup worker.
     */
    override fun setAutoBackupEnabled(enabled: Boolean) {
        if (enabled) {
            scheduleBackupWorker()
            logger.i { "setAutoBackupEnabled() - worker scheduled" }
        } else {
            cancelBackupWorker()
            logger.i { "setAutoBackupEnabled() - worker cancelled" }
        }
    }

    /**
     * Perform an immediate backup to Google Drive.
     */
    override suspend fun backupNow(): BackupPromptResult {
        logger.i { "backupNow() - starting" }

        val token = getAccessTokenOrFail() ?: return BackupPromptResult.FAILED

        return try {
            val fileId = googleDriveManager.uploadBackup(token)
            if (fileId != null) {
                // Update timestamp
                val currentSettings = settingsRepository.settings.first().backupSettings
                settingsRepository.setBackupSettings(
                    currentSettings.copy(cloudLastBackupTimestamp = TimeProvider.now()),
                )
                logger.i { "backupNow() - success" }
                BackupPromptResult.SUCCESS
            } else {
                logger.e { "backupNow() - upload returned null" }
                BackupPromptResult.FAILED
            }
        } catch (e: Exception) {
            logger.e(e) { "backupNow() - failed" }
            BackupPromptResult.FAILED
        }
    }

    /**
     * List available backups from Google Drive.
     */
    override suspend fun listAvailableBackups(): List<String> {
        logger.d { "listAvailableBackups() - fetching" }

        val token = getAccessTokenOrFail() ?: return emptyList()

        return try {
            googleDriveManager.listBackups(token)
        } catch (e: Exception) {
            logger.e(e) { "listAvailableBackups() - failed" }
            emptyList()
        }
    }

    /**
     * Restore from a specific backup file.
     */
    override suspend fun restoreFromBackup(fileName: String): BackupPromptResult {
        logger.i { "restoreFromBackup() - $fileName" }

        val token = getAccessTokenOrFail() ?: return BackupPromptResult.FAILED

        return try {
            val tempFilePath = googleDriveManager.downloadBackup(token, fileName)
            if (tempFilePath != null) {
                backupManager.restoreFromFile(tempFilePath)
            } else {
                logger.e { "restoreFromBackup() - download returned null" }
                BackupPromptResult.FAILED
            }
        } catch (e: Exception) {
            logger.e(e) { "restoreFromBackup() - failed" }
            BackupPromptResult.FAILED
        }
    }

    /**
     * Attempt to enable auto-backup.
     *
     * This will:
     * 1. Check authorization (may return AUTH_REQUIRED if consent needed)
     * 2. Perform an initial backup
     * 3. Return null on success, or an issue on failure
     */
    override suspend fun attemptEnableAutoBackup(): CloudAutoBackupIssue? {
        logger.d { "attemptEnableAutoBackup() - starting" }

        // If we already have a token (e.g., from handleAuthResult after consent),
        // skip preflight to avoid calling authorize() again which may return a stale token
        if (currentAccessToken == null) {
            val preflight = preflightBackup()
            if (preflight != null) {
                logger.d { "attemptEnableAutoBackup() - preflight failed: $preflight" }
                return preflight
            }
        }

        // Perform initial backup
        return try {
            val token = currentAccessToken
            if (token == null) {
                logger.e { "attemptEnableAutoBackup() - no token after preflight" }
                return CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_FAILED
            }

            val fileId = googleDriveManager.uploadBackup(token)
            if (fileId != null) {
                // Update timestamp
                val currentSettings = settingsRepository.settings.first().backupSettings
                settingsRepository.setBackupSettings(
                    currentSettings.copy(cloudLastBackupTimestamp = TimeProvider.now()),
                )
                logger.i { "attemptEnableAutoBackup() - initial backup success" }
                null
            } else {
                logger.e { "attemptEnableAutoBackup() - initial backup failed" }
                CloudAutoBackupIssue.GOOGLE_DRIVE_UNAVAILABLE
            }
        } catch (e: Exception) {
            logger.e(e) { "attemptEnableAutoBackup() - failed" }
            e.toCloudAutoBackupIssue()
        }
    }

    /**
     * Process the result from the consent UI.
     * Call this after the user completes the Google authorization consent flow.
     *
     * After consent, we call authorize() again to get a fresh token rather than
     * using the token from the intent, which may not be suitable for Drive API calls.
     *
     * @param data The Intent data from the activity result
     * @return true if authorization was successful
     */
    suspend fun handleAuthResult(data: Intent?): Boolean {
        // Get the token from the consent result
        val token = googleDriveAuthManager.getAuthorizationResultFromIntent(data)
        if (token == null) {
            _authState.value = GoogleDriveAuthState.Failed("Failed to get authorization result")
            logger.e { "handleAuthResult() - failed to parse consent result" }
            return false
        }

        // Use the token directly from the consent flow
        currentAccessToken = token
        isDisconnected = false
        _authState.value = GoogleDriveAuthState.Authorized
        logger.d { "handleAuthResult() - got token from consent (length=${token.length})" }
        return true
    }

    /**
     * Reset auth state to Idle. Call this after auth UI completes.
     * Does NOT clear the access token - that's handled by signOut().
     */
    fun resetAuthState() {
        _authState.value = GoogleDriveAuthState.Idle
    }

    /**
     * Disconnect from Google Drive.
     * Clears local state. The next operation will call authorize() again.
     * Note: This does NOT revoke permissions with Google. If the user wants to
     * switch accounts, they need to revoke access from myaccount.google.com.
     */
    fun disconnect() {
        currentAccessToken = null
        isDisconnected = true
        _authState.value = GoogleDriveAuthState.Idle
        logger.i { "disconnect() - local state cleared" }
    }

    /**
     * Reconnect to Google Drive after being disconnected.
     * Initiates the authorization flow.
     * Returns null on success, or an issue if authorization failed/needs consent.
     */
    suspend fun reconnect(): CloudAutoBackupIssue? {
        logger.d { "reconnect() - starting authorization flow" }

        // Clear disconnected state first so tryAuthorize() doesn't block
        isDisconnected = false

        return when (val authResult = googleDriveAuthManager.authorize()) {
            is GoogleDriveAuthResult.Success -> {
                currentAccessToken = authResult.accessToken
                _authState.value = GoogleDriveAuthState.Authorized
                logger.d { "reconnect() - authorized" }
                null
            }
            is GoogleDriveAuthResult.NeedsUserConsent -> {
                logger.d { "reconnect() - needs user consent" }
                _authState.value = GoogleDriveAuthState.NeedsConsent(authResult.pendingIntent)
                CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_REQUIRED
            }
            is GoogleDriveAuthResult.Error -> {
                // Re-set disconnected on error so user must try again
                isDisconnected = true
                logger.e(authResult.exception) { "reconnect() - authorization failed" }
                _authState.value = GoogleDriveAuthState.Failed(authResult.exception.message ?: "Unknown error")
                CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_FAILED
            }
        }
    }

    /**
     * Get current pending intent if consent is needed.
     * Used by UI to launch the consent flow.
     */
    fun getPendingIntentForConsent(): PendingIntent? {
        val state = _authState.value
        return if (state is GoogleDriveAuthState.NeedsConsent) {
            state.pendingIntent
        } else {
            null
        }
    }

    /**
     * Get access token, authorizing if needed.
     * Returns null if authorization failed.
     */
    private suspend fun getAccessTokenOrFail(): String? {
        // Use cached token if available
        currentAccessToken?.let { return it }

        // Try to authorize
        return when (val authResult = googleDriveAuthManager.authorize()) {
            is GoogleDriveAuthResult.Success -> {
                currentAccessToken = authResult.accessToken
                authResult.accessToken
            }
            is GoogleDriveAuthResult.NeedsUserConsent -> {
                _authState.value = GoogleDriveAuthState.NeedsConsent(authResult.pendingIntent)
                null
            }
            is GoogleDriveAuthResult.Error -> {
                _authState.value = GoogleDriveAuthState.Failed(authResult.exception.message ?: "Unknown error")
                null
            }
        }
    }

    private fun scheduleBackupWorker() {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val workRequest =
            PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(
                repeatInterval = 1L,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            ).setInitialDelay(1L, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

        workManager.enqueueUniquePeriodicWork(
            GoogleDriveBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    private fun cancelBackupWorker() {
        workManager.cancelUniqueWork(GoogleDriveBackupWorker.WORK_NAME)
    }

    private fun Exception.toCloudAutoBackupIssue(): CloudAutoBackupIssue {
        val msg = (this.message ?: "").lowercase()
        return when {
            msg.contains("unauthorized") ||
                msg.contains("401") ||
                msg.contains("403") ||
                msg.contains("auth") ||
                msg.contains("invalid_grant") ||
                msg.contains("revoked") ||
                msg.contains("token") ||
                msg.contains("credential") ||
                msg.contains("permission") -> CloudAutoBackupIssue.GOOGLE_DRIVE_AUTH_FAILED
            msg.contains("network") ||
                msg.contains("connect") ||
                msg.contains("timeout") ||
                msg.contains("unreachable") -> CloudAutoBackupIssue.GOOGLE_DRIVE_UNAVAILABLE
            else -> CloudAutoBackupIssue.UNKNOWN
        }
    }
}
